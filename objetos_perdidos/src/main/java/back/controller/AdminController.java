package back.controller;

import back.api.SessionManager;
import back.config.AppConfig;
import back.model.ObjetoPerdido;
import back.model.ReportePublicacion;
import back.model.Usuario;
import back.service.AuthService;
import back.service.ObjetoPerdidoService;
import back.service.ReportePublicacionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/resumen")
    public Map<String,Object> resumen(HttpServletRequest request) {
        requerirAdmin(request);
        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        long visibles = objetos.stream().filter(o -> !o.isOculto()).count();
        long encontrados = objetos.stream().filter(o -> !o.isOculto() && "ENCONTRADO".equalsIgnoreCase(o.getEstado())).count();
        long devueltos = objetos.stream().filter(o -> !o.isOculto() && o.estaDevuelto()).count();
        long reportesPendientes = ReportePublicacionService.listarReportes().stream()
                .filter(r -> "PENDIENTE".equalsIgnoreCase(r.getEstado())).count();
        return Map.of("totalObjetos", visibles, "encontrados", encontrados,
                "devueltos", devueltos, "reportesPendientes", reportesPendientes);
    }

    @GetMapping("/reportes")
    public Map<String,Object> reportes(HttpServletRequest request) {
        requerirAdmin(request);
        List<Map<String,Object>> salida = new ArrayList<>();
        for (ReportePublicacion r : ReportePublicacionService.listarReportes()) {
            ObjetoPerdido objeto = ObjetoPerdidoService.obtenerObjeto(r.getObjetoId());
            Map<String,Object> fila = new LinkedHashMap<>();
            fila.put("id", r.getId()); fila.put("objetoId", r.getObjetoId());
            fila.put("nombreObjeto", objeto == null ? "Publicación retirada" : objeto.getNombre());
            fila.put("correoReportante", r.getCorreoUsuario()); fila.put("motivo", r.getMotivo());
            fila.put("detalle", r.getDetalle()); fila.put("fecha", r.getFecha()); fila.put("estado", r.getEstado());
            salida.add(fila);
        }
        return Map.of("reportes", salida);
    }

    @PostMapping("/reportes/{id}/decision")
    public Map<String,Object> decision(@PathVariable int id, @RequestBody Map<String,String> body, HttpServletRequest request) {
        requerirAdmin(request);
        String decision = body.getOrDefault("decision", "").toUpperCase(Locale.ROOT);
        if (!ReportePublicacionService.resolverReporte(id, decision))
            throw new RuntimeException("No se pudo aplicar la decisión. Verifica el reporte y la publicación.");
        return Map.of("ok", true, "mensaje", "La decisión fue guardada.");
    }

    @GetMapping("/usuarios")
    public Map<String,Object> usuarios(HttpServletRequest request) {
        requerirAdmin(request);
        List<Map<String,Object>> salida = new ArrayList<>();
        for (Usuario u : AuthService.listarUsuarios()) {
            salida.add(Map.of("nombre", u.getNombre(), "correo", u.getCorreo(), "bloqueado", u.isBloqueado()));
        }
        return Map.of("usuarios", salida);
    }

    @PostMapping("/usuarios/bloqueo")
    public Map<String,Object> bloqueo(@RequestBody Map<String,Object> body, HttpServletRequest request) {
        SessionManager.Sesion admin = requerirAdmin(request);
        String correo = String.valueOf(body.getOrDefault("correo", ""));
        boolean bloquear = Boolean.parseBoolean(String.valueOf(body.getOrDefault("bloqueado", "true")));
        if (correo.isBlank()) throw new RuntimeException("Debes indicar el correo del usuario.");
        if (correo.equalsIgnoreCase(admin.correo())) throw new RuntimeException("No puedes bloquear tu propia cuenta administrativa.");
        if (AuthService.buscarUsuario(correo) == null) throw new RuntimeException("El usuario no existe.");
        if (!AuthService.establecerBloqueo(correo, bloquear)) throw new RuntimeException("No se pudo guardar el cambio.");
        if (bloquear) SessionManager.cerrarTodas(correo);
        return Map.of("ok", true, "mensaje", bloquear ? "Cuenta bloqueada." : "Cuenta desbloqueada.");
    }

    private SessionManager.Sesion requerirAdmin(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = null;
        if (cookies != null) for (Cookie c : cookies) if ("fyb_session".equals(c.getName())) token = c.getValue();
        SessionManager.Sesion sesion = SessionManager.obtener(token);
        if (sesion == null) throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        if (!sesion.correo().equalsIgnoreCase(AppConfig.CORREO_ADMIN))
            throw new RuntimeException("No tienes permisos de administrador.");
        return sesion;
    }
}
