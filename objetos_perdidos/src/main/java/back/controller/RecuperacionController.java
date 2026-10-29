package back.controller;

import back.service.RecuperacionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HU: Como estudiante de la Sergio, quiero recuperar mi contraseña mediante
 * un procedimiento seguro para volver a ingresar a la aplicación si la olvido.
 *
 * Son rutas públicas: quien olvidó la contraseña no tiene sesión.
 */
@RestController
@RequestMapping("/api/recuperar")
public class RecuperacionController {

    private final RecuperacionService servicio;

    public RecuperacionController(RecuperacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/solicitar")
    public Map<String, Object> solicitar(@RequestBody Map<String, Object> cuerpo) {

        servicio.solicitar(texto(cuerpo, "correo"));

        // Mismo mensaje exista o no la cuenta.
        return Map.of(
                "ok", true,
                "mensaje",
                "Si el correo está registrado, te enviamos un código de 6 dígitos. Vence en "
                        + RecuperacionService.MINUTOS_DE_VIGENCIA + " minutos."
        );
    }

    @PostMapping("/restablecer")
    public Map<String, Object> restablecer(@RequestBody Map<String, Object> cuerpo) {

        servicio.restablecer(
                texto(cuerpo, "correo"),
                texto(cuerpo, "codigo"),
                texto(cuerpo, "contrasena"),
                texto(cuerpo, "confirmar")
        );

        return Map.of("ok", true);
    }

    private String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        return valor == null ? "" : valor.toString();
    }
}
