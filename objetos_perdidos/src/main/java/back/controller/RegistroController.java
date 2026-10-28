package back.controller;

import back.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RegistroController {

    @PostMapping("/api/registro")
    public Map<String, Object> registrar(@RequestBody Map<String, Object> cuerpo) {

        String resultado = AuthService.registrarUsuario(
                texto(cuerpo, "nombre"),
                texto(cuerpo, "correo"),
                texto(cuerpo, "contrasena"),
                texto(cuerpo, "confirmar")
        );

        if (AuthService.REGISTRO_EXITOSO.equals(resultado)) {
            return Map.of("ok", true);
        }

        throw new RuntimeException(resultado);
    }

    private String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        return valor == null ? "" : valor.toString();
    }
}