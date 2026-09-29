package back.controller;

import back.api.SessionManager;
import back.service.ObjetoPerdidoService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/imagenes")
public class ImagenesController {

    private static final Pattern NOMBRE_IMAGEN =
            Pattern.compile("objeto_\\d+\\.(jpg|jpeg|png|gif|webp)");

    @GetMapping("/{nombre}")
    public ResponseEntity<ByteArrayResource> imagen(
            @PathVariable String nombre,
            HttpServletRequest request) throws IOException {

        requerirSesion(request);

        if (!NOMBRE_IMAGEN.matcher(nombre).matches()) {
            return ResponseEntity.notFound().build();
        }

        Path archivo = ObjetoPerdidoService
                .carpetaImagenes()
                .resolve(nombre)
                .normalize();

        if (!Files.isRegularFile(archivo)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Files.readAllBytes(archivo);

        ByteArrayResource recurso =
                new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(tipoImagen(nombre))
                .cacheControl(
                        CacheControl
                                .maxAge(3600, TimeUnit.SECONDS)
                                .cachePrivate()
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(recurso);
    }

    private void requerirSesion(
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

    private MediaType tipoImagen(String nombre) {

        if (nombre.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }

        if (nombre.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }

        if (nombre.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }

        return MediaType.IMAGE_JPEG;
    }
}