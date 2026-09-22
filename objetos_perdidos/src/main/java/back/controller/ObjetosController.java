package back.controller;

import back.api.ImagenUtil;
import back.api.SessionManager;
import back.model.ObjetoPerdido;
import back.service.ObjetoPerdidoService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/objetos")
public class ObjetosController {

    @GetMapping
    public Map<String, Object> listarObjetos(
            HttpServletRequest request) {

        requerirSesion(request);

        List<ObjetoPerdido> objetos =
                ObjetoPerdidoService.obtenerObjetos();

        objetos.sort(
                Comparator.comparingInt(ObjetoPerdido::getId).reversed()
        );

        List<Object> lista = new ArrayList<>();

        for (ObjetoPerdido objeto : objetos) {
            lista.add(objetoJson(objeto));
        }

        return Map.of("objetos", lista);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detalleObjeto(
            @PathVariable int id,
            HttpServletRequest request) {

        requerirSesion(request);

        ObjetoPerdido objeto =
                ObjetoPerdidoService.obtenerObjeto(id);

        if (objeto == null) {
            throw new RuntimeException(
                    "No encontramos este objeto."
            );
        }

        return Map.of("objeto", objetoJson(objeto));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> publicarObjeto(
            @RequestBody Map<String, Object> cuerpo,
            HttpServletRequest request) {

        SessionManager.Sesion sesion =
                requerirSesion(request);

        String imagen = texto(cuerpo, "imagen");

        Path temporal = null;

        try {
            if (!imagen.isBlank()) {

                ImagenUtil.Imagen decodificada =
                        ImagenUtil.decodificar(imagen);

                try {
                    temporal = Files.createTempFile(
                            "fyb_",
                            decodificada.extension()
                    );

                    Files.write(
                            temporal,
                            decodificada.bytes()
                    );

                } catch (Exception e) {
                    throw new RuntimeException(
                            "No se pudo procesar la imagen."
                    );
                }
            }

            String resultado =
                    ObjetoPerdidoService.guardarObjeto(
                            texto(cuerpo, "nombre"),
                            texto(cuerpo, "descripcion"),
                            texto(cuerpo, "lugar"),
                            texto(cuerpo, "fecha"),
                            temporal,
                            sesion.correo()
                    );

            if (!ObjetoPerdidoService.OBJETO_GUARDADO.equals(resultado)) {
                throw new RuntimeException(resultado);
            }

            return Map.of("ok", true);

        } finally {

            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (Exception ignored) {
                }
            }
        }
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

    private Map<String, Object> objetoJson(
            ObjetoPerdido objeto) {

        String ruta = objeto.getImagen() == null
                ? ""
                : objeto.getImagen().replace('\\', '/');

        String archivo = ruta.substring(
                ruta.lastIndexOf('/') + 1
        );

        String imagenUrl = "";

        if (archivo.matches(
                "objeto_\\d+\\.(jpg|jpeg|png|gif|webp)"
        )) {
            imagenUrl = "/api/imagenes/" + archivo;
        }

        Map<String, Object> resultado =
                new HashMap<>();

        resultado.put("id", objeto.getId());
        resultado.put("nombre", objeto.getNombre());
        resultado.put("descripcion", objeto.getDescripcion());
        resultado.put("lugar", objeto.getLugar());
        resultado.put("fecha", objeto.getFecha());
        resultado.put("imagenUrl", imagenUrl);
        resultado.put("correoUsuario", objeto.getCorreoUsuario());

        return resultado;
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