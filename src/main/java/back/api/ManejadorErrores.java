package back.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Convierte los errores de los controladores en JSON con la forma
 * {"mensaje": "..."} y el código HTTP que el front espera:
 *
 *  - 401: sesión vencida o correo/contraseña incorrectos
 *  - 404: objeto no encontrado
 *  - 429: demasiadas solicitudes seguidas
 *  - 400: cualquier otro mensaje pensado para el usuario
 *  - 500: errores inesperados (el detalle solo se imprime en la consola)
 *
 * Sin esto, Spring responde un 500 genérico sin "mensaje" y el front
 * siempre muestra "Ocurrió un error inesperado".
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoInvalido(
            HttpMessageNotReadableException e) {

        return responder(
                HttpStatus.BAD_REQUEST,
                "La solicitud no es válida."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(
            MethodArgumentTypeMismatchException e) {

        return responder(
                HttpStatus.BAD_REQUEST,
                "La solicitud no es válida."
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> error(RuntimeException e) {

        // Los controladores lanzan RuntimeException "pelada" con un mensaje para el usuario.
        // Cualquier subclase (NullPointerException, IllegalStateException...) es un fallo real.
        boolean esMensajeDeUsuario =
                e.getClass() == RuntimeException.class
                        && e.getMessage() != null
                        && !e.getMessage().isBlank();

        if (!esMensajeDeUsuario) {
            e.printStackTrace();

            return responder(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ocurrió un error inesperado. Inténtalo de nuevo."
            );
        }

        return responder(estadoPara(e.getMessage()), e.getMessage());
    }

    private HttpStatus estadoPara(String mensaje) {

        String m = mensaje.toLowerCase();

        if (m.contains("sesión expiró")
                || m.contains("sesion expiro")
                || m.contains("debes iniciar sesión")
                || m.contains("correo o contraseña incorrectos")) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (m.contains("demasiados intentos")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        if (m.contains("no encontramos este objeto")) {
            return HttpStatus.NOT_FOUND;
        }

        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<Map<String, Object>> responder(
            HttpStatus estado,
            String mensaje) {

        return ResponseEntity
                .status(estado)
                .body(Map.of("mensaje", mensaje));
    }
}
