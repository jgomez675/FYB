package back.service;

import back.model.Estudiante;
import back.model.Usuario;

/**
 * Guarda quién tiene la sesión abierta en la aplicación.
 *
 * Hace falta porque LoginView, al iniciar sesión correctamente, solo
 * muestra el mensaje de bienvenida: el Usuario que devuelve AuthService
 * se pierde ahí mismo. Con esta clase, cualquier pantalla posterior
 * (lista de objetos, detalle, recompensas) puede preguntar
 * "¿quién está usando la app?" y otorgarle puntos.
 *
 * Es estática a propósito, siguiendo el mismo estilo de AuthService.
 *
 * Uso desde LoginView, justo donde hoy dice "Bienvenido, ...":
 *
 *     Usuario usuario = AuthService.iniciarSesion(correo, contraseña);
 *     if (usuario != null) {
 *         SesionActual.iniciarSesion(usuario);
 *         // ... seguir a la siguiente pantalla
 *     }
 */
public class SesionActual {

    private static Estudiante estudianteActual;

    private SesionActual() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Registra al usuario recién autenticado como la sesión activa y
     * le asocia su Estudiante (con los puntos que ya tuviera guardados).
     */
    public static void iniciarSesion(Usuario usuario) {
        estudianteActual = ServicioEstudiantes.obtenerOCrearEstudiante(usuario);
    }

    /**
     * @return el estudiante con sesión abierta, o null si nadie ha iniciado sesión.
     */
    public static Estudiante getEstudianteActual() {
        return estudianteActual;
    }

    /**
     * @return true si hay una sesión abierta.
     */
    public static boolean haySesionAbierta() {
        return estudianteActual != null;
    }

    /**
     * Cierra la sesión. Los puntos ya quedaron guardados en disco por
     * ServicioPuntos/ServicioRecompensas, así que no se pierde nada.
     */
    public static void cerrarSesion() {
        estudianteActual = null;
    }
}
