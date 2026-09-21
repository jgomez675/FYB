package back.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Frena la adivinación de contraseñas: 5 fallos seguidos bloquean 10 minutos (por IP y correo). */
final class IntentosFallidos {

    private static final int MAXIMO = 5;
    private static final long VENTANA_MS = 10L * 60 * 1000;

    private record Registro(int cuenta, long desde) {
    }

    private static final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private IntentosFallidos() {
    }

    static boolean bloqueado(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) return false;

        if (System.currentTimeMillis() - registro.desde() > VENTANA_MS) {
            registros.remove(clave);
            return false;
        }
        return registro.cuenta() >= MAXIMO;
    }

    static void registrarFallo(String clave) {
        long ahora = System.currentTimeMillis();

        if (registros.size() > 10_000) {
            registros.values().removeIf(r -> ahora - r.desde() > VENTANA_MS);
        }

        registros.compute(clave, (k, r) ->
                (r == null || ahora - r.desde() > VENTANA_MS) ? new Registro(1, ahora) : new Registro(r.cuenta() + 1, r.desde()));
    }

    static void limpiar(String clave) {
        registros.remove(clave);
    }
}
