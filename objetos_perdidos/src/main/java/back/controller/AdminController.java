package back.controller;

import back.api.SessionManager;
import back.config.AppConfig;
import back.model.ObjetoPerdido;
import back.model.ReportePublicacion;
import back.model.Usuario;
import back.service.AuthService;
import back.service.ObjetoPerdidoService;
import back.service.ReportePublicacionService;
import back.service.ReclamacionesService;
import back.persistence.repository.ObjetoRepository;
import back.persistence.repository.UsuarioRepository;
import back.persistence.repository.MovimientoPuntosRepository;
import back.service.PuntosService;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ObjetoRepository objetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoPuntosRepository movimientoPuntosRepository;

    public AdminController(ObjetoRepository objetoRepository,
                           UsuarioRepository usuarioRepository,
                           MovimientoPuntosRepository movimientoPuntosRepository) {
        this.objetoRepository = objetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoPuntosRepository = movimientoPuntosRepository;
    }

    @GetMapping("/resumen")
    public Map<String,Object> resumen(HttpServletRequest request) {
        requerirAdmin(request);
        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        long visibles = objetos.stream().filter(o -> !o.isOculto()).count();
        long encontrados = objetos.stream().filter(o -> !o.isOculto() && "ENCONTRADO".equalsIgnoreCase(o.getEstado())).count();
        long devueltos = objetos.stream().filter(o -> !o.isOculto() && o.estaDevuelto()).count();
        long reportesPendientes = ReportePublicacionService.listarReportes().stream()
                .filter(r -> "PENDIENTE".equalsIgnoreCase(r.getEstado())).count();
        return Map.of("totalObjetos", visibles, "encontrados", encontrados,
                "devueltos", devueltos, "reportesPendientes", reportesPendientes);
    }

    /** Indicadores ampliados para el tablero administrativo. */
    @GetMapping("/analitica")
    public Map<String,Object> analitica(HttpServletRequest request) {
        requerirAdmin(request);
        List<ObjetoPerdido> objetos = ObjetoPerdidoService.obtenerObjetos();
        Map<String,Long> categorias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ObjetoPerdido o : objetos) {
            if (o.isOculto()) continue;
            String categoria = (o.getCategoria() == null || o.getCategoria().isBlank())
                    ? "Otros" : o.getCategoria();
            categorias.merge(categoria, 1L, Long::sum);
        }
        List<ReclamacionesService.Reclamacion> reclamaciones = ReclamacionesService.todas();
        long pendientes = reclamaciones.stream().filter(r -> "PENDIENTE".equals(r.estado())).count();
        long aprobadas = reclamaciones.stream().filter(r -> "APROBADA".equals(r.estado())).count();
        long rechazadas = reclamaciones.stream().filter(r -> "RECHAZADA".equals(r.estado())).count();
        long devueltos = objetos.stream().filter(o -> !o.isOculto() && o.estaDevuelto()).count();
        Map<String,Object> resultado = new LinkedHashMap<>();
        resultado.put("usuariosRegistrados", AuthService.listarUsuarios().size());
        resultado.put("publicaciones", objetos.stream().filter(o -> !o.isOculto()).count());
        resultado.put("objetosRecuperados", devueltos);
        resultado.put("reclamaciones", reclamaciones.size());
        resultado.put("reclamacionesPendientes", pendientes);
        resultado.put("reclamacionesAprobadas", aprobadas);
        resultado.put("reclamacionesRechazadas", rechazadas);
        resultado.put("categorias", categorias);
        return resultado;
    }

    /** Lista todas las publicaciones para moderación, incluidas las ocultas. */
    @GetMapping("/objetos")
    public Map<String,Object> objetos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            HttpServletRequest request) {
        requerirAdmin(request);

        String categoriaFiltro = limpiarFiltro(categoria);
        String estadoFiltro = limpiarFiltro(estado);
        String textoFiltro = limpiarFiltro(q).toLowerCase(Locale.ROOT);

        List<Map<String,Object>> salida = new ArrayList<>();
        for (ObjetoPerdido objeto : ObjetoPerdidoService.obtenerObjetos()) {
            if (!categoriaFiltro.isEmpty() && !categoriaFiltro.equalsIgnoreCase(objeto.getCategoria())) continue;
            if (!estadoFiltro.isEmpty() && !estadoFiltro.equalsIgnoreCase(objeto.getEstado())) continue;

            String contenido = String.join(" ",
                    valor(objeto.getNombre()),
                    valor(objeto.getDescripcion()),
                    valor(objeto.getLugar()),
                    valor(objeto.getCorreoUsuario()),
                    valor(objeto.getCategoria()),
                    valor(objeto.getEstado()));
            if (!textoFiltro.isEmpty() && !contenido.toLowerCase(Locale.ROOT).contains(textoFiltro)) continue;

            Map<String,Object> fila = new LinkedHashMap<>();
            fila.put("id", objeto.getId());
            fila.put("nombre", objeto.getNombre());
            fila.put("descripcion", objeto.getDescripcion());
            fila.put("lugar", objeto.getLugar());
            fila.put("fecha", objeto.getFecha());
            fila.put("imagenUrl", imagenUrl(objeto.getImagen()));
            fila.put("correoUsuario", objeto.getCorreoUsuario());
            fila.put("categoria", objeto.getCategoria());
            fila.put("estado", objeto.getEstado());
            fila.put("oculto", objeto.isOculto());
            salida.add(fila);
        }
        return Map.of("objetos", salida, "total", salida.size());
    }

    /** Eliminación masiva protegida por el rol administrador. */
    @DeleteMapping("/objetos/todos")
    public Map<String,Object> eliminarTodasLasPublicaciones(HttpServletRequest request) {
        requerirAdmin(request);
        int eliminados = ObjetoPerdidoService.eliminarTodosLosObjetosPorAdmin();
        if (eliminados < 0) {
            throw new RuntimeException("No se pudieron eliminar las publicaciones. No se guardaron los cambios.");
        }
        objetoRepository.deleteAllInBatch();
        return Map.of("ok", true, "eliminados", eliminados,
                "mensaje", "Se eliminaron " + eliminados + " publicaciones.");
    }

    /** Eliminación definitiva protegida por el rol administrador. */
    @DeleteMapping("/objetos/{id}")
    public Map<String,Object> eliminarObjeto(@PathVariable int id, HttpServletRequest request) {
        requerirAdmin(request);
        if (!ObjetoPerdidoService.eliminarObjetoPorAdmin(id)) {
            throw new RuntimeException("No se encontró el objeto o no se pudo guardar su eliminación.");
        }
        objetoRepository.deleteById(id);
        return Map.of("ok", true, "mensaje", "La publicación fue eliminada.");
    }

    @GetMapping("/reportes")
    public Map<String,Object> reportes(HttpServletRequest request) {
        requerirAdmin(request);
        List<Map<String,Object>> salida = new ArrayList<>();
        for (ReportePublicacion r : ReportePublicacionService.listarReportes()) {
            ObjetoPerdido objeto = ObjetoPerdidoService.obtenerObjeto(r.getObjetoId());
            Map<String,Object> fila = new LinkedHashMap<>();
            fila.put("id", r.getId()); fila.put("objetoId", r.getObjetoId());
            fila.put("nombreObjeto", objeto == null ? "Publicación retirada" : objeto.getNombre());
            fila.put("correoReportante", r.getCorreoUsuario()); fila.put("motivo", r.getMotivo());
            fila.put("detalle", r.getDetalle()); fila.put("fecha", r.getFecha()); fila.put("estado", r.getEstado());
            salida.add(fila);
        }
        return Map.of("reportes", salida);
    }

    @PostMapping("/reportes/{id}/decision")
    public Map<String,Object> decision(@PathVariable int id, @RequestBody Map<String,String> body, HttpServletRequest request) {
        requerirAdmin(request);
        String decision = body.getOrDefault("decision", "").toUpperCase(Locale.ROOT);
        if (!ReportePublicacionService.resolverReporte(id, decision))
            throw new RuntimeException("No se pudo aplicar la decisión. Verifica el reporte y la publicación.");
        return Map.of("ok", true, "mensaje", "La decisión fue guardada.");
    }

    @GetMapping("/usuarios")
    public Map<String,Object> usuarios(HttpServletRequest request) {
        requerirAdmin(request);
        List<Map<String,Object>> salida = new ArrayList<>();
        for (Usuario u : AuthService.listarUsuarios()) {
            salida.add(Map.of("nombre", u.getNombre(), "correo", u.getCorreo(), "bloqueado", u.isBloqueado()));
        }
        return Map.of("usuarios", salida);
    }

    /** Elimina todas las cuentas estudiantiles, sus puntos y sesiones; conserva al administrador. */
    @DeleteMapping("/usuarios/todos-excepto-admin")
    @Transactional
    public Map<String,Object> eliminarTodasLasCuentasExceptoAdmin(HttpServletRequest request) {
        SessionManager.Sesion admin = requerirAdmin(request);
        String correoAdmin = AppConfig.CORREO_ADMIN;

        // No se permite continuar si falta la cuenta que debe conservar el acceso administrativo.
        if (AuthService.buscarUsuario(correoAdmin) == null) {
            throw new RuntimeException("No se encontró la cuenta administradora. No se eliminó ninguna cuenta.");
        }
        if (!PuntosService.limpiarDatosUsuariosExcepto(correoAdmin)) {
            throw new RuntimeException("No se pudieron limpiar los puntos. No se eliminaron las cuentas.");
        }
        int eliminados = AuthService.eliminarUsuariosExcepto(correoAdmin);
        if (eliminados < 0) {
            throw new RuntimeException("No se pudieron guardar los cambios de usuarios.");
        }

        movimientoPuntosRepository.eliminarTodosExcepto(correoAdmin);
        usuarioRepository.eliminarTodosExcepto(correoAdmin);
        SessionManager.cerrarTodasExcepto(correoAdmin);

        return Map.of("ok", true, "eliminados", eliminados,
                "correoConservado", correoAdmin,
                "mensaje", "Se eliminaron " + eliminados + " cuentas. La cuenta administradora se conservó.");
    }

    @PostMapping("/usuarios/bloqueo")
    public Map<String,Object> bloqueo(@RequestBody Map<String,Object> body, HttpServletRequest request) {
        SessionManager.Sesion admin = requerirAdmin(request);
        String correo = String.valueOf(body.getOrDefault("correo", ""));
        boolean bloquear = Boolean.parseBoolean(String.valueOf(body.getOrDefault("bloqueado", "true")));
        if (correo.isBlank()) throw new RuntimeException("Debes indicar el correo del usuario.");
        if (correo.equalsIgnoreCase(admin.correo())) throw new RuntimeException("No puedes bloquear tu propia cuenta administrativa.");
        if (AuthService.buscarUsuario(correo) == null) throw new RuntimeException("El usuario no existe.");
        if (!AuthService.establecerBloqueo(correo, bloquear)) throw new RuntimeException("No se pudo guardar el cambio.");
        if (bloquear) SessionManager.cerrarTodas(correo);
        return Map.of("ok", true, "mensaje", bloquear ? "Cuenta bloqueada." : "Cuenta desbloqueada.");
    }

    private static String limpiarFiltro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String valor(String valor) {
        return valor == null ? "" : valor;
    }

    private static String imagenUrl(String imagen) {
        if (imagen == null || imagen.isBlank()) return "";
        String archivo = imagen.replace('\\', '/');
        archivo = archivo.substring(archivo.lastIndexOf('/') + 1);
        return archivo.matches("objeto_\\d+\\.(jpg|jpeg|png|gif|webp)")
                ? "/api/imagenes/" + archivo : "";
    }

    private SessionManager.Sesion requerirAdmin(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = null;
        if (cookies != null) for (Cookie c : cookies) if ("fyb_session".equals(c.getName())) token = c.getValue();
        SessionManager.Sesion sesion = SessionManager.obtener(token);
        if (sesion == null) throw new RuntimeException("Tu sesión expiró. Inicia sesión de nuevo.");
        if (!sesion.correo().equalsIgnoreCase(AppConfig.CORREO_ADMIN))
            throw new RuntimeException("No tienes permisos de administrador.");
        return sesion;
    }
}
