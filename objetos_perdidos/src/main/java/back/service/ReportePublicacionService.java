package back.service;

import back.model.ReportePublicacion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportePublicacionService {

    private static final List<ReportePublicacion> reportes = new ArrayList<>();
    private static final Path ARCHIVO_REPORTES = Paths.get("data", "reportes.txt");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    static {
        cargarReportes();
    }

    private static void cargarReportes() {
        if (!Files.exists(ARCHIVO_REPORTES)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_REPORTES)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] partes = linea.split("\\|", -1);
                if (partes.length != 6) continue;

                try {
                    reportes.add(new ReportePublicacion(
                            Integer.parseInt(partes[0]),
                            Integer.parseInt(partes[1]),
                            partes[2],
                            partes[3],
                            partes[4],
                            partes[5]
                    ));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los reportes: " + e.getMessage());
        }
    }

    public static String guardarReporte(int objetoId, String correoUsuario,
                                        String motivo, String detalle) {
        if (motivo == null || motivo.isBlank()) {
            return "Debes seleccionar un motivo para el reporte.";
        }

        if (objetoId <= 0 || ObjetoPerdidoService.obtenerObjeto(objetoId) == null) {
            return "El objeto que intentas reportar no existe.";
        }

        motivo = limpiar(motivo);
        detalle = limpiar(detalle);
        correoUsuario = limpiar(correoUsuario);

        int id = obtenerSiguienteId();
        ReportePublicacion reporte = new ReportePublicacion(
                id,
                objetoId,
                correoUsuario,
                motivo,
                detalle,
                LocalDateTime.now().format(FORMATO_FECHA)
        );

        reportes.add(reporte);

        try {
            guardarReportes();
            return "REPORTE_GUARDADO";
        } catch (IOException e) {
            reportes.remove(reporte);
            return "No se pudo guardar el reporte. Inténtalo de nuevo.";
        }
    }

    private static void guardarReportes() throws IOException {
        Path carpeta = ARCHIVO_REPORTES.getParent();
        if (carpeta != null) Files.createDirectories(carpeta);

        try (BufferedWriter escritor = Files.newBufferedWriter(ARCHIVO_REPORTES)) {
            for (ReportePublicacion reporte : reportes) {
                escritor.write(
                        reporte.getId() + "|" +
                        reporte.getObjetoId() + "|" +
                        reporte.getCorreoUsuario() + "|" +
                        reporte.getMotivo() + "|" +
                        reporte.getDetalle() + "|" +
                        reporte.getFecha()
                );
                escritor.newLine();
            }
        }
    }

    private static int obtenerSiguienteId() {
        int mayor = 0;
        for (ReportePublicacion reporte : reportes) {
            if (reporte.getId() > mayor) mayor = reporte.getId();
        }
        return mayor + 1;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim()
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
