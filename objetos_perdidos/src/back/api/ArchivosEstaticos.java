package back.api;

import back.config.AppConfig;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/** Entrega el front (HTML, CSS y JS de src/front) sin permitir salirse de esa carpeta. */
final class ArchivosEstaticos {

    private static final Path RAIZ = Paths.get(AppConfig.CARPETA_FRONT).toAbsolutePath().normalize();

    private static final Map<String, String> TIPOS = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".js", "text/javascript; charset=utf-8",
            ".svg", "image/svg+xml",
            ".png", "image/png",
            ".ico", "image/x-icon",
            ".txt", "text/plain; charset=utf-8"
    );

    private ArchivosEstaticos() {
    }

    static void servir(HttpExchange ex, String metodo, String ruta) throws IOException {
        boolean get = metodo.equals("GET");
        if (!get && !metodo.equals("HEAD")) {
            throw new ApiException(405, "Método no permitido.");
        }

        if (ruta.equals("/")) ruta = "/index.html";

        Path archivo;
        try {
            archivo = RAIZ.resolve("." + ruta).normalize();
        } catch (InvalidPathException e) {
            throw new ApiException(404, "No encontrado.");
        }

        if (!archivo.startsWith(RAIZ) || tienePuntoInicial(archivo) || !Files.isRegularFile(archivo)) {
            throw new ApiException(404, "No encontrado.");
        }

        String nombre = archivo.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        String tipo = punto < 0 ? null : TIPOS.get(nombre.substring(punto).toLowerCase());
        if (tipo == null) {
            throw new ApiException(404, "No encontrado.");
        }

        Headers cabeceras = ex.getResponseHeaders();
        cabeceras.set("Content-Type", tipo);
        cabeceras.set("Cache-Control", "no-cache");
        Http.cabecerasDeSeguridad(cabeceras);

        if (!get) {
            ex.sendResponseHeaders(200, -1);
            return;
        }

        ex.sendResponseHeaders(200, Files.size(archivo));
        try (OutputStream salida = ex.getResponseBody()) {
            Files.copy(archivo, salida);
        }
    }

    // No se sirven archivos ocultos (.git, .env...).
    private static boolean tienePuntoInicial(Path archivo) {
        for (Path parte : RAIZ.relativize(archivo)) {
            if (parte.toString().startsWith(".")) return true;
        }
        return false;
    }
}
