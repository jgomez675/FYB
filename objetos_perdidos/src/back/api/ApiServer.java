package back.api;

import back.config.AppConfig;
import back.model.ObjetoPerdido;
import back.model.Usuario;
import back.service.AuthService;
import back.service.ObjetoPerdidoService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Servidor HTTP del proyecto (usa el servidor incluido en el JDK, no necesita librerías).
 * Expone la API en /api/... y entrega el front (src/front) en el resto de rutas.
 *
 *   GET  /api/config          nombre de la app, eslogan y dominio de correo
 *   POST /api/registro        crea una cuenta
 *   POST /api/login           inicia sesión (cookie)
 *   POST /api/logout          cierra sesión
 *   GET  /api/sesion          usuario de la sesión actual (o null)
 *   GET  /api/objetos         lista de objetos              [requiere sesión]
 *   GET  /api/objetos/{id}    detalle de un objeto          [requiere sesión]
 *   POST /api/objetos         publica un objeto             [requiere sesión]
 *   GET  /api/imagenes/{f}    foto de un objeto             [requiere sesión]
 */
public final class ApiServer {

    private static final Pattern NOMBRE_IMAGEN = Pattern.compile("objeto_\\d+\\.(jpg|jpeg|png|gif|webp)");

    private ApiServer() {
    }

