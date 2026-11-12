package back.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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


    /** Elimina movimientos y visualizaciones de todos los correos excepto el administrador. */
    public static synchronized boolean limpiarDatosUsuariosExcepto(String correoConservar) {
        if (correoConservar == null || correoConservar.isBlank()) return false;
        String admin = normalizarCorreo(correoConservar);
        try {
            java.util.List<String> movimientosConservados = new java.util.ArrayList<>();
            if (Files.exists(ARCHIVO_PUNTOS)) {
                for (String linea : Files.readAllLines(ARCHIVO_PUNTOS)) {
                    if (linea.isBlank()) continue;
                    String[] partes = linea.split("\\|", -1);
                    if (partes.length > 0 && normalizarCorreo(partes[0]).equals(admin)) {
                        movimientosConservados.add(linea);
                    }
                }
            }

            java.util.List<String> visualizacionesConservadas = new java.util.ArrayList<>();
            if (Files.exists(ARCHIVO_VISUALIZACIONES)) {
                for (String linea : Files.readAllLines(ARCHIVO_VISUALIZACIONES)) {
                    if (linea.isBlank()) continue;
                    String[] partes = linea.split("\\|", -1);
                    if (partes.length > 0 && normalizarCorreo(partes[0]).equals(admin)) {
                        visualizacionesConservadas.add(linea);
                    }
                }
            }

            Archivos.escribirLineas(ARCHIVO_PUNTOS, movimientosConservados);
            Archivos.escribirLineas(ARCHIVO_VISUALIZACIONES, visualizacionesConservadas);

            puntos.clear();
            for (String linea : movimientosConservados) {
                String[] partes = linea.split("\\|", -1);
                if (partes.length >= 2) {
                    try {
                        puntos.put(admin, puntos.getOrDefault(admin, 0) + Integer.parseInt(partes[1].trim()));
                    } catch (NumberFormatException ignored) { }
                }
            }
            visualizacionesConPuntos.clear();
            visualizacionesConPuntos.addAll(visualizacionesConservadas);
            return true;
        } catch (IOException e) {
            System.err.println("No se pudieron limpiar los puntos de los usuarios: " + e.getMessage());
            return false;
        }
    }

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

        /*
         * Primero se guarda en el archivo y solo después se cambia el saldo en memoria.
         * Si el archivo no se pudo escribir, el saldo queda igual: así lo que se ve en
         * pantalla nunca es distinto de lo que se conserva al reiniciar.
         */
        if (!guardarMovimiento(
                correoNormalizado,
                cantidad,
                motivo)) {

            return saldoActual;
        }

        puntos.put(
                correoNormalizado,
                nuevoSaldo
        );

        return nuevoSaldo;
    }


    /*
     * Descuenta puntos (por ejemplo, al canjear una recompensa) y guarda el
     * movimiento con cantidad negativa. Lanza un error si el saldo no alcanza.
     * Todavía ninguna pantalla la usa: queda lista para cuando exista el canje.
     */
    public static synchronized int descontarPuntos(
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

        if (saldoActual < cantidad) {
            throw new IllegalArgumentException(
                    "No tienes puntos suficientes."
            );
        }

        if (!guardarMovimiento(
                correoNormalizado,
                -cantidad,
                motivo)) {

            return saldoActual;
        }

        int nuevoSaldo =
                saldoActual - cantidad;

        puntos.put(
                correoNormalizado,
                nuevoSaldo
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

        /*
         * Primero se anota que este objeto ya dio su punto. Si algo falla después,
         * lo peor que pasa es que no se entrega el punto; nunca se entrega dos veces.
         */
        if (!guardarVisualizacion(clave)) {
            return 0;
        }

        visualizacionesConPuntos.add(clave);

        return agregarPuntos(
                correoNormalizado,
                PUNTOS_POR_VER_OBJETO,
                "Visualizacion del objeto " + idObjeto
        );
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
                categoria.trim().toLowerCase(Locale.ROOT);

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
     * Guarda un movimiento de puntos. Devuelve false si no se pudo escribir.
     */
    private static boolean guardarMovimiento(
            String correo,
            int cantidad,
            String motivo) {

        String linea =
                correo +
                "|" +
                cantidad +
                "|" +
                limpiar(motivo) +
                "|" +
                LocalDate.now();

        return anexarLinea(
                ARCHIVO_PUNTOS,
                linea,
                "el movimiento de puntos"
        );
    }


    /*
     * Guarda que un estudiante ya recibió el punto por visualizar un objeto.
     * Devuelve false si no se pudo escribir.
     */
    private static boolean guardarVisualizacion(
            String clave) {

        return anexarLinea(
                ARCHIVO_VISUALIZACIONES,
                clave,
                "la visualizacion"
        );
    }


    private static boolean anexarLinea(
            Path archivo,
            String linea,
            String queGuarda) {

        try {

            Path carpeta =
                    archivo.getParent();

            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            try (
                    BufferedWriter escritor =
                            Files.newBufferedWriter(
                                    archivo,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            )
            ) {

                escritor.write(linea);
                escritor.newLine();
            }

            return true;

        } catch (IOException e) {

            System.err.println(
                    "No se pudo guardar "
                            + queGuarda
                            + ": "
                            + e.getMessage()
            );

            return false;
        }
    }


    private static String normalizarCorreo(
            String correo) {

        return correo
                .trim()
                .toLowerCase(Locale.ROOT);
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