package back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Puntos aleatorios directos, sin ruleta. Cada usuario solo cobra una vez por objeto. */
public final class RecompensasService {
    private static final Path ARCHIVO = Paths.get("data", "recompensas.txt");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Integer> saldos = new LinkedHashMap<>();
    private static final List<String> cobrados = new ArrayList<>();
    static {
        if (Files.exists(ARCHIVO)) {
            try { for (String linea : Files.readAllLines(ARCHIVO)) { String[] p=linea.split("\\|",-1); if(p.length==3) { try { saldos.put(p[0], Integer.parseInt(p[1])); cobrados.add(p[0]+"|"+p[2]); } catch(NumberFormatException ignored){} } } }
            catch(IOException e) { System.err.println("No se pudieron cargar los puntos."); }
        }
    }
    private RecompensasService() { }
    public static synchronized int saldo(String correo) { return saldos.getOrDefault(clave(correo), 0); }
    public static synchronized List<Object> resumen(String correo) { List<Object> r=new ArrayList<>(); String c=clave(correo); for(String linea:cobrados) if(linea.startsWith(c+"|")) { Map<String,Object> item=new LinkedHashMap<>(); item.put("objetoId", linea.substring(c.length()+1)); r.add(item); } return r; }
    public static int puntosCategoria(String nombre) { return 0; }
    public static synchronized int confirmar(String correo, int objetoId, int ignorado) throws IOException {
        String c=clave(correo); String marca=c+"|"+objetoId;
        if(cobrados.contains(marca)) return -1;
        int puntos=5+RANDOM.nextInt(21); // 5 a 25 puntos, elegidos automáticamente.
        saldos.put(c, saldos.getOrDefault(c,0)+puntos); cobrados.add(marca); guardar(); return puntos;
    }
    private static String clave(String correo) { return correo == null ? "" : correo.trim().toLowerCase(); }
    private static void guardar() throws IOException {
        Files.createDirectories(ARCHIVO.getParent()); List<String> lineas=new ArrayList<>();
        for(String marca:cobrados) { String[] p=marca.split("\\|",-1); lineas.add(p[0]+"|"+saldos.getOrDefault(p[0],0)+"|"+p[1]); }
        Files.write(ARCHIVO,lineas);
    }
}
