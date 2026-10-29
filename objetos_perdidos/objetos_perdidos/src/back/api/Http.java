package back.api;

import back.config.AppConfig;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Pequeñas ayudas para leer peticiones y escribir respuestas. */
final class Http {

    static final String COOKIE_SESION = "fyb_session";
    static final int MAX_CUERPO = 8 * 1024 * 1024;

    private Http() {
    }

    /* ---------------- Respuestas ---------------- */

    static void cabecerasDeSeguridad(Headers cabeceras) {
        cabeceras.set("X-Content-Type-Options", "nosniff");
        cabeceras.set("Referrer-Policy", "same-origin");
        cabeceras.set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data: blob:; object-src 'none'; "
                        + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
    }

    static void json(HttpExchange ex, int estado, Object cuerpo) throws IOException {
        byte[] bytes = Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);

        Headers cabeceras = ex.getResponseHeaders();
        cabeceras.set("Content-Type", "application/json; charset=utf-8");
        cabeceras.set("Cache-Control", "no-store");
        cabecerasDeSeguridad(cabeceras);

        ex.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    static void ponerCookieSesion(HttpExchange ex, String token, long segundos) {
        StringBuilder cookie = new StringBuilder(COOKIE_SESION)
                .append('=').append(token)
                .append("; Path=/; HttpOnly; SameSite=Lax; Max-Age=").append(segundos);

        boolean https = "https".equalsIgnoreCase(ex.getRequestHeaders().getFirst("X-Forwarded-Proto"));
        if (AppConfig.COOKIE_SEGURA || https) cookie.append("; Secure");

        ex.getResponseHeaders().add("Set-Cookie", cookie.toString());
    }

    /* ---------------- Peticiones ---------------- */

    static String leerCookie(HttpExchange ex, String nombre) {
        List<String> cabeceras = ex.getRequestHeaders().get("Cookie");
        if (cabeceras == null) return null;

        for (String cabecera : cabeceras) {
            for (String par : cabecera.split(";")) {
                par = par.trim();
                if (par.startsWith(nombre + "=")) return par.substring(nombre.length() + 1);
            }
        }
        return null;
    }

    /** Lee el cuerpo como JSON (solo objetos). Exige Content-Type: application/json. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> leerJson(HttpExchange ex) throws IOException {
        String tipo = ex.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, "La petición debe enviarse como JSON.");
        }

        byte[] bytes = ex.getRequestBody().readNBytes(MAX_CUERPO + 1);
        if (bytes.length > MAX_CUERPO) {
            throw new ApiException(413, "La petición es demasiado grande.");
        }

        try {
            Object valor = Json.leer(new String(bytes, StandardCharsets.UTF_8));
            if (valor instanceof Map) return (Map<String, Object>) valor;
        } catch (IllegalArgumentException e) {
            // cae al error de abajo
        }
        throw new ApiException(400, "La petición no tiene un formato válido.");
    }

    static String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        return valor instanceof String ? (String) valor : "";
    }

    /**
     * Defensa contra peticiones enviadas desde otra página: si el navegador manda
     * Origin, debe coincidir con el Host al que se le está hablando.
     */
    static void verificarOrigen(HttpExchange ex) {
        String origen = ex.getRequestHeaders().getFirst("Origin");
        if (origen == null) return;

        String host = ex.getRequestHeaders().getFirst("Host");
        String autoridad = origen.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");

        if (host == null || !autoridad.equalsIgnoreCase(host)) {
            throw new ApiException(403, "Origen no permitido.");
        }
    }

    static String ipCliente(HttpExchange ex) {
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }
}
