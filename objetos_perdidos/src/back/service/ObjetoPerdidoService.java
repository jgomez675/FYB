package back.service;

import back.model.ObjetoPerdido;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ObjetoPerdidoService {

    private static final List<ObjetoPerdido> objetos = new ArrayList<>();
    private static final Path ARCHIVO_OBJETOS = Paths.get("data", "objetos.txt");
    private static final Path CARPETA_IMAGENES = Paths.get("data", "objetos");

    // Último id entregado. Se guarda aparte para que un id retirado no se vuelva a usar:
    // si no, las fotos en caché, los favoritos y los puntos de un objeto viejo quedarían pegados a uno nuevo.
    private static final Path ARCHIVO_ULTIMO_ID = Paths.get("data", "objetos_ultimo_id.txt");
    private static final Pattern NOMBRE_IMAGEN = Pattern.compile("objeto_\\d+\\.(jpg|jpeg|png|gif|webp)");
    private static int ultimoId = 0;

    static {
        cargarObjetos();
        cargarUltimoId();
    }

    private static void cargarObjetos() {
        if (!Files.exists(ARCHIVO_OBJETOS)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_OBJETOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] partes = linea.split("\\|", -1);
                if (partes.length == 7) {
                    try {
                        objetos.add(new ObjetoPerdido(
                                Integer.parseInt(partes[0]), partes[1], partes[2], partes[3],
                                partes[4], partes[5], partes[6]
                        ));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los objetos: " + e.getMessage());
        }
    }

    private static void cargarUltimoId() {
        int guardado = 0;
        try {
            if (Files.exists(ARCHIVO_ULTIMO_ID)) {
                guardado = Integer.parseInt(Files.readString(ARCHIVO_ULTIMO_ID).trim());
            }
        } catch (IOException | NumberFormatException ignorada) {
            // se calcula con los objetos existentes
        }

        int mayor = 0;
        for (ObjetoPerdido objeto : objetos) {
            if (objeto.getId() > mayor) mayor = objeto.getId();
        }

        ultimoId = Math.max(guardado, mayor);
        if (ultimoId != guardado) {
            try {
                guardarUltimoId();
            } catch (IOException e) {
                System.err.println("No se pudo guardar el último id: " + e.getMessage());
            }
        }
    }

    private static void guardarUltimoId() throws IOException {
        Archivos.escribirLineas(ARCHIVO_ULTIMO_ID, List.of(String.valueOf(ultimoId)));
    }

    public static List<ObjetoPerdido> obtenerObjetos() {
        synchronized (objetos) {
            return new ArrayList<>(objetos);
        }
    }

    public static ObjetoPerdido obtenerObjeto(int id) {
        synchronized (objetos) {
            for (ObjetoPerdido objeto : objetos) {
                if (objeto.getId() == id) {
                    return objeto;
                }
            }
        }
        return null;
    }

    public static Path carpetaImagenes() {
        return CARPETA_IMAGENES;
    }

    public static final String OBJETO_GUARDADO = "OBJETO_GUARDADO";
    public static final String OBJETO_ELIMINADO = "OBJETO_ELIMINADO";
    public static final String OBJETO_NO_ENCONTRADO = "No encontramos este objeto.";
    public static final String SIN_PERMISO = "Solo quien publicó el objeto puede retirarlo.";

    public static String guardarObjeto(String nombre, String descripcion, String lugar,
                                       String fecha, Path imagenOriginal, String correoUsuario) {
        if (nombre == null || nombre.isBlank()) return "El nombre del objeto es obligatorio.";
        if (descripcion == null || descripcion.isBlank()) return "La descripción es obligatoria.";
        if (lugar == null || lugar.isBlank()) return "El lugar es obligatorio.";
        if (fecha == null || fecha.isBlank()) return "La fecha es obligatoria.";
        if (imagenOriginal == null) return "Debes seleccionar una imagen.";

        synchronized (objetos) {
            try {
                Files.createDirectories(CARPETA_IMAGENES);

                int id = obtenerSiguienteId();
                String extension = obtenerExtension(imagenOriginal.getFileName().toString());
                Path destino = CARPETA_IMAGENES.resolve("objeto_" + id + extension);
                Files.copy(imagenOriginal, destino, StandardCopyOption.REPLACE_EXISTING);

                ObjetoPerdido objeto = new ObjetoPerdido(
                        id,
                        limpiar(nombre),
                        limpiar(descripcion),
                        limpiar(lugar),
                        limpiar(fecha),
                        destino.toString(),
                        limpiar(correoUsuario)
                );

                objetos.add(objeto);
                guardarObjetos();
                return "OBJETO_GUARDADO";

            } catch (IOException e) {
                return "No se pudo guardar el objeto: " + e.getMessage();
            }
        }
    }

    /**
     * Retira una publicación. Solo puede hacerlo quien la publicó. Devuelve OBJETO_ELIMINADO,
     * OBJETO_NO_ENCONTRADO, SIN_PERMISO o un mensaje de error si no se pudo guardar el cambio.
     */
    public static String eliminarObjeto(int id, String correoSolicitante) {
        synchronized (objetos) {
            int posicion = -1;
            for (int i = 0; i < objetos.size(); i++) {
                if (objetos.get(i).getId() == id) {
                    posicion = i;
                    break;
                }
            }

            if (posicion < 0) return OBJETO_NO_ENCONTRADO;

            ObjetoPerdido objeto = objetos.get(posicion);
            if (correoSolicitante == null || !objeto.getCorreoUsuario().equalsIgnoreCase(correoSolicitante.trim())) {
                return SIN_PERMISO;
            }

            objetos.remove(posicion);
            try {
                guardarObjetos();
            } catch (IOException e) {
                objetos.add(posicion, objeto);
                System.err.println("No se pudo retirar el objeto: " + e.getMessage());
                return "No se pudo retirar la publicación. Inténtalo de nuevo.";
            }

            borrarImagen(objeto.getImagen());
            return OBJETO_ELIMINADO;
        }
    }

    // data/objetos.txt puede traer rutas de Windows: solo se borra un archivo con nombre objeto_N.ext dentro de data/objetos.
    private static void borrarImagen(String ruta) {
        if (ruta == null) return;

        String normalizada = ruta.replace('\\', '/');
        String archivo = normalizada.substring(normalizada.lastIndexOf('/') + 1);
        if (!NOMBRE_IMAGEN.matcher(archivo).matches()) return;

        try {
            Files.deleteIfExists(CARPETA_IMAGENES.resolve(archivo));
        } catch (IOException e) {
            System.err.println("No se pudo borrar la foto " + archivo + ": " + e.getMessage());
        }
    }

    // Quien llama debe tener el candado de `objetos`.
    private static void guardarObjetos() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (ObjetoPerdido objeto : objetos) {
            lineas.add(
                    objeto.getId() + "|" + objeto.getNombre() + "|" +
                    objeto.getDescripcion() + "|" + objeto.getLugar() + "|" +
                    objeto.getFecha() + "|" + objeto.getImagen() + "|" +
                    objeto.getCorreoUsuario()
            );
        }
        Archivos.escribirLineas(ARCHIVO_OBJETOS, lineas);
    }

    // Quien llama debe tener el candado de `objetos`. Los ids nunca se reutilizan.
    private static int obtenerSiguienteId() throws IOException {
        int anterior = ultimoId;
        ultimoId++;
        try {
            guardarUltimoId();
        } catch (IOException e) {
            ultimoId = anterior;
            throw e;
        }
        return ultimoId;
    }

    private static String limpiar(String texto) {
        return texto.trim().replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static String obtenerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto >= 0) return nombre.substring(punto).toLowerCase();
        return ".jpg";
    }
}
