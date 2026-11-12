package back.recompensas;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.api.SessionManager;
import back.service.PuntosService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET  /api/recompensas               catálogo + saldo + canjes del estudiante en sesión
 * POST /api/recompensas/{id}/canjear  canjea la recompensa si el saldo alcanza
 *
 * Siempre se usa el correo de la sesión: nadie puede canjear ni ver canjes de otra persona.
 */
@RestController
@RequestMapping("/api/recompensas")
public class RecompensasController {

    private final RecompensasService servicio;

    public RecompensasController(RecompensasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> listar(HttpServletRequest request) {
        SessionManager.Sesion sesion = requerirSesion(request);

        List<RecompensasService.Recompensa> catalogo = servicio.catalogo();

        return Map.of(
                "saldo", PuntosService.obtenerSaldo(sesion.correo()),
                "recompensas", catalogo,
                "canjes", servicio.misCanjes(sesion.correo()));
    }

    @PostMapping("/{id}/canjear")
    public Map<String, Object> canjear(@PathVariable int id, HttpServletRequest request) {
        SessionManager.Sesion sesion = requerirSesion(request);

        RecompensasService.ResultadoCanje resultado = servicio.canjear(sesion.correo(), id);

        return Map.of(
                "mensaje", "¡Canje exitoso! Presenta el código " + resultado.canje().codigo()
                        + " para reclamar tu recompensa.",
                "canje", resultado.canje(),
                "saldo", resultado.saldo());
    }

    private SessionManager.Sesion requerirSesion(HttpServletRequest request) {
        SessionManager.Sesion sesion = SessionManager.obtener(leerCookie(request, "fyb_session"));

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }
        return sesion;
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
