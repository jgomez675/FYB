package back.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PuntosService {

    private static final Path ARCHIVO_PUNTOS =
            Paths.get("data", "puntos.txt");

    private static final Path ARCHIVO_VISUALIZACIONES =
            Paths.get("data", "visualizaciones_puntos.txt");

    private static final Map<String, Integer> puntos =
            new HashMap<>();

    /*
     * Guarda las combinaciones:
     * correo|idObjeto
     *
     * Esto evita entregar varias veces el punto
     * por visualizar el mismo objeto.
     */
    private static final Set<String> visualizacionesConPuntos =
            new HashSet<>();

    static {
        cargarPuntos();
        cargarVisualizaciones();
    }

    // Puntos por publicar un objeto.
    public static final int PUNTOS_POR_REGISTRAR_OBJETO = 5;

    // Puntos por consultar un objeto por primera vez.
    public static final int PUNTOS_POR_VER_OBJETO = 1;

    /*
     * Se mantiene temporalmente para que los controladores
     * anteriores sigan compilando mientras migramos
     * la devolución al nuevo sistema por categorías.
     */
    public static final int PUNTOS_POR_DEVOLVER_OBJETO = 20;

    // Puntos por devolver según categoría.
    public static final int PUNTOS_ACCESORIOS = 25;
    public static final int PUNTOS_OTROS = 40;
    public static final int PUNTOS_ROPA = 40;
    public static final int PUNTOS_LLAVES = 50;
    public static final int PUNTOS_DOCUMENTOS = 100;
    public static final int PUNTOS_TECNOLOGIA = 200;


    /*
     * Consulta el saldo actual de un estudiante.
     */
    public static synchronized int obtenerSaldo(String correo) {

        if (correo == null || correo.isBlank()) {
            return 0;
        }

        return puntos.getOrDefault(
                normalizarCorreo(correo),
                0
        );
    }


    /*
     * Agrega puntos y guarda el movimiento.
     */
    public static synchronized int agregarPuntos(
            String correo,
            int cantidad,
            String motivo) {

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo del estudiante es obligatorio."
            );
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de puntos debe ser mayor que cero."
            );
        }

        String correoNormalizado =
                normalizarCorreo(correo);

        int saldoActual =
                puntos.getOrDefault(
                        correoNormalizado,
                        0
                );

        int nuevoSaldo =
                saldoActual + cantidad;

        puntos.put(
                correoNormalizado,
                nuevoSaldo
        );

        guardarMovimiento(
                correoNormalizado,
                cantidad,
                motivo
        );

        return nuevoSaldo;
    }


    /*
     * Entrega +1 punto cuando un estudiante
     * consulta un objeto por primera vez.
     */
    public static synchronized int registrarVisualizacion(
            String correo,
            int idObjeto) {

        if (correo == null || correo.isBlank()) {
            return 0;
        }

        if (idObjeto <= 0) {
            return 0;
        }

        String correoNormalizado =
                normalizarCorreo(correo);

        String clave =
                correoNormalizado + "|" + idObjeto;

        /*
         * Si ya recibió el punto por este objeto,
         * no se vuelve a entregar.
         */
        if (visualizacionesConPuntos.contains(clave)) {
            return 0;
        }

        int nuevoSaldo =
                agregarPuntos(
                        correoNormalizado,
                        PUNTOS_POR_VER_OBJETO,
                        "Visualizacion del objeto " + idObjeto
                );

        visualizacionesConPuntos.add(clave);

        guardarVisualizacion(clave);

        return nuevoSaldo;
    }


    /*
     * Calcula los puntos de devolución
     * dependiendo de la categoría.
     */
    public static int calcularPuntosDevolucion(
            String categoria) {

        if (categoria == null || categoria.isBlank()) {
            return PUNTOS_OTROS;
        }

        String categoriaNormalizada =
                categoria.trim().toLowerCase();

        switch (categoriaNormalizada) {

            case "accesorios":
                return PUNTOS_ACCESORIOS;

            case "otros":
                return PUNTOS_OTROS;

            case "ropa":
                return PUNTOS_ROPA;

            case "llaves":
                return PUNTOS_LLAVES;

            case "documentos":
                return PUNTOS_DOCUMENTOS;

            case "tecnología":
            case "tecnologia":
                return PUNTOS_TECNOLOGIA;

            default:
                return PUNTOS_OTROS;
        }
    }


    /*
     * Entrega los puntos correspondientes
     * por devolver un objeto.
     */
    public static synchronized int agregarPuntosPorDevolucion(
            String correoDescubridor,
            String categoria,
            String nombreObjeto) {

        int cantidad =
                calcularPuntosDevolucion(categoria);

        String nombre =
                nombreObjeto == null ||
                nombreObjeto.isBlank()
                        ? "objeto"
                        : nombreObjeto;

        return agregarPuntos(
                correoDescubridor,
                cantidad,
                "Devolucion del objeto " + nombre
        );
    }


    /*
     * Carga los movimientos de puntos.
     */
    private static void cargarPuntos() {

        if (!Files.exists(ARCHIVO_PUNTOS)) {
            return;
        }

        try (
                BufferedReader lector =
                        Files.newBufferedReader(
                                ARCHIVO_PUNTOS
                        )
        ) {

            String linea;

            while (
                    (linea = lector.readLine()) != null
            ) {

                if (linea.isBlank()) {
                    continue;
                }

                String[] partes =
                        linea.split("\\|", -1);

                if (partes.length < 2) {
                    continue;
                }

                try {

                    String correo =
                            normalizarCorreo(partes[0]);

                    int cantidad =
                            Integer.parseInt(
                                    partes[1].trim()
                            );

                    puntos.put(
                            correo,
                            puntos.getOrDefault(
                                    correo,
                                    0
                            ) + cantidad
                    );

                } catch (NumberFormatException ignored) {
                    // Ignorar líneas dañadas.
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "No se pudieron cargar los puntos: "
                            + e.getMessage()
            );
        }
    }


    /*
     * Carga las visualizaciones que ya
     * entregaron puntos.
     */
    private static void cargarVisualizaciones() {

        if (!Files.exists(ARCHIVO_VISUALIZACIONES)) {
            return;
        }

        try (
                BufferedReader lector =
                        Files.newBufferedReader(
                                ARCHIVO_VISUALIZACIONES
                        )
        ) {

            String linea;

            while (
                    (linea = lector.readLine()) != null
            ) {

                if (!linea.isBlank()) {

                    visualizacionesConPuntos.add(
                            linea.trim()
                    );
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "No se pudieron cargar las visualizaciones: "
                            + e.getMessage()
            );
        }
    }


    /*
     * Guarda un movimiento de puntos.
     */
    private static void guardarMovimiento(
            String correo,
            int cantidad,
            String motivo) {

        try {

            Path carpeta =
                    ARCHIVO_PUNTOS.getParent();

            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            try (
                    BufferedWriter escritor =
                            Files.newBufferedWriter(
                                    ARCHIVO_PUNTOS,
                                    java.nio.file.StandardOpenOption.CREATE,
                                    java.nio.file.StandardOpenOption.APPEND
                            )
            ) {

                escritor.write(
                        correo +
                        "|" +
                        cantidad +
                        "|" +
                        limpiar(motivo) +
                        "|" +
                        LocalDate.now()
                );

                escritor.newLine();
            }

        } catch (IOException e) {

            System.err.println(
                    "No se pudo guardar el movimiento de puntos: "
                            + e.getMessage()
            );
        }
    }


    /*
     * Guarda que un estudiante ya recibió
     * el punto por visualizar un objeto.
     */
    private static void guardarVisualizacion(
            String clave) {

        try {

            Path carpeta =
                    ARCHIVO_VISUALIZACIONES.getParent();

            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            try (
                    BufferedWriter escritor =
                            Files.newBufferedWriter(
                                    ARCHIVO_VISUALIZACIONES,
                                    java.nio.file.StandardOpenOption.CREATE,
                                    java.nio.file.StandardOpenOption.APPEND
                            )
            ) {

                escritor.write(clave);
                escritor.newLine();
            }

        } catch (IOException e) {

            System.err.println(
                    "No se pudo guardar la visualizacion: "
                            + e.getMessage()
            );
        }
    }


    private static String normalizarCorreo(
            String correo) {

        return correo
                .trim()
                .toLowerCase();
    }


    private static String limpiar(
            String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .trim()
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}