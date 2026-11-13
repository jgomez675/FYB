package back.controller;

import back.api.SessionManager;
import back.model.ObjetoPerdido;
import back.service.ObjetoPerdidoService;
import back.service.ReclamacionesService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

/**
 * Centro de actividad de FYB. Las notificaciones se construyen desde los eventos
 * ya persistidos de reclamaciones y devoluciones; no duplica ni altera puntos.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesController {

    @GetMapping
    public Map<String, Object> listar(HttpServletRequest request) {
        String correo = obtenerSesion(request).correo();
        List<Map<String, Object>> salida = new ArrayList<>();

        for (ReclamacionesService.Reclamacion r : ReclamacionesService.todas()) {
            boolean recibida = r.propietario().equalsIgnoreCase(correo);
            boolean enviada = r.solicitante().equalsIgnoreCase(correo);
            if (!recibida && !enviada) continue;

            String titulo;
            String mensaje;
            String tipo;
            if (recibida && "PENDIENTE".equals(r.estado())) {
                titulo = "Nueva reclamación";
                mensaje = "Un estudiante envió una solicitud para: " + r.objetoNombre();
                tipo = "RECLAMACION_RECIBIDA";
            } else if (enviada && "APROBADA".equals(r.estado())) {
                titulo = "Reclamación aprobada";
                mensaje = "El propietario aprobó tu solicitud para: " + r.objetoNombre() + ". Coordina la entrega.";
                tipo = "RECLAMACION_APROBADA";
            } else if (enviada && "RECHAZADA".equals(r.estado())) {
                titulo = "Reclamación rechazada";
                mensaje = "El propietario rechazó tu solicitud para: " + r.objetoNombre();
                tipo = "RECLAMACION_RECHAZADA";
            } else {
                continue;
            }
            salida.add(notificacion(r.id(), tipo, titulo, mensaje, r.creada(), "/#/mis-reclamaciones"));
        }

        for (ObjetoPerdido o : ObjetoPerdidoService.obtenerObjetos()) {
            if (o.estaDevuelto() && (o.getCorreoUsuario().equalsIgnoreCase(correo)
                    || (o.getCorreoDescubridor() != null && o.getCorreoDescubridor().equalsIgnoreCase(correo)))) {
                String ruta = o.getCorreoUsuario().equalsIgnoreCase(correo) ? "/#/puntos" : "/#/mis-reclamaciones";
                salida.add(notificacion("devolucion-" + o.getId() + "-" + correo, "DEVOLUCION_CONFIRMADA",
                        "Devolución confirmada", "La devolución de " + o.getNombre() + " quedó registrada.",
                        "", ruta));
            }
        }

        salida.sort((a, b) -> String.valueOf(b.get("fecha")).compareTo(String.valueOf(a.get("fecha"))));
        return Map.of("notificaciones", salida, "total", salida.size());
    }

    private Map<String, Object> notificacion(String id, String tipo, String titulo,
                                              String mensaje, String fecha, String ruta) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", id); n.put("tipo", tipo); n.put("titulo", titulo);
        n.put("mensaje", mensaje); n.put("fecha", fecha); n.put("ruta", ruta);
        return n;
    }

    private SessionManager.Sesion obtenerSesion(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie c : cookies) {
            if ("fyb_session".equals(c.getName())) {
                SessionManager.Sesion sesion = SessionManager.obtener(c.getValue());
                if (sesion != null) return sesion;
            }
        }
        throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
    }
}
