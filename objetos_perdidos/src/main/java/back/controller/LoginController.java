package back.controller;

import back.service.AuthService;
import back.api.SessionManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class LoginController {

    private static final long DURACION_COOKIE =
            7L * 24 * 3600;

    @PostMapping("/api/login")
    public Map<String, Object> login(
            @RequestBody Map<String, Object> cuerpo,
            jakarta.servlet.http.HttpServletResponse respuesta) {

        String correo = texto(cuerpo, "correo").trim();
        String contrasena = texto(cuerpo, "contrasena");

        if (correo.isEmpty() || contrasena.isEmpty()) {
            throw new RuntimeException(
                    "Completa todos los campos."
            );
        }

        var usuario = AuthService.iniciarSesion(
                correo,
                contrasena
        );

        if (usuario == null) {
            throw new RuntimeException(
                    "Correo o contraseña incorrectos."
            );
        }

        String token = SessionManager.crear(
                usuario.getNombre(),
                usuario.getCorreo()
        );

        ResponseCookie cookie = ResponseCookie
                .from("fyb_session", token)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(DURACION_COOKIE)
                .build();

        respuesta.setHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        /*
         * El frontend original espera solamente los datos
         * del usuario. La sesión queda guardada en la cookie.
         */
        return Map.of(
                "usuario",
                Map.of(
                        "nombre", usuario.getNombre(),
                        "correo", usuario.getCorreo()
                )
        );
    }

    private String texto(
            Map<String, Object> cuerpo,
            String clave) {

        Object valor = cuerpo.get(clave);

        return valor == null
                ? ""
                : valor.toString();
    }
}