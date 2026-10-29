package back.categorias;

import back.service.ObjetoPerdidoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Categorías de los objetos, administrables por el administrador.
 * Se guardan en data/categorias.txt (una por línea). La categoría "Otros" siempre existe
 * porque es la que se usa por defecto, así que no se puede eliminar ni renombrar.
 */
public final class CategoriasService {

    public static final String OTROS = "Otros";
    public static final String OK = "OK";

    private static final Path ARCHIVO = Paths.get("data", "categorias.txt");
    private static final Set<String> categorias = new LinkedHashSet<>();

    static {
        if (Files.exists(ARCHIVO)) {
            try {
                for (String linea : Files.readAllLines(ARCHIVO)) {
                    if (!linea.isBlank()) {
                        categorias.add(limpiar(linea));
                    }
                }
            } catch (IOException e) {
                System.err.println("No se pudieron cargar las categorías.");
            }
        }

        if (categorias.isEmpty()) {
            categorias.add("Tecnología");
            categorias.add("Documentos");
            categorias.add("Accesorios");
            categorias.add("Ropa");
            categorias.add("Llaves");
            categorias.add(OTROS);
            guardar();
        } else if (buscar(OTROS) == null) {
            categorias.add(OTROS);
            guardar();
        }
    }

    private CategoriasService() {
    }

    public static synchronized List<String> listar() {
        return new ArrayList<>(categorias);
    }

    /** Devuelve el nombre tal como está guardado, o null si la categoría no existe. */
    public static synchronized String nombreExistente(String nombre) {
        return buscar(nombre);
    }

    public static synchronized String crear(String nombre) {
        String n = limpiar(nombre);

        if (n.isBlank()) {
            return "Escribe el nombre de la categoría.";
        }
        if (buscar(n) != null) {
            return "Esa categoría ya existe.";
        }

        categorias.add(n);
        guardar();
        return OK;
    }

    public static synchronized String editar(String anterior, String nuevo) {
        String viejo = buscar(anterior);
        String n = limpiar(nuevo);

        if (viejo == null) {
            return "No encontramos esa categoría.";
        }
        if (viejo.equalsIgnoreCase(OTROS)) {
            return "La categoría «Otros» se usa por defecto y no se puede cambiar.";
        }
        if (n.isBlank()) {
            return "Escribe el nuevo nombre.";
        }
        if (!viejo.equalsIgnoreCase(n) && buscar(n) != null) {
            return "Ya existe una categoría con ese nombre.";
        }

        List<String> copia = new ArrayList<>(categorias);
        categorias.clear();
        for (String c : copia) {
            categorias.add(c.equals(viejo) ? n : c);
        }

        ObjetoPerdidoService.reasignarCategoria(viejo, n);
        guardar();
        return OK;
    }

    public static synchronized String eliminar(String nombre) {
        String encontrada = buscar(nombre);

        if (encontrada == null) {
            return "No encontramos esa categoría.";
        }
        if (encontrada.equalsIgnoreCase(OTROS)) {
            return "La categoría «Otros» se usa por defecto y no se puede eliminar.";
        }

        categorias.remove(encontrada);
        // Los objetos que tenían esta categoría pasan a "Otros".
        ObjetoPerdidoService.reasignarCategoria(encontrada, OTROS);
        guardar();
        return OK;
    }

    private static String buscar(String nombre) {
        if (nombre == null) {
            return null;
        }
        for (String c : categorias) {
            if (c.equalsIgnoreCase(nombre.trim())) {
                return c;
            }
        }
        return null;
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.trim().replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static void guardar() {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Files.write(ARCHIVO, categorias);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron guardar las categorías.");
        }
    }
}
