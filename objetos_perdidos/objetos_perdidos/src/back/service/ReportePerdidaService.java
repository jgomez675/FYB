package back.service;

import back.model.ReportePerdida;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ReportePerdidaService {

    private static final List<ReportePerdida> reportes = new ArrayList<>();

    private static final Path ARCHIVO_REPORTES =
            Paths.get("data", "reportes_perdida.txt");

    static {
        cargarReportes();
    }

    private static void cargarReportes() {

        if (!Files.exists(ARCHIVO_REPORTES)) {
            return;
        }

        try (BufferedReader lector =
                     Files.newBufferedReader(ARCHIVO_REPORTES)) {

            String linea;

            while ((linea = lector.readLine()) != null) {

                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split("\\|", -1);

                if (partes.length == 6) {

                    try {

                        reportes.add(new ReportePerdida(
                                Integer.parseInt(partes[0]),
                                partes[1],
                                partes[2],
                                partes[3],
                                partes[4],
                                partes[5]
                        ));

                    } catch (NumberFormatException ignored) {
                    }
                }
            }

        } catch (IOException e) {
            System.err.println(
                    "No se pudieron cargar los reportes: "
                            + e.getMessage()
            );
        }
    }

    public static String guardarReporte(
            String nombreObjeto,
            String descripcion,
            String lugar,
            String fecha,
            String correoUsuario) {

        if (nombreObjeto == null || nombreObjeto.isBlank()) {
            return "El nombre del objeto es obligatorio.";
        }

        if (descripcion == null || descripcion.isBlank()) {
            return "La descripción es obligatoria.";
        }

        if (lugar == null || lugar.isBlank()) {
            return "El lugar donde se perdió es obligatorio.";
        }

        if (fecha == null || fecha.isBlank()) {
            return "La fecha es obligatoria.";
        }

        try {

            int id = obtenerSiguienteId();

            ReportePerdida reporte = new ReportePerdida(
                    id,
                    limpiar(nombreObjeto),
                    limpiar(descripcion),
                    limpiar(lugar),
                    limpiar(fecha),
                    limpiar(correoUsuario)
            );

            reportes.add(reporte);

            guardarReportes();

            return "REPORTE_GUARDADO";

        } catch (IOException e) {

            return "No se pudo guardar el reporte: "
                    + e.getMessage();
        }
    }

    public static List<ReportePerdida> obtenerReportes() {
        return new ArrayList<>(reportes);
    }

    private static void guardarReportes() throws IOException {

        Path carpeta = ARCHIVO_REPORTES.getParent();

        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        try (BufferedWriter escritor =
                     Files.newBufferedWriter(ARCHIVO_REPORTES)) {

            for (ReportePerdida reporte : reportes) {

                escritor.write(
                        reporte.getId() + "|" +
                        reporte.getNombreObjeto() + "|" +
                        reporte.getDescripcion() + "|" +
                        reporte.getLugar() + "|" +
                        reporte.getFecha() + "|" +
                        reporte.getCorreoUsuario()
                );

                escritor.newLine();
            }
        }
    }

    private static int obtenerSiguienteId() {

        int mayor = 0;

        for (ReportePerdida reporte : reportes) {

            if (reporte.getId() > mayor) {
                mayor = reporte.getId();
            }
        }

        return mayor + 1;
    }

    private static String limpiar(String texto) {

        return texto.trim()
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}