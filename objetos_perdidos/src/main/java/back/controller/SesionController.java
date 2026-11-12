package back.controller;

import back.api.SessionManager;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SesionController {

    @GetMapping("/api/sesion")
    public Map<String, Object> sesion(HttpServletRequest request) {

        String token = leerCookie(request, "fyb_session");

        SessionManager.Sesion sesion =
                SessionManager.obtener(token);

        if (sesion == null) {
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("usuario", null);
            return respuesta;
        }

        return Map.of(
                "usuario",
                Map.of(
                        "nombre", sesion.nombre(),
                        "correo", sesion.correo()
                )
        );
    }

    private String leerCookie(
            HttpServletRequest request,
            String nombre) {

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