    public static void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(AppConfig.PUERTO), 0);
        servidor.createContext("/", ApiServer::atender);
        servidor.setExecutor(Executors.newFixedThreadPool(16));
        servidor.start();

        System.out.println(AppConfig.NOMBRE + " funcionando en http://localhost:" + AppConfig.PUERTO);
    }

    /* ---------------- Entrada de todas las peticiones ---------------- */

    private static void atender(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            String metodo = ex.getRequestMethod();

            if (ruta.startsWith("/api/")) {
                api(ex, metodo, ruta);
            } else {
                ArchivosEstaticos.servir(ex, metodo, ruta);
            }
        } catch (ApiException e) {
            responderError(ex, e.estado, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            responderError(ex, 500, "Error interno del servidor.");
        } finally {
            ex.close();
        }
    }

    private static void responderError(HttpExchange ex, int estado, String mensaje) {
        try {
            Http.json(ex, estado, mapa("mensaje", mensaje));
        } catch (Exception ignorada) {
            // la respuesta ya había empezado a enviarse
        }
    }

    private static void api(HttpExchange ex, String metodo, String ruta) throws IOException {
        if (!metodo.equals("GET") && !metodo.equals("HEAD")) {
            Http.verificarOrigen(ex);
        }

        switch (ruta) {
            case "/api/config":
                exigir(metodo, "GET");
                config(ex);
                return;
            case "/api/registro":
                exigir(metodo, "POST");
                registro(ex);
                return;
            case "/api/login":
                exigir(metodo, "POST");
                login(ex);
                return;
            case "/api/logout":
                exigir(metodo, "POST");
                logout(ex);
                return;
            case "/api/sesion":
                exigir(metodo, "GET");
                sesion(ex);
                return;
            case "/api/objetos":
                if (metodo.equals("GET")) listarObjetos(ex);
                else if (metodo.equals("POST")) publicarObjeto(ex);
                else throw new ApiException(405, "Método no permitido.");
                return;
            default:
                break;
        }

        if (ruta.startsWith("/api/objetos/")) {
            exigir(metodo, "GET");
            detalleObjeto(ex, ruta.substring("/api/objetos/".length()));
        } else if (ruta.startsWith("/api/imagenes/")) {
            exigir(metodo, "GET");
            imagen(ex, ruta.substring("/api/imagenes/".length()));
        } else {
            throw new ApiException(404, "No encontrado.");
        }
    }

    private static void exigir(String metodo, String esperado) {
        if (!metodo.equals(esperado)) {
            throw new ApiException(405, "Método no permitido.");
        }
    }

    /* ---------------- Configuración y cuentas ---------------- */

    private static void config(HttpExchange ex) throws IOException {
        Http.json(ex, 200, mapa(
                "nombre", AppConfig.NOMBRE,
                "eslogan", AppConfig.ESLOGAN,
                "dominioCorreo", AppConfig.DOMINIO_CORREO
        ));
    }

    private static void registro(HttpExchange ex) throws IOException {
        Map<String, Object> cuerpo = Http.leerJson(ex);

        String resultado = AuthService.registrarUsuario(
                Http.texto(cuerpo, "nombre"),
                Http.texto(cuerpo, "correo"),
                Http.texto(cuerpo, "contrasena"),
                Http.texto(cuerpo, "confirmar")
        );

        if (AuthService.REGISTRO_EXITOSO.equals(resultado)) {
            Http.json(ex, 200, mapa("ok", true));
        } else {
            throw new ApiException(400, resultado);
        }
    }

    private static void login(HttpExchange ex) throws IOException {
        Map<String, Object> cuerpo = Http.leerJson(ex);
        String correo = Http.texto(cuerpo, "correo").trim();
        String contrasena = Http.texto(cuerpo, "contrasena");

        if (correo.isEmpty() || contrasena.isEmpty()) {
            throw new ApiException(400, "Completa todos los campos.");
        }

        String clave = Http.ipCliente(ex) + "|" + correo.toLowerCase();
        if (IntentosFallidos.bloqueado(clave)) {
            throw new ApiException(429, "Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.");
        }

        Usuario usuario = AuthService.iniciarSesion(correo, contrasena);
        if (usuario == null) {
            IntentosFallidos.registrarFallo(clave);
            throw new ApiException(401, "Correo o contraseña incorrectos.");
        }

        IntentosFallidos.limpiar(clave);

        String token = SessionManager.crear(usuario.getNombre(), usuario.getCorreo());
        Http.ponerCookieSesion(ex, token, SessionManager.DURACION_SEGUNDOS);
        Http.json(ex, 200, mapa("usuario", usuarioJson(usuario.getNombre(), usuario.getCorreo())));
    }

    private static void logout(HttpExchange ex) throws IOException {
        SessionManager.cerrar(Http.leerCookie(ex, Http.COOKIE_SESION));
        Http.ponerCookieSesion(ex, "", 0);
        Http.json(ex, 200, mapa("ok", true));
    }

    private static void sesion(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = SessionManager.obtener(Http.leerCookie(ex, Http.COOKIE_SESION));
        Http.json(ex, 200, mapa("usuario", sesion == null ? null : usuarioJson(sesion.nombre(), sesion.correo())));
    }

    private static SessionManager.Sesion requerirSesion(HttpExchange ex) {
        SessionManager.Sesion sesion = SessionManager.obtener(Http.leerCookie(ex, Http.COOKIE_SESION));
        if (sesion == null) {
            throw new ApiException(401, "Tu sesión expiró. Inicia sesión de nuevo.");
        }
        return sesion;
    }

    /* ---------------- Objetos perdidos ---------------- */

    private static void listarObjetos(HttpExchange ex) throws IOException {
        requerirSesion(ex);

        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        objetos.sort(Comparator.comparingInt(ObjetoPerdido::getId).reversed()); // más recientes primero

        List<Object> lista = new ArrayList<>();
        for (ObjetoPerdido objeto : objetos) lista.add(objetoJson(objeto));

        Http.json(ex, 200, mapa("objetos", lista));
    }

    private static void detalleObjeto(HttpExchange ex, String idTexto) throws IOException {
        requerirSesion(ex);

        ObjetoPerdido objeto = null;
        try {
            objeto = ObjetoPerdidoService.obtenerObjeto(Integer.parseInt(idTexto));
        } catch (NumberFormatException ignorada) {
            // se responde 404 igual que un id que no existe
        }

        if (objeto == null) throw new ApiException(404, "No encontramos este objeto.");
        Http.json(ex, 200, mapa("objeto", objetoJson(objeto)));
    }

    private static void publicarObjeto(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        Map<String, Object> cuerpo = Http.leerJson(ex);

        String imagen = Http.texto(cuerpo, "imagen");
        Path temporal = null;

        try {
            if (!imagen.isBlank()) {
                ImagenUtil.Imagen decodificada = ImagenUtil.decodificar(imagen);
                temporal = Files.createTempFile("fyb_", decodificada.extension());
                Files.write(temporal, decodificada.bytes());
            }

            // El autor sale de la sesión, nunca de lo que diga el navegador.
            String resultado = ObjetoPerdidoService.guardarObjeto(
                    Http.texto(cuerpo, "nombre"),
                    Http.texto(cuerpo, "descripcion"),
                    Http.texto(cuerpo, "lugar"),
                    Http.texto(cuerpo, "fecha"),
                    temporal,
                    sesion.correo()
            );

            if (ObjetoPerdidoService.OBJETO_GUARDADO.equals(resultado)) {
                Http.json(ex, 201, mapa("ok", true));
            } else {
                throw new ApiException(400, resultado);
            }
        } finally {
            if (temporal != null) Files.deleteIfExists(temporal);
        }
    }

    private static void imagen(HttpExchange ex, String nombre) throws IOException {
        requerirSesion(ex);

        if (!NOMBRE_IMAGEN.matcher(nombre).matches()) {
            throw new ApiException(404, "No encontrado.");
        }

        Path archivo = ObjetoPerdidoService.carpetaImagenes().resolve(nombre);
        if (!Files.isRegularFile(archivo)) {
            throw new ApiException(404, "No encontrado.");
        }

        Headers cabeceras = ex.getResponseHeaders();
        cabeceras.set("Content-Type", tipoImagen(nombre));
        cabeceras.set("Cache-Control", "private, max-age=3600");
        Http.cabecerasDeSeguridad(cabeceras);

        ex.sendResponseHeaders(200, Files.size(archivo));
        try (OutputStream salida = ex.getResponseBody()) {
            Files.copy(archivo, salida);
        }
    }

    private static String tipoImagen(String nombre) {
        if (nombre.endsWith(".png")) return "image/png";
        if (nombre.endsWith(".gif")) return "image/gif";
        if (nombre.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    /* ---------------- Formato de las respuestas ---------------- */

    private static Map<String, Object> usuarioJson(String nombre, String correo) {
        return mapa("nombre", nombre, "correo", correo);
    }

    private static Map<String, Object> objetoJson(ObjetoPerdido o) {
        // data/objetos.txt puede traer rutas de Windows (data\objetos\objeto_1.jpg): solo importa el nombre del archivo.
        String ruta = o.getImagen() == null ? "" : o.getImagen().replace('\\', '/');
        String archivo = ruta.substring(ruta.lastIndexOf('/') + 1);

        return mapa(
                "id", o.getId(),
                "nombre", o.getNombre(),
                "descripcion", o.getDescripcion(),
                "lugar", o.getLugar(),
                "fecha", o.getFecha(),
                "imagenUrl", NOMBRE_IMAGEN.matcher(archivo).matches() ? "/api/imagenes/" + archivo : "",
                "correoUsuario", o.getCorreoUsuario()
        );
    }

    private static Map<String, Object> mapa(Object... paresClaveValor) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            resultado.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return resultado;
    }
}
