package back.historial;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.api.SessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/** GET /api/puntos/historial: movimientos de puntos del estudiante en sesión (ganados y gastados). */
@RestController
@RequestMapping("/api/puntos/historial")
public class HistorialPuntosController {

    private final HistorialPuntosService servicio;

    public HistorialPuntosController(HistorialPuntosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public HistorialPuntosService.Historial historial(HttpServletRequest request) {
        SessionManager.Sesion sesion = SessionManager.obtener(leerCookie(request, "fyb_session"));

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        // Siempre el correo de la sesión: nadie puede pedir el historial de otra persona.
        return servicio.obtener(sesion.correo());
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
