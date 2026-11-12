package back.persistence.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.model.ObjetoPerdido;
import back.persistence.entity.CategoriaEntity;
import back.persistence.entity.MovimientoPuntosEntity;
import back.persistence.entity.ObjetoEntity;
import back.persistence.entity.UsuarioEntity;
import back.persistence.repository.CategoriaRepository;
import back.persistence.repository.MovimientoPuntosRepository;
import back.persistence.repository.ObjetoRepository;
import back.persistence.repository.UsuarioRepository;
import back.service.ObjetoPerdidoService;
import back.service.PuntosService;

/**
 * Copia a la base de datos lo que los servicios actuales ya guardan en data/*.txt.
 *
 * Los servicios existentes no se tocaron: este paquete solo LEE de ellos (y de los archivos)
 * y hace "upsert" en la base. Nunca borra nada de la base, así que la base también sirve
 * de respaldo si un .txt se daña o se pierde.
 *
 * Es idempotente: se puede ejecutar las veces que sea necesario sin duplicar datos.
 */
@Service
public class SincronizacionService {

    private static final Path ARCHIVO_USUARIOS = Paths.get("data", "usuarios.txt");
    private static final Path ARCHIVO_CATEGORIAS = Paths.get("data", "categorias.txt");
    private static final Path ARCHIVO_PUNTOS = Paths.get("data", "puntos.txt");

    private static final String CATEGORIA_POR_DEFECTO = "Otros";
    private static final List<String> CATEGORIAS_BASE =
            List.of("Tecnología", "Documentos", "Accesorios", "Ropa", "Llaves", "Otros");

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ObjetoRepository objetos;
    private final MovimientoPuntosRepository movimientos;

    public SincronizacionService(
            UsuarioRepository usuarios,
            CategoriaRepository categorias,
            ObjetoRepository objetos,
            MovimientoPuntosRepository movimientos) {

        this.usuarios = usuarios;
        this.categorias = categorias;
        this.objetos = objetos;
        this.movimientos = movimientos;
    }

    @Transactional
    public void sincronizar() {
        Map<String, CategoriaEntity> categoriasPorNombre = sincronizarCategorias();
        sincronizarUsuarios();
        sincronizarObjetos(categoriasPorNombre);
        sincronizarPuntos();
    }

    // ---------------------------------------------------------------- categorías

    private Map<String, CategoriaEntity> sincronizarCategorias() {
        Map<String, CategoriaEntity> porNombre = new HashMap<>();
        for (CategoriaEntity existente : categorias.findAll()) {
            porNombre.put(clave(existente.getNombre()), existente);
        }

        List<String> nombres = new ArrayList<>(leerLineas(ARCHIVO_CATEGORIAS));
        if (nombres.isEmpty()) {
            nombres.addAll(CATEGORIAS_BASE);
        }

        for (String nombre : nombres) {
            categoriaPorNombre(porNombre, nombre);
        }
        return porNombre;
    }

    /** Busca la categoría (sin importar mayúsculas) y la crea si todavía no existe. */
    private CategoriaEntity categoriaPorNombre(Map<String, CategoriaEntity> cache, String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty() || limpio.equalsIgnoreCase("OTRO")) {
            limpio = CATEGORIA_POR_DEFECTO;
        }

        CategoriaEntity categoria = cache.get(clave(limpio));
        if (categoria == null) {
            categoria = categorias.save(new CategoriaEntity(
                    corta(limpio, 80),
                    PuntosService.calcularPuntosDevolucion(limpio)));
            cache.put(clave(limpio), categoria);
        }
        return categoria;
    }

    // ------------------------------------------------------------------ usuarios

    private void sincronizarUsuarios() {
        for (String linea : leerLineas(ARCHIVO_USUARIOS)) {
            String[] partes = linea.split("\\|", -1);
            if (partes.length != 3 || partes[1].isBlank()) {
                continue;
            }

            String correo = partes[1].trim().toLowerCase(Locale.ROOT);
            UsuarioEntity usuario = usuarios.findById(correo).orElse(null);

            if (usuario == null) {
                usuarios.save(new UsuarioEntity(correo, corta(partes[0], 200), partes[2]));
            } else {
                usuario.setNombre(corta(partes[0], 200));
                usuario.setContrasena(partes[2]);
            }
        }
    }

    // ------------------------------------------------------------------- objetos

    private void sincronizarObjetos(Map<String, CategoriaEntity> categoriasPorNombre) {
        for (ObjetoPerdido origen : ObjetoPerdidoService.obtenerObjetos()) {
            ObjetoEntity destino = objetos.findById(origen.getId()).orElse(null);
            boolean nuevo = destino == null;
            if (nuevo) {
                destino = new ObjetoEntity(origen.getId());
            }

            destino.setNombre(corta(origen.getNombre(), 500));
            destino.setDescripcion(corta(origen.getDescripcion(), 4000));
            destino.setLugar(corta(origen.getLugar(), 500));
            destino.setFecha(corta(origen.getFecha(), 40));
            destino.setImagen(corta(origen.getImagen(), 500));
            destino.setCorreoUsuario(corta(origen.getCorreoUsuario(), 254));
            destino.setCaracteristicaPrivada(corta(origen.getCaracteristicaPrivada(), 4000));
            destino.setCategoria(categoriaPorNombre(categoriasPorNombre, origen.getCategoria()));
            destino.setEstado(corta(origen.getEstado(), 30));
            destino.setCorreoEstudianteDevuelve(corta(origen.getCorreoEstudianteDevuelve(), 254));
            destino.setCorreoDescubridor(corta(origen.getCorreoDescubridor(), 254));

            if (nuevo) {
                objetos.save(destino);
            }
        }
    }

    // -------------------------------------------------------------------- puntos

    /**
     * puntos.txt solo crece (cada línea es un movimiento), así que basta con insertar
     * las líneas que la base todavía no tiene.
     */
    private void sincronizarPuntos() {
        List<MovimientoPuntosEntity> delArchivo = new ArrayList<>();

        for (String linea : leerLineas(ARCHIVO_PUNTOS)) {
            String[] partes = linea.split("\\|", -1);
            if (partes.length < 2 || partes[0].isBlank()) {
                continue;
            }

            try {
                delArchivo.add(new MovimientoPuntosEntity(
                        partes[0].trim().toLowerCase(Locale.ROOT),
                        Integer.parseInt(partes[1].trim()),
                        partes.length > 2 ? corta(partes[2], 500) : "",
                        partes.length > 3 ? fecha(partes[3]) : null));
            } catch (NumberFormatException ignorada) {
                // Línea dañada: se salta, igual que hace PuntosService.
            }
        }

        int yaGuardados = (int) movimientos.count();
        if (delArchivo.size() > yaGuardados) {
            movimientos.saveAll(delArchivo.subList(yaGuardados, delArchivo.size()));
        }
    }

    // ------------------------------------------------------------------- ayudas

    private static List<String> leerLineas(Path archivo) {
        if (!Files.exists(archivo)) {
            return List.of();
        }

        try {
            List<String> limpias = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo)) {
                if (!linea.isBlank()) {
                    limpias.add(linea);
                }
            }
            return limpias;
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + " para la base de datos: " + e.getMessage());
            return List.of();
        }
    }

    private static LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String clave(String nombre) {
        return nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static String corta(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
