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
public class ConfirmacionDevolucionController {

    @PostMapping("/{id}/confirmar-devolucion")
    public Map<String, Object> confirmarDevolucion(
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

        // Solo el dueño del objeto puede confirmar
        // que lo recibió.
        if (!objeto.getCorreoUsuario()
                .equalsIgnoreCase(correoEstudiante)) {

            return Map.of(
                    "ok", false,
                    "confirmado", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "Solo el estudiante que reportó el objeto puede confirmar la devolución."
            );
        }

        // Evita confirmar dos veces el mismo objeto.
        if (objeto.estaDevuelto()) {

            return Map.of(
                    "ok", false,
                    "confirmado", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "Este objeto ya aparece como devuelto."
            );
        }

        String correoDescubridor =
                objeto.getCorreoDescubridor();

        // El objeto debe tener un estudiante
        // registrado como descubridor.
        if (correoDescubridor == null ||
                correoDescubridor.isBlank()) {

            return Map.of(
                    "ok", false,
                    "confirmado", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "Todavía no hay un estudiante registrado como descubridor."
            );
        }

        // Seguridad adicional: el dueño no puede
        // ser su propio descubridor.
        if (correoEstudiante
                .equalsIgnoreCase(correoDescubridor)) {

            return Map.of(
                    "ok", false,
                    "confirmado", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "El dueño del objeto no puede recibir puntos por su propia devolución."
            );
        }

        // Primero se marca el objeto como devuelto.
        boolean devolucionGuardada =
                ObjetoPerdidoService.marcarComoDevuelto(
                        id,
                        correoDescubridor
                );

        if (!devolucionGuardada) {

            return Map.of(
                    "ok", false,
                    "confirmado", false,
                    "puntosGanados", 0,
                    "mensaje",
                    "No se pudo registrar la devolución del objeto."
            );
        }

        /*
         * Los puntos se entregan al estudiante que
         * encontró el objeto.
         *
         * La cantidad depende de la categoría.
         */
        int puntosGanados =
                PuntosService.calcularPuntosDevolucion(
                        objeto.getCategoria()
                );

        int nuevoSaldo =
                PuntosService.agregarPuntosPorDevolucion(
                        correoDescubridor,
                        objeto.getCategoria(),
                        objeto.getNombre()
                );

        return Map.of(
                "ok", true,
                "confirmado", true,
                "puntosGanados", puntosGanados,
                "saldoPuntos", nuevoSaldo,
                "categoria", objeto.getCategoria(),
                "correoDescubridor", correoDescubridor,
                "mensaje",
                "Devolución confirmada correctamente. "
                        + "El estudiante que encontró el objeto recibió "
                        + puntosGanados
                        + " puntos."
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
                    "Debes iniciar sesión."
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
                    cookie.getName())) {

                return cookie.getValue();
            }
        }

        return null;
    }
}