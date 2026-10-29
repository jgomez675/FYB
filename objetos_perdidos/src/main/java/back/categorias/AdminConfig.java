package back.categorias;

/**
 * Quién es el administrador de la aplicación.
 * Es la cuenta cuyo correo coincide con FYB_ADMIN_EMAIL (por defecto admin@usa.edu.co).
 * Esa persona debe registrarse normalmente; al iniciar sesión verá "Gestionar categorías".
 */
public final class AdminConfig {

    private AdminConfig() {
    }

    public static final String CORREO_ADMIN = leerCorreoAdmin();

    public static boolean esAdmin(String correo) {
        return correo != null && correo.trim().equalsIgnoreCase(CORREO_ADMIN);
    }

    private static String leerCorreoAdmin() {
        String valor = System.getenv("FYB_ADMIN_EMAIL");
        return (valor == null || valor.isBlank())
                ? "admin@usa.edu.co"
                : valor.trim().toLowerCase();
    }
}
