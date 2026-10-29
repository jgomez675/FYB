package back.api;

import back.config.AppConfig;
import back.model.ObjetoPerdido;
import back.model.Usuario;
import back.service.AuthService;
import back.service.FavoritosService;
import back.service.ObjetoPerdidoService;
import back.service.RecompensasService;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *   DELETE /api/objetos/{id}  retira una publicación propia [requiere sesión]
 *   GET  /api/imagenes/{f}    foto de un objeto             [requiere sesión]
 *   GET  /api/favoritos       objetos favoritos             [requiere sesión]
 *   PUT  /api/favoritos/{id}  guarda un favorito            [requiere sesión]
 *   DELETE /api/favoritos/{id} quita un favorito            [requiere sesión]
 *   GET  /api/perfil          datos de contacto             [requiere sesión]
 *   PUT  /api/perfil          actualiza los datos           [requiere sesión]
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
            case "/api/perfil":
                if (metodo.equals("GET")) verPerfil(ex);
                else if (metodo.equals("PUT")) actualizarPerfil(ex);
                else throw new ApiException(405, "Método no permitido.");
                return;
            case "/api/favoritos":
                exigir(metodo, "GET");
                listarFavoritos(ex);
                return;
            case "/api/puntos":
                exigir(metodo, "GET");
                puntos(ex);
                return;
            case "/api/devoluciones/confirmar":
                exigir(metodo, "POST");
                confirmarDevolucion(ex);
                return;
            case "/api/ruleta/girar":
                exigir(metodo, "POST");
                girarRuleta(ex);
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
            String id = ruta.substring("/api/objetos/".length());
            if (metodo.equals("GET")) detalleObjeto(ex, id);
            else if (metodo.equals("DELETE")) retirarObjeto(ex, id);
            else throw new ApiException(405, "Método no permitido.");
        } else if (ruta.startsWith("/api/favoritos/")) {
            String id = ruta.substring("/api/favoritos/".length());
            if (metodo.equals("PUT")) guardarFavorito(ex, id);
            else if (metodo.equals("DELETE")) quitarFavorito(ex, id);
            else throw new ApiException(405, "Método no permitido.");
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
        SessionManager.Sesion sesion = requerirSesion(ex);
        Set<Integer> favoritos = new HashSet<>(FavoritosService.idsDe(sesion.correo()));

        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        objetos.sort(Comparator.comparingInt(ObjetoPerdido::getId).reversed()); // más recientes primero

        List<Object> lista = new ArrayList<>();
        for (ObjetoPerdido objeto : objetos) lista.add(objetoJsonPara(objeto, sesion, favoritos));

        Http.json(ex, 200, mapa("objetos", lista));
    }

    private static void detalleObjeto(HttpExchange ex, String idTexto) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);

        ObjetoPerdido objeto = null;
        try {
            objeto = ObjetoPerdidoService.obtenerObjeto(Integer.parseInt(idTexto));
        } catch (NumberFormatException ignorada) {
            // se responde 404 igual que un id que no existe
        }

        if (objeto == null) throw new ApiException(404, "No encontramos este objeto.");

        Set<Integer> favoritos = new HashSet<>(FavoritosService.idsDe(sesion.correo()));
        Map<String, Object> json = objetoJsonPara(objeto, sesion, favoritos);

        // Datos de contacto actuales de quien lo publicó (los que mantiene en su perfil).
        Usuario autor = AuthService.obtenerUsuario(objeto.getCorreoUsuario());
        json.put("publicadoPor", autor == null ? "" : autor.getNombre());
        json.put("telefonoContacto", autor == null ? "" : autor.getTelefono());

        Http.json(ex, 200, mapa("objeto", json));
    }

    private static void retirarObjeto(HttpExchange ex, String idTexto) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        int id = idDeRuta(idTexto);

        String resultado = ObjetoPerdidoService.eliminarObjeto(id, sesion.correo());

        if (ObjetoPerdidoService.OBJETO_ELIMINADO.equals(resultado)) {
            FavoritosService.quitarObjeto(id);
            Http.json(ex, 200, mapa("ok", true));
        } else if (ObjetoPerdidoService.OBJETO_NO_ENCONTRADO.equals(resultado)) {
            throw new ApiException(404, resultado);
        } else if (ObjetoPerdidoService.SIN_PERMISO.equals(resultado)) {
            throw new ApiException(403, resultado);
        } else {
            throw new ApiException(500, resultado);
        }
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


    /* ---------------- Favoritos ---------------- */

    private static void listarFavoritos(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);

        List<Integer> ids = FavoritosService.idsDe(sesion.correo());
        Set<Integer> favoritos = new HashSet<>(ids);

        // Solo objetos que siguen publicados, del último guardado al primero.
        List<Object> lista = new ArrayList<>();
        for (int id : ids) {
            ObjetoPerdido objeto = ObjetoPerdidoService.obtenerObjeto(id);
            if (objeto != null) lista.add(objetoJsonPara(objeto, sesion, favoritos));
        }

        Http.json(ex, 200, mapa("favoritos", lista));
    }

    private static void guardarFavorito(HttpExchange ex, String idTexto) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        int id = idDeRuta(idTexto);

        String resultado = FavoritosService.agregar(sesion.correo(), id);

        if (FavoritosService.FAVORITO_GUARDADO.equals(resultado)) {
            Http.json(ex, 200, mapa("ok", true, "favorito", true));
        } else if (FavoritosService.OBJETO_NO_ENCONTRADO.equals(resultado)) {
            throw new ApiException(404, resultado);
        } else {
            throw new ApiException(500, resultado);
        }
    }

    private static void quitarFavorito(HttpExchange ex, String idTexto) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        int id = idDeRuta(idTexto);

        String resultado = FavoritosService.quitar(sesion.correo(), id);

        if (FavoritosService.FAVORITO_QUITADO.equals(resultado)) {
            Http.json(ex, 200, mapa("ok", true, "favorito", false));
        } else {
            throw new ApiException(500, resultado);
        }
    }

    private static int idDeRuta(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "No encontramos este objeto.");
        }
    }

    /* ---------------- Perfil ---------------- */

    private static void verPerfil(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        Http.json(ex, 200, mapa("perfil", perfilJson(sesion.correo())));
    }

    private static void actualizarPerfil(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        Map<String, Object> cuerpo = Http.leerJson(ex);

        // El correo no se puede cambiar: sale de la sesión, nunca de lo que diga el navegador.
        String resultado = AuthService.actualizarPerfil(
                sesion.correo(),
                Http.texto(cuerpo, "nombre"),
                Http.texto(cuerpo, "telefono")
        );

        if (!AuthService.PERFIL_ACTUALIZADO.equals(resultado)) {
            throw new ApiException(400, resultado);
        }

        Map<String, Object> perfil = perfilJson(sesion.correo());
        SessionManager.actualizarNombre(sesion.correo(), (String) perfil.get("nombre"));
        Http.json(ex, 200, mapa("perfil", perfil));
    }

    private static Map<String, Object> perfilJson(String correo) {
        Usuario usuario = AuthService.obtenerUsuario(correo);
        if (usuario == null) throw new ApiException(404, "No encontramos tu cuenta.");
        return mapa("nombre", usuario.getNombre(), "correo", usuario.getCorreo(), "telefono", usuario.getTelefono());
    }

    /* ---------------- Puntos y ruleta ---------------- */
    private static void puntos(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        Http.json(ex, 200, mapa("saldo", RecompensasService.saldo(sesion.correo()), "recompensas", RecompensasService.resumen(sesion.correo())));
    }

    private static int idObjeto(Map<String,Object> cuerpo) {
        Object valor = cuerpo.get("objetoId");
        if (!(valor instanceof Number)) throw new ApiException(400, "Indica un objeto válido.");
        return ((Number) valor).intValue();
    }

    private static void confirmarDevolucion(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        int id = idObjeto(Http.leerJson(ex));
        ObjetoPerdido objeto = ObjetoPerdidoService.obtenerObjeto(id);
        if (objeto == null) throw new ApiException(404, "No encontramos este objeto.");
        try {
            int puntos = RecompensasService.confirmar(sesion.correo(), id, RecompensasService.puntosCategoria(objeto.getNombre()));
            if (puntos < 0) throw new ApiException(409, "Ya recibiste puntos por este objeto.");
            Http.json(ex, 200, mapa("ok", true, "puntosGanados", puntos, "saldo", RecompensasService.saldo(sesion.correo())));
        } catch (IOException e) { throw e; }
    }

    private static void girarRuleta(HttpExchange ex) throws IOException {
        SessionManager.Sesion sesion = requerirSesion(ex);
        int id = idObjeto(Http.leerJson(ex));
        try { Http.json(ex, 200, RecompensasService.girar(sesion.correo(), id)); }
        catch (IllegalStateException e) { throw new ApiException(409, e.getMessage()); }
    }

    /* ---------------- Formato de las respuestas ---------------- */

    private static Map<String, Object> usuarioJson(String nombre, String correo) {
        return mapa("nombre", nombre, "correo", correo);
    }

    // Además del objeto, indica si es favorito de la persona y si lo publicó ella.
    private static Map<String, Object> objetoJsonPara(ObjetoPerdido o, SessionManager.Sesion sesion, Set<Integer> favoritos) {
        Map<String, Object> json = objetoJson(o);
        json.put("favorito", favoritos.contains(o.getId()));
        json.put("propio", o.getCorreoUsuario() != null && o.getCorreoUsuario().equalsIgnoreCase(sesion.correo()));
        return json;
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
