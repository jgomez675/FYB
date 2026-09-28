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

        return Map.of(
                "ok", true,
                "verificado", true,
                "mensaje",
                "La característica coincide. El objeto puede ser verificado."
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