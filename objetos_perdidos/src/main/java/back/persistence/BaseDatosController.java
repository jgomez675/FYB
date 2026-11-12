package back.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.api.SessionManager;
import back.config.AppConfig;
import back.persistence.repository.CategoriaRepository;
import back.persistence.repository.MovimientoPuntosRepository;
import back.persistence.repository.ObjetoRepository;
import back.persistence.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/** Para comprobar rápido que la base de datos tiene los datos: GET /api/db/resumen (solo admin). */
@RestController
@RequestMapping("/api/db")
public class BaseDatosController {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ObjetoRepository objetos;
    private final MovimientoPuntosRepository movimientos;

    public BaseDatosController(
            UsuarioRepository usuarios,
            CategoriaRepository categorias,
            ObjetoRepository objetos,
            MovimientoPuntosRepository movimientos) {

        this.usuarios = usuarios;
        this.categorias = categorias;
        this.objetos = objetos;
        this.movimientos = movimientos;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen(HttpServletRequest request) {
        SessionManager.Sesion sesion = SessionManager.obtener(leerCookie(request, "fyb_session"));

        if (sesion == null) {
            throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        }

        if (!sesion.correo().equalsIgnoreCase(AppConfig.CORREO_ADMIN)) {
            throw new RuntimeException("No tienes permisos para ver esta información.");
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("usuarios", usuarios.count());
        respuesta.put("categorias", categorias.count());
        respuesta.put("objetos", objetos.count());
        respuesta.put("movimientosPuntos", movimientos.count());
        return respuesta;
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
