package back.controller;

import back.api.SessionManager;
import back.service.ReportePublicacionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    @PostMapping
    public Map<String, Object> reportarPublicacion(
            @RequestBody Map<String, Object> cuerpo,
            HttpServletRequest request) {

        SessionManager.Sesion sesion = requerirSesion(request);

        int objetoId;
        try {
            objetoId = Integer.parseInt(texto(cuerpo, "objetoId"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("El objeto seleccionado no es válido.");
        }

        String resultado = ReportePublicacionService.guardarReporte(
                objetoId,
                sesion.correo(),
                texto(cuerpo, "motivo"),
                texto(cuerpo, "detalle")
        );

        if (!"REPORTE_GUARDADO".equals(resultado)) {
            throw new RuntimeException(resultado);
        }

        return Map.of(
                "ok", true,
                "mensaje", "El reporte fue enviado correctamente."
        );
    }

    private SessionManager.Sesion requerirSesion(HttpServletRequest request) {
        String token = leerCookie(request, "fyb_session");
        SessionManager.Sesion sesion = SessionManager.obtener(token);

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        return sesion;
    }

    private String leerCookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        return valor == null ? "" : valor.toString();
    }
}
