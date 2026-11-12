package back.controller;

import back.api.SessionManager;
import back.service.AuthService;
import back.model.Usuario;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HU: Como estudiante, quiero actualizar mi perfil para mantener vigentes mis datos.
 *
 * El correo sale siempre de la sesión (nunca del cliente), así que cada persona
 * solo puede editar su propia cuenta.
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    @GetMapping
    public Map<String, Object> verPerfil(HttpServletRequest request) {

        SessionManager.Sesion sesion = requerirSesion(request);

        return Map.of("usuario", usuarioJson(sesion.nombre(), sesion.correo()));
    }

    @PutMapping
    public Map<String, Object> actualizarPerfil(
            HttpServletRequest request,
            @RequestBody Map<String, Object> cuerpo) {

        SessionManager.Sesion sesion = requerirSesion(request);

        String resultado = AuthService.actualizarPerfil(
                sesion.correo(),
                texto(cuerpo, "nombre"),
                texto(cuerpo, "contrasenaActual"),
                texto(cuerpo, "contrasenaNueva"),
                texto(cuerpo, "confirmar")
        );

        if (!AuthService.PERFIL_ACTUALIZADO.equals(resultado)) {
            throw new RuntimeException(resultado);
        }

        Usuario usuario = AuthService.buscarUsuario(sesion.correo());

        if (usuario == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        // Las sesiones abiertas guardan el nombre: se actualiza para que se vea el cambio de inmediato.
        SessionManager.actualizarNombre(usuario.getCorreo(), usuario.getNombre());

        boolean cambioContraseña = !texto(cuerpo, "contrasenaNueva").isEmpty();

        if (cambioContraseña) {
            // Si alguien más tenía la cuenta abierta, tras cambiar la contraseña debe volver a entrar.
            SessionManager.cerrarOtras(usuario.getCorreo(), leerCookie(request, "fyb_session"));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ok", true);
        respuesta.put("contrasenaCambiada", cambioContraseña);
        respuesta.put("usuario", usuarioJson(usuario.getNombre(), usuario.getCorreo()));
        return respuesta;
    }

    private Map<String, Object> usuarioJson(String nombre, String correo) {
        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("correo", correo);
        return usuario;
    }

    private SessionManager.Sesion requerirSesion(HttpServletRequest request) {

        SessionManager.Sesion sesion =
                SessionManager.obtener(leerCookie(request, "fyb_session"));

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        return sesion;
    }

    private String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        return valor == null ? "" : valor.toString();
    }

    private String leerCookie(HttpServletRequest request, String nombre) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
