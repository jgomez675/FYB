package back.api;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones en memoria: al iniciar sesión el navegador recibe una cookie con un código
 * aleatorio y aquí queda guardado quién es. Si se reinicia el servidor, todos deben volver a entrar.
 */
public final class SessionManager {

    static final long DURACION_SEGUNDOS = 7L * 24 * 3600;

    public record Sesion(String nombre, String correo, long expira) {
    }

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    private SessionManager() {
    }

   public static String crear(String nombre, String correo) {
        long ahora = System.currentTimeMillis();
        if (sesiones.size() > 1000) {
            sesiones.values().removeIf(s -> s.expira() < ahora);
        }

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sesiones.put(token, new Sesion(nombre, correo, ahora + DURACION_SEGUNDOS * 1000));
        return token;
    }

    public static Sesion obtener(String token) {
        if (token == null) return null;

        Sesion sesion = sesiones.get(token);
        if (sesion == null) return null;

        if (sesion.expira() < System.currentTimeMillis()) {
            sesiones.remove(token);
            return null;
        }
        return sesion;
    }

    public static void cerrar(String token) {
        if (token != null) sesiones.remove(token);
    }
}
