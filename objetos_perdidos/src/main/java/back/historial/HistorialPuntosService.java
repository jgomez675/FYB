package back.historial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/**
 * Arma el historial de puntos de un estudiante a partir de data/puntos.txt, que es donde
 * PuntosService anota cada movimiento (correo|cantidad|motivo|fecha). Solo lee: no
 * modifica PuntosService ni el archivo.
 *
 * Un movimiento positivo es un punto ganado; uno negativo es un gasto (por ejemplo, al
 * canjear una recompensa). Así el historial ya soporta los gastos el día que existan.
 */
@Service
public class HistorialPuntosService {

    private static final Path ARCHIVO_PUNTOS = Paths.get("data", "puntos.txt");

    public record Movimiento(
            int cantidad,
            String tipo,
            String motivo,
            String fecha,
            int saldoDespues) {
    }

    public record Historial(
            int saldo,
            int totalGanado,
            int totalGastado,
            List<Movimiento> movimientos) {
    }

    public Historial obtener(String correo) {
        String buscado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);

        List<Movimiento> cronologico = new ArrayList<>();
        int saldo = 0;
        int ganado = 0;
        int gastado = 0;

        for (String linea : leerLineas()) {
            String[] partes = linea.split("\\|", -1);

            if (partes.length < 2 || !partes[0].trim().toLowerCase(Locale.ROOT).equals(buscado)) {
                continue;
            }

            int cantidad;
            try {
                cantidad = Integer.parseInt(partes[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            saldo += cantidad;
            if (cantidad >= 0) {
                ganado += cantidad;
            } else {
                gastado += -cantidad;
            }

            cronologico.add(new Movimiento(
                    cantidad,
                    cantidad >= 0 ? "GANADO" : "GASTADO",
                    partes.length > 2 ? partes[2].trim() : "",
                    partes.length > 3 ? partes[3].trim() : "",
                    saldo));
        }

        // El archivo está en orden de llegada: se muestra lo más reciente primero.
        Collections.reverse(cronologico);

        return new Historial(saldo, ganado, gastado, cronologico);
    }

    private List<String> leerLineas() {
        if (!Files.exists(ARCHIVO_PUNTOS)) {
            return List.of();
        }

        try {
            return Files.readAllLines(ARCHIVO_PUNTOS);
        } catch (IOException e) {
            System.err.println("No se pudo leer el historial de puntos: " + e.getMessage());
            return List.of();
        }
    }
}
