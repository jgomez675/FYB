package back.controller;

import back.api.SessionManager;
import back.service.PuntosService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/puntos")
public class PuntosController {

    @GetMapping
    public Map<String, Object> obtenerPuntos(
            HttpServletRequest request) {

        SessionManager.Sesion sesion =
                requerirSesion(request);

        int saldo =
                PuntosService.obtenerSaldo(
                        sesion.correo()
                );

        return Map.of(
                "puntos", saldo
        );
    }

    private SessionManager.Sesion requerirSesion(
            HttpServletRequest request) {

        String token =
                leerCookie(
                        request,
                        "fyb_session"
                );

        SessionManager.Sesion sesion =
                SessionManager.obtener(token);

        if (sesion == null) {
            throw new RuntimeException(
                    "Tu sesión expiró. Inicia sesión de nuevo."
            );
        }

        return sesion;
    }

    private String leerCookie(
            HttpServletRequest request,
            String nombre) {

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (nombre.equals(
                    cookie.getName()
            )) {
                return cookie.getValue();
            }
        }

        return null;
    }
}