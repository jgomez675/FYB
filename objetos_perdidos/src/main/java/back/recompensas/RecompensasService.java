package back.recompensas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import back.service.PuntosService;

/**
 * Canje de puntos por recompensas.
 *
 * - El catálogo se lee de data/recompensas.txt (id|nombre|descripcion|costo). Si el archivo no
 *   existe se usa un catálogo por defecto, así la app funciona sin configurar nada.
 * - Cada canje se anota en data/canjes.txt (codigo|correo|idRecompensa|nombre|costo|fecha).
 * - Los puntos se descuentan con PuntosService.descontarPuntos, que valida que alcance el saldo y
 *   deja el movimiento negativo en data/puntos.txt (por eso el historial de puntos lo muestra solo).
 */
@Service
public class RecompensasService {

    private static final Path ARCHIVO_RECOMPENSAS = Paths.get("data", "recompensas.txt");
    private static final Path ARCHIVO_CANJES = Paths.get("data", "canjes.txt");

    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public record Recompensa(int id, String nombre, String descripcion, int costo) {
    }

    public record Canje(String codigo, int idRecompensa, String nombre, int costo, String fecha) {
    }

    public record ResultadoCanje(Canje canje, int saldo) {
    }

    private static final List<Recompensa> CATALOGO_POR_DEFECTO = List.of(
            new Recompensa(1, "Bono de cafetería",
                    "Vale para una bebida o un snack en la cafetería del campus.", 100),
            new Recompensa(2, "Bono de fotocopias e impresión",
                    "Vale para fotocopias e impresiones en la papelería del campus.", 250),
            new Recompensa(3, "Bono de librería",
                    "Vale para artículos de la librería universitaria.", 500));

    // ------------------------------------------------------------------ consultas

    public List<Recompensa> catalogo() {
        List<Recompensa> leidas = new ArrayList<>();

        for (String linea : leerLineas(ARCHIVO_RECOMPENSAS)) {
            String[] partes = linea.split("\\|", -1);
            if (partes.length < 4) {
                continue;
            }

            try {
                int id = Integer.parseInt(partes[0].trim());
                int costo = Integer.parseInt(partes[3].trim());

                if (id > 0 && costo > 0 && !partes[1].isBlank()) {
                    leidas.add(new Recompensa(id, partes[1].trim(), partes[2].trim(), costo));
                }
            } catch (NumberFormatException ignorada) {
                // Línea dañada: se salta.
            }
        }

        return leidas.isEmpty() ? CATALOGO_POR_DEFECTO : leidas;
    }

    /** Canjes del estudiante, el más reciente primero. */
    public List<Canje> misCanjes(String correo) {
        String buscado = normalizar(correo);
        List<Canje> lista = new ArrayList<>();

        for (String linea : leerLineas(ARCHIVO_CANJES)) {
            String[] partes = linea.split("\\|", -1);
            if (partes.length < 6 || !normalizar(partes[1]).equals(buscado)) {
                continue;
            }

            try {
                lista.add(new Canje(
                        partes[0].trim(),
                        Integer.parseInt(partes[2].trim()),
                        partes[3].trim(),
                        Integer.parseInt(partes[4].trim()),
                        partes[5].trim()));
            } catch (NumberFormatException ignorada) {
                // Línea dañada: se salta.
            }
        }

        Collections.reverse(lista);
        return lista;
    }

    // ---------------------------------------------------------------------- canje

    /**
     * Canjea una recompensa. Todo ocurre bajo el mismo candado que usa PuntosService, así dos
     * canjes simultáneos no pueden gastar los mismos puntos dos veces.
     */
    public ResultadoCanje canjear(String correo, int idRecompensa) {
        if (correo == null || correo.isBlank()) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        Recompensa recompensa = catalogo().stream()
                .filter(r -> r.id() == idRecompensa)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Esa recompensa no existe."));

        String correoNormalizado = normalizar(correo);

        synchronized (PuntosService.class) {
            int saldoActual = PuntosService.obtenerSaldo(correoNormalizado);

            if (saldoActual < recompensa.costo()) {
                throw new RuntimeException(
                        "Te faltan " + (recompensa.costo() - saldoActual)
                                + " puntos para canjear esta recompensa.");
            }

            String codigo = generarCodigo();

            int saldoNuevo;
            try {
                saldoNuevo = PuntosService.descontarPuntos(
                        correoNormalizado,
                        recompensa.costo(),
                        "Canje de recompensa: " + recompensa.nombre() + " (" + codigo + ")");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("No tienes puntos suficientes.");
            }

            // descontarPuntos devuelve el saldo sin cambios si no pudo escribir el movimiento.
            if (saldoNuevo != saldoActual - recompensa.costo()) {
                throw new RuntimeException(
                        "No se pudo registrar el canje. Tus puntos no cambiaron; inténtalo de nuevo.");
            }

            Canje canje = new Canje(
                    codigo, recompensa.id(), recompensa.nombre(), recompensa.costo(),
                    LocalDate.now().toString());

            if (!guardarCanje(correoNormalizado, canje)) {
                // Sin el registro del canje no hay con qué reclamar el premio: se devuelven los puntos.
                PuntosService.agregarPuntos(
                        correoNormalizado,
                        recompensa.costo(),
                        "Reembolso: no se pudo registrar el canje de " + recompensa.nombre());

                throw new RuntimeException(
                        "No se pudo registrar el canje. Tus puntos fueron devueltos; inténtalo de nuevo.");
            }

            return new ResultadoCanje(canje, saldoNuevo);
        }
    }

    // --------------------------------------------------------------------- ayudas

    private boolean guardarCanje(String correo, Canje c) {
        String linea = String.join("|",
                c.codigo(),
                correo,
                String.valueOf(c.idRecompensa()),
                limpiar(c.nombre()),
                String.valueOf(c.costo()),
                c.fecha());

        try {
            Files.createDirectories(ARCHIVO_CANJES.getParent());
            Files.writeString(
                    ARCHIVO_CANJES,
                    linea + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo guardar el canje: " + e.getMessage());
            return false;
        }
    }

    private String generarCodigo() {
        StringBuilder sb = new StringBuilder("FYB-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALFABETO_CODIGO.charAt(ALEATORIO.nextInt(ALFABETO_CODIGO.length())));
        }
        return sb.toString();
    }

    private static List<String> leerLineas(Path archivo) {
        if (!Files.exists(archivo)) {
            return List.of();
        }

        try {
            List<String> limpias = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo)) {
                if (!linea.isBlank() && !linea.startsWith("#")) {
                    limpias.add(linea);
                }
            }
            return limpias;
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
            return List.of();
        }
    }

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace("|", "/").replace("\n", " ").replace("\r", " ");
    }
}
