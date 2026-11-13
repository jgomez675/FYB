package back.service;

import back.model.ObjetoPerdido;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Persistencia sencilla y compatible con el almacenamiento actual basado en archivos. */
public final class ReclamacionesService {
    private static final Path ARCHIVO = Paths.get("data", "reclamaciones.txt");
    private static final List<Reclamacion> reclamaciones = new ArrayList<>();
    static { cargar(); }
    private ReclamacionesService() {}

    public record Reclamacion(String id, int objetoId, String objetoNombre, String propietario,
                              String solicitante, String explicacion, String estado,
                              String creada, String respuesta) {}

    private static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s) { try { return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8); } catch (Exception e) { return ""; } }
    private static synchronized void cargar() {
        if (!Files.exists(ARCHIVO)) return;
        try (BufferedReader r=Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
            String l; while ((l=r.readLine())!=null) {
                String[] p=l.split("\\|",-1); if(p.length!=9) continue;
                try { reclamaciones.add(new Reclamacion(dec(p[0]),Integer.parseInt(p[1]),dec(p[2]),dec(p[3]),dec(p[4]),dec(p[5]),dec(p[6]),dec(p[7]),dec(p[8]))); } catch(Exception ignored) {}
            }
        } catch(IOException e) { System.err.println("No se pudieron cargar reclamaciones: "+e.getMessage()); }
    }
    private static synchronized void guardar() throws IOException {
        Files.createDirectories(ARCHIVO.getParent()); Path tmp=ARCHIVO.resolveSibling("reclamaciones.txt.tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            for(Reclamacion r:reclamaciones) w.write(String.join("|",enc(r.id()),String.valueOf(r.objetoId()),enc(r.objetoNombre()),enc(r.propietario()),enc(r.solicitante()),enc(r.explicacion()),enc(r.estado()),enc(r.creada()),enc(r.respuesta()))+"\n");
        }
        try { Files.move(tmp,ARCHIVO,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException e) { Files.move(tmp,ARCHIVO,StandardCopyOption.REPLACE_EXISTING); }
    }
    public static synchronized Reclamacion crear(int objetoId,String solicitante,String explicacion) throws IOException {
        ObjetoPerdido o=ObjetoPerdidoService.obtenerObjeto(objetoId);
        if(o==null) throw new IllegalArgumentException("No encontramos este objeto.");
        if(o.getCorreoUsuario().equalsIgnoreCase(solicitante)) throw new IllegalArgumentException("No puedes reclamar una publicación propia.");
        if(o.estaDevuelto()) throw new IllegalArgumentException("Este objeto ya aparece como devuelto.");
        if(o.getCorreoDescubridor()!=null && !o.getCorreoDescubridor().isBlank()) throw new IllegalArgumentException("Este objeto ya tiene una reclamación aprobada.");
        boolean pendiente=reclamaciones.stream().anyMatch(r->r.objetoId()==objetoId && r.solicitante().equalsIgnoreCase(solicitante) && r.estado().equals("PENDIENTE"));
        if(pendiente) throw new IllegalArgumentException("Ya tienes una solicitud pendiente para este objeto.");
        Reclamacion r=new Reclamacion(UUID.randomUUID().toString(),objetoId,o.getNombre(),o.getCorreoUsuario(),solicitante,explicacion.trim(),"PENDIENTE",Instant.now().toString(),"");
        reclamaciones.add(r); guardar(); return r;
    }
    public static synchronized List<Reclamacion> todas() { return List.copyOf(reclamaciones); }
    public static synchronized List<Reclamacion> recibidas(String correo) { return reclamaciones.stream().filter(r->r.propietario().equalsIgnoreCase(correo)).sorted(Comparator.comparing(Reclamacion::creada).reversed()).toList(); }
    public static synchronized List<Reclamacion> enviadas(String correo) { return reclamaciones.stream().filter(r->r.solicitante().equalsIgnoreCase(correo)).sorted(Comparator.comparing(Reclamacion::creada).reversed()).toList(); }
    public static synchronized Reclamacion decidir(String id,String propietario,String decision,String respuesta) throws IOException {
        int idx=-1; Reclamacion old=null;
        for(int i=0;i<reclamaciones.size();i++) if(reclamaciones.get(i).id().equals(id)){idx=i;old=reclamaciones.get(i);break;}
        if(old==null) throw new IllegalArgumentException("No encontramos esta solicitud.");
        if(!old.propietario().equalsIgnoreCase(propietario)) throw new SecurityException("Solo el propietario puede revisar esta solicitud.");
        if(!old.estado().equals("PENDIENTE")) throw new IllegalArgumentException("Esta solicitud ya fue revisada.");
        if(!decision.equals("APROBADA") && !decision.equals("RECHAZADA")) throw new IllegalArgumentException("Decisión no válida.");
        if(decision.equals("APROBADA") && !ObjetoPerdidoService.registrarDescubridor(old.objetoId(),old.solicitante())) throw new IllegalArgumentException("No se pudo aprobar: el objeto puede tener otra reclamación aprobada.");
        Reclamacion updated=new Reclamacion(old.id(),old.objetoId(),old.objetoNombre(),old.propietario(),old.solicitante(),old.explicacion(),decision,old.creada(),respuesta==null?"":respuesta.trim());
        reclamaciones.set(idx,updated);
        try { guardar(); } catch(IOException e) { reclamaciones.set(idx,old); throw e; }
        return updated;
    }
}
