package back.config;

/**
 * Configuración de la aplicación. Un solo lugar para el nombre de la app:
 * el front lo pide al arrancar (GET /api/config), así que cambiarlo aquí
 * (o con la variable de entorno FYB_NAME) actualiza toda la página.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static final String NOMBRE = variable("FYB_NAME", "FYB");
    public static final String ESLOGAN = variable("FYB_TAGLINE", "Encuentra lo que creías perdido");
    public static final String DOMINIO_CORREO = variable("FYB_EMAIL_DOMAIN", "usa.edu.co");

    public static final int PUERTO = entero("PORT", 8080);

    /** Carpeta con el HTML/CSS/JS, relativa a donde se ejecuta el servidor. */
    public static final String CARPETA_FRONT = variable("FYB_FRONT_DIR", "src/front");

    /** Ponlo en "true" si sirves la app por https (recomendado en producción). */
    public static final boolean COOKIE_SEGURA = "true".equalsIgnoreCase(variable("FYB_COOKIE_SECURE", "false"));

    private static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    private static int entero(String nombre, int porDefecto) {
        try {
            return Integer.parseInt(variable(nombre, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
