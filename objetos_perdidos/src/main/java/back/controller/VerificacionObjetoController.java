package back.controller;

import back.api.SessionManager;
import back.model.ObjetoPerdido;
import back.service.ObjetoPerdidoService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/objetos")
public class VerificacionObjetoController {

    @PostMapping("/{id}/verificar")
    public Map<String, Object> verificar(
            @PathVariable int id,
            @RequestBody Map<String, Object> cuerpo,
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

        String respuesta =
                texto(cuerpo, "caracteristica");

        if (respuesta.isBlank()) {
            throw new RuntimeException(
                    "Debes ingresar la característica del objeto."
            );
        }

        boolean correcta =
                objeto.getCaracteristicaPrivada()
                        .equalsIgnoreCase(respuesta.trim());

        if (!correcta) {
            return Map.of(
                    "ok", false,
                    "verificado", false,
                    "mensaje",
                    "La característica no coincide."
            );
        }

        String correoEstudiante =
                sesion.correo();

        /*
         * El estudiante que publicó el objeto
         * no puede ser su propio descubridor.
         */
        if (objeto.getCorreoUsuario()
                .equalsIgnoreCase(correoEstudiante)) {

            return Map.of(
                    "ok", false,
                    "verificado", false,
                    "esPropietario", true,
                    "mensaje",
                    "No puedes verificar tu propio objeto."
            );
        }

        /*
         * Registramos al estudiante que encontró
         * el objeto.
         */
        boolean descubridorRegistrado =
                ObjetoPerdidoService.registrarDescubridor(
                        id,
                        correoEstudiante
                );

        if (!descubridorRegistrado) {

            if (objeto.getCorreoDescubridor() != null &&
                    !objeto.getCorreoDescubridor().isBlank()) {

                return Map.of(
                        "ok", false,
                        "verificado", false,
                        "mensaje",
                        "Este objeto ya tiene un estudiante registrado como descubridor."
                );
            }

            return Map.of(
                    "ok", false,
                    "verificado", false,
                    "mensaje",
                    "No se pudo registrar al estudiante como descubridor."
            );
        }

        return Map.of(
                "ok", true,
                "verificado", true,
                "descubridorRegistrado", true,
                "mensaje",
                "La característica coincide. Quedaste registrado como descubridor del objeto."
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
                    "Tu sesión expiró. Inicia sesión de nuevo."
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

    private String texto(
            Map<String, Object> cuerpo,
            String clave) {

        Object valor = cuerpo.get(clave);

        return valor == null
                ? ""
                : valor.toString();
    }
}