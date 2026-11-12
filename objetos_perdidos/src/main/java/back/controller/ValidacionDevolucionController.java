package back.controller;

import back.api.SessionManager;
import back.model.ObjetoPerdido;
import back.service.ObjetoPerdidoService;
import back.service.PuntosService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/objetos")
public class ValidacionDevolucionController {

    @PostMapping("/{id}/validar-devolucion")
    public Map<String, Object> validarDevolucion(
            @PathVariable int id,
            HttpServletRequest request) {

        SessionManager.Sesion sesion =
                requerirSesion(request);

        ObjetoPerdido objeto =
                ObjetoPerdidoService.obtenerObjeto(id);

        if (objeto == null) {
            throw new RuntimeException(
                    "No encontramos este objeto."
            );
        }

        String correoEstudiante =
                sesion.correo();

        String correoReportante =
                objeto.getCorreoUsuario();

        boolean esMismoEstudiante =
                correoEstudiante.equalsIgnoreCase(
                        correoReportante
                );

        // No puede devolver su propio objeto para ganar puntos.
        if (esMismoEstudiante) {
            return Map.of(
                    "ok", false,
                    "puedeDevolver", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "No puedes recibir puntos por devolver tu propio objeto."
            );
        }

        // El objeto no puede devolverse dos veces.
        if (objeto.estaDevuelto()) {
            return Map.of(
                    "ok", false,
                    "puedeDevolver", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "Este objeto ya fue devuelto."
            );
        }

        // Guarda la devolución.
        boolean devolucionGuardada =
                ObjetoPerdidoService.marcarComoDevuelto(
                        id,
                        correoEstudiante
                );

        if (!devolucionGuardada) {
            return Map.of(
                    "ok", false,
                    "puedeDevolver", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "No se pudo registrar la devolucion."
            );
        }

        // Entrega 20 puntos.
        int nuevoSaldo =
                PuntosService.agregarPuntos(
                        correoEstudiante,
                        PuntosService.PUNTOS_POR_DEVOLVER_OBJETO,
                        "Devolucion del objeto " +
                                objeto.getNombre()
                );

        return Map.of(
                "ok", true,
                "puedeDevolver", true,
                "puntosGanados",
                PuntosService.PUNTOS_POR_DEVOLVER_OBJETO,
                "saldoPuntos",
                nuevoSaldo,
                "mensaje",
                "Objeto devuelto correctamente. Ganaste " +
                        PuntosService.PUNTOS_POR_DEVOLVER_OBJETO +
                        " puntos."
        );
    }

    private SessionManager.Sesion requerirSesion(
            HttpServletRequest request) {

        String token = leerCookie(
                request,
                "fyb_session"
        );

        SessionManager.Sesion sesion =
                SessionManager.obtener(token);

        if (sesion == null) {
            throw new RuntimeException(
                    "Tu sesion expiro. Inicia sesion de nuevo."
            );
        }

        return sesion;
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