package back.categorias;

import back.api.SessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET    /api/categorias                 -> cualquier usuario con sesión
 * POST   /api/categorias   {nombre}      -> solo administrador
 * PUT    /api/categorias   {anterior, nombre} -> solo administrador
 * DELETE /api/categorias?nombre=...      -> solo administrador
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriasController {

    @GetMapping
    public Map<String, Object> listar(HttpServletRequest request) {
        requerirSesion(request);
        return Map.of("categorias", CategoriasService.listar());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @RequestBody Map<String, Object> cuerpo,
            HttpServletRequest request) {

        if (!esAdmin(request)) {
            return prohibido();
        }

        String r = CategoriasService.crear(texto(cuerpo, "nombre"));
        return responder(HttpStatus.CREATED, r);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> editar(
            @RequestBody Map<String, Object> cuerpo,
            HttpServletRequest request) {

        if (!esAdmin(request)) {
            return prohibido();
        }

        String r = CategoriasService.editar(texto(cuerpo, "anterior"), texto(cuerpo, "nombre"));
        return responder(HttpStatus.OK, r);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> eliminar(
            @RequestParam("nombre") String nombre,
            HttpServletRequest request) {

        if (!esAdmin(request)) {
            return prohibido();
        }

        String r = CategoriasService.eliminar(nombre);
        return responder(HttpStatus.OK, r);
    }

    private ResponseEntity<Map<String, Object>> responder(HttpStatus exito, String resultado) {
        if (!CategoriasService.OK.equals(resultado)) {
            // RuntimeException con mensaje para el usuario: ManejadorErrores la convierte en 400.
            throw new RuntimeException(resultado);
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ok", true);
        cuerpo.put("categorias", CategoriasService.listar());
        return ResponseEntity.status(exito).body(cuerpo);
    }

    private ResponseEntity<Map<String, Object>> prohibido() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensaje", "Esta opción es solo para el administrador."));
    }

    private boolean esAdmin(HttpServletRequest request) {
        return AdminConfig.esAdmin(requerirSesion(request).correo());
    }

    private SessionManager.Sesion requerirSesion(HttpServletRequest request) {
        SessionManager.Sesion sesion = SessionManager.obtener(leerCookie(request, "fyb_session"));

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        return sesion;
    }

    private String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
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
