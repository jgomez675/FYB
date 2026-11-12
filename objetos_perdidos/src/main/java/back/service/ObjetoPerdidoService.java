package back.service;

import back.model.ObjetoPerdido;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ObjetoPerdidoService {

    private static final List<ObjetoPerdido> objetos =
            new ArrayList<>();

    private static final Path ARCHIVO_OBJETOS =
            Paths.get("data", "objetos.txt");

    private static final Path CARPETA_IMAGENES =
            Paths.get("data", "objetos");

    private static final String CATEGORIA_OTROS = "Otros";

    private static final String ESTADO_PERDIDO = "Perdido";

    private static final String ESTADO_ENCONTRADO = "Encontrado";

    static {
        cargarObjetos();
    }

    private static void cargarObjetos() {

        if (!Files.exists(ARCHIVO_OBJETOS)) {
            return;
        }

        try (BufferedReader lector =
                     Files.newBufferedReader(ARCHIVO_OBJETOS)) {

            String linea;

            while ((linea = lector.readLine()) != null) {

                if (linea.isBlank()) {
                    continue;
                }

                String[] partes =
                        linea.split("\\|", -1);

                try {

                    /*
                     * Formato antiguo:
                     * 7 campos
                     */
                    if (partes.length == 7) {

                        objetos.add(
                                new ObjetoPerdido(
                                        Integer.parseInt(partes[0]),
                                        partes[1],
                                        partes[2],
                                        partes[3],
                                        partes[4],
                                        partes[5],
                                        partes[6],
                                        ""
                                )
                        );
                    }

                    /*
                     * Formato con característica privada:
                     * 8 campos
                     */
                    else if (partes.length == 8) {

                        objetos.add(
                                new ObjetoPerdido(
                                        Integer.parseInt(partes[0]),
                                        partes[1],
                                        partes[2],
                                        partes[3],
                                        partes[4],
                                        partes[5],
                                        partes[6],
                                        partes[7]
                                )
                        );
                    }

                    /*
                     * Formato con categoría y estado:
                     * 10 campos
                     */
                    else if (partes.length == 10) {

                        ObjetoPerdido objeto =
                                new ObjetoPerdido(
                                        Integer.parseInt(partes[0]),
                                        partes[1],
                                        partes[2],
                                        partes[3],
                                        partes[4],
                                        partes[5],
                                        partes[6],
                                        partes[7],
                                        partes[8].isBlank()
                                                ? CATEGORIA_OTROS
                                                : partes[8],
                                        partes[9].isBlank()
                                                ? ESTADO_PERDIDO
                                                : partes[9]
                                );

                        objetos.add(objeto);
                    }

                    /*
                     * Formato anterior con devolución:
                     * 11 campos
                     *
                     * id|nombre|descripcion|lugar|fecha|imagen|
                     * correoUsuario|caracteristicaPrivada|estado|
                     * correoEstudianteDevuelve|correoDescubridor
                     */
                    else if (partes.length == 11) {

                        ObjetoPerdido objeto =
                                new ObjetoPerdido(
                                        Integer.parseInt(partes[0]),
                                        partes[1],
                                        partes[2],
                                        partes[3],
                                        partes[4],
                                        partes[5],
                                        partes[6],
                                        partes[7],
                                        CATEGORIA_OTROS,
                                        partes[8].isBlank()
                                                ? "PERDIDO"
                                                : partes[8]
                                );

                        if ("DEVUELTO".equalsIgnoreCase(partes[8])) {
                            objeto.marcarComoDevuelto(partes[9]);
                        }

                        if (!partes[10].isBlank()) {
                            objeto.registrarDescubridor(
                                    partes[10]
                            );
                        }

                        objetos.add(objeto);
                    }

                    /*
                     * Formato actual:
                     * 12 campos
                     *
                     * id|nombre|descripcion|lugar|fecha|imagen|
                     * correoUsuario|caracteristicaPrivada|categoria|
                     * estado|correoEstudianteDevuelve|
                     * correoDescubridor
                     */
                    else if (partes.length >= 12) {

                        ObjetoPerdido objeto =
                                new ObjetoPerdido(
                                        Integer.parseInt(partes[0]),
                                        partes[1],
                                        partes[2],
                                        partes[3],
                                        partes[4],
                                        partes[5],
                                        partes[6],
                                        partes[7],
                                        partes[8].isBlank()
                                                ? CATEGORIA_OTROS
                                                : partes[8],
                                        partes[9].isBlank()
                                                ? "PERDIDO"
                                                : partes[9]
                                );

                        if ("DEVUELTO".equalsIgnoreCase(partes[9])) {
                            objeto.marcarComoDevuelto(
                                    partes[10]
                            );
                        }

                        if (!partes[11].isBlank()) {
                            objeto.registrarDescubridor(
                                    partes[11]
                            );
                        }

                        objetos.add(objeto);
                    }

                } catch (NumberFormatException ignored) {
                    // Ignora registros con ID inválido.
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "No se pudieron cargar los objetos: "
                            + e.getMessage()
            );
        }
    }

    public static List<ObjetoPerdido> obtenerObjetos() {
        return new ArrayList<>(objetos);
    }

    public static ObjetoPerdido obtenerObjeto(int id) {

        for (ObjetoPerdido objeto : objetos) {

            if (objeto.getId() == id) {
                return objeto;
            }
        }

        return null;
    }

    public static Path carpetaImagenes() {
        return CARPETA_IMAGENES;
    }

    public static final String OBJETO_GUARDADO =
            "OBJETO_GUARDADO";

    public static String guardarObjeto(
            String nombre,
            String descripcion,
            String lugar,
            String fecha,
            Path imagenOriginal,
            String correoUsuario,
            String caracteristicaPrivada,
            String categoria,
            String estado) {

        if (nombre == null || nombre.isBlank()) {
            return "El nombre del objeto es obligatorio.";
        }

        if (descripcion == null || descripcion.isBlank()) {
            return "La descripción es obligatoria.";
        }

        if (lugar == null || lugar.isBlank()) {
            return "El lugar es obligatorio.";
        }

        if (fecha == null || fecha.isBlank()) {
            return "La fecha es obligatoria.";
        }

        if (imagenOriginal == null) {
            return "Debes seleccionar una imagen.";
        }

        if (caracteristicaPrivada == null ||
                caracteristicaPrivada.isBlank()) {

            return "La característica privada es obligatoria.";
        }

        categoria = validarCategoria(categoria);
        estado = validarEstado(estado);

        try {

            Files.createDirectories(
                    CARPETA_IMAGENES
            );

            int id = obtenerSiguienteId();

            String extension =
                    obtenerExtension(
                            imagenOriginal.getFileName().toString()
                    );

            Path destino =
                    CARPETA_IMAGENES.resolve(
                            "objeto_" + id + extension
                    );

            Files.copy(
                    imagenOriginal,
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            ObjetoPerdido objeto =
                    new ObjetoPerdido(
                            id,
                            limpiar(nombre),
                            limpiar(descripcion),
                            limpiar(lugar),
                            limpiar(fecha),
                            destino.toString(),
                            limpiar(correoUsuario),
                            limpiar(caracteristicaPrivada),
                            limpiar(categoria),
                            limpiar(estado)
                    );

            objetos.add(objeto);

            guardarObjetos();

            /*
             * El estudiante recibe 5 puntos
             * después de guardar correctamente
             * el objeto.
             */
            PuntosService.agregarPuntos(
                    correoUsuario,
                    PuntosService.PUNTOS_POR_REGISTRAR_OBJETO,
                    "Registro del objeto " + objeto.getNombre()
            );

            return OBJETO_GUARDADO;

        } catch (IOException e) {

            return "No se pudo guardar el objeto: "
                    + e.getMessage();
        }
    }

    public static boolean registrarDescubridor(
            int id,
            String correoDescubridor) {

        if (correoDescubridor == null ||
                correoDescubridor.isBlank()) {

            return false;
        }

        ObjetoPerdido objeto =
                obtenerObjeto(id);

        if (objeto == null) {
            return false;
        }

        // El propietario no puede ser el descubridor.
        if (objeto.getCorreoUsuario()
                .equalsIgnoreCase(correoDescubridor)) {

            return false;
        }

        // Solo puede existir un descubridor.
        if (objeto.getCorreoDescubridor() != null &&
                !objeto.getCorreoDescubridor().isBlank()) {

            return false;
        }

        objeto.registrarDescubridor(
                correoDescubridor.trim()
        );

        try {

            guardarObjetos();

            return true;

        } catch (IOException e) {

            objeto.registrarDescubridor("");

            System.err.println(
                    "No se pudo guardar el descubridor: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public static boolean marcarComoDevuelto(
            int id,
            String correoEstudiante) {

        ObjetoPerdido objeto =
                obtenerObjeto(id);

        if (objeto == null) {
            return false;
        }

        if (objeto.estaDevuelto()) {
            return false;
        }

        objeto.marcarComoDevuelto(
                correoEstudiante
        );

        try {

            guardarObjetos();

            return true;

        } catch (IOException e) {

            System.err.println(
                    "No se pudo guardar la devolución: "
                            + e.getMessage()
            );

            return false;
        }
    }

    private static void guardarObjetos()
            throws IOException {

        Path carpeta =
                ARCHIVO_OBJETOS.getParent();

        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        try (BufferedWriter escritor =
                     Files.newBufferedWriter(
                             ARCHIVO_OBJETOS)) {

            for (ObjetoPerdido objeto : objetos) {

                escritor.write(
                        objeto.getId() + "|" +
                        objeto.getNombre() + "|" +
                        objeto.getDescripcion() + "|" +
                        objeto.getLugar() + "|" +
                        objeto.getFecha() + "|" +
                        objeto.getImagen() + "|" +
                        objeto.getCorreoUsuario() + "|" +
                        objeto.getCaracteristicaPrivada() + "|" +
                        objeto.getCategoria() + "|" +
                        objeto.getEstado() + "|" +
                        objeto.getCorreoEstudianteDevuelve() + "|" +
                        objeto.getCorreoDescubridor()
                );

                escritor.newLine();
            }
        }
    }

    private static int obtenerSiguienteId() {

        int mayor = 0;

        for (ObjetoPerdido objeto : objetos) {

            if (objeto.getId() > mayor) {
                mayor = objeto.getId();
            }
        }

        return mayor + 1;
    }

    private static String limpiar(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .trim()
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String validarCategoria(
            String categoria) {

        if (categoria == null ||
                categoria.isBlank()) {

            return CATEGORIA_OTROS;
        }

        return switch (categoria.trim()) {

            case "Tecnología",
                 "Documentos",
                 "Accesorios",
                 "Ropa",
                 "Llaves",
                 "Otros" -> categoria.trim();

            default -> CATEGORIA_OTROS;
        };
    }

    private static String validarEstado(
            String estado) {

        if (ESTADO_ENCONTRADO.equalsIgnoreCase(estado)) {
            return ESTADO_ENCONTRADO;
        }

        return ESTADO_PERDIDO;
    }

    private static String obtenerExtension(
            String nombre) {

        int punto =
                nombre.lastIndexOf('.');

        if (punto >= 0) {

            return nombre.substring(punto)
                    .toLowerCase();
        }

        return ".jpg";
    }
}