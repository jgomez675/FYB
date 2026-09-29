package back.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Persistencia sencilla de puntos y giros por usuario/objeto. */
public final class RecompensasService {
  private static final Path FILE=Paths.get("data","recompensas.txt");
  private static final Map<String,Integer> balances=new HashMap<>();
  private static final Map<String,Integer> base=new HashMap<>();
  private static final Map<String,Integer> bonus=new HashMap<>();
  private static final Random random=new Random();
  static { cargar(); }
  private static String key(String email,int id){return email.toLowerCase(Locale.ROOT)+"#"+id;}
  private static synchronized void cargar(){
    try { if(!Files.exists(FILE)) return; for(String l:Files.readAllLines(FILE)){String[] p=l.split("\\|",-1); if(p.length==4){String k=p[0]+"#"+p[1]; base.put(k,Integer.parseInt(p[2])); bonus.put(k,Integer.parseInt(p[3])); balances.merge(p[0],Integer.parseInt(p[2])+Integer.parseInt(p[3]),Integer::sum);}}} catch(Exception e){System.err.println("No se cargaron recompensas: "+e.getMessage());}
  }
  private static synchronized void guardar() throws IOException {Files.createDirectories(FILE.getParent());try(BufferedWriter w=Files.newBufferedWriter(FILE)){for(String k:base.keySet()){String[] p=k.split("#",2);w.write(p[0]+"|"+p[1]+"|"+base.get(k)+"|"+bonus.getOrDefault(k,0));w.newLine();}}}
  public static synchronized int saldo(String email){return balances.getOrDefault(email.toLowerCase(Locale.ROOT),0);}
  /** Resumen persistido de recompensas del usuario para pintar estados en la interfaz. */
  public static synchronized List<Map<String,Object>> resumen(String email){
    String usuario=email.toLowerCase(Locale.ROOT);
    List<Map<String,Object>> lista=new ArrayList<>();
    for(String k:base.keySet()){
      String[] p=k.split("#",2);
      if(p.length==2 && p[0].equals(usuario)){
        Map<String,Object> item=new LinkedHashMap<>();
        item.put("objetoId",Integer.parseInt(p[1]));
        item.put("puntosBase",base.get(k));
        item.put("puntosExtra",bonus.getOrDefault(k,0));
        item.put("ruletaUsada",bonus.getOrDefault(k,0)>0);
        lista.add(item);
      }
    }
    return lista;
  }
  public static int puntosCategoria(String nombre){String n=nombre.toLowerCase(Locale.ROOT);if(n.contains("portátil")||n.contains("portatil")||n.contains("laptop"))return 500;if(n.contains("audífono")||n.contains("audifono")||n.contains("headphone"))return 200;if(n.contains("cargador"))return 50;return 100;}
  public static synchronized int confirmar(String email,int id,int puntos)throws IOException {String k=key(email,id);if(base.containsKey(k))return -1;base.put(k,puntos);bonus.put(k,0);balances.merge(email.toLowerCase(Locale.ROOT),puntos,Integer::sum);guardar();return puntos;}
  public static synchronized Map<String,Object> girar(String email,int id)throws IOException {String k=key(email,id);if(!base.containsKey(k))throw new IllegalStateException("Primero debe confirmarse la devolución.");if(bonus.getOrDefault(k,0)>0)throw new IllegalStateException("La ruleta de este objeto ya fue utilizada.");int[] mult={110,125,150,200};int m=mult[random.nextInt(mult.length)];int b=(base.get(k)*m/100)-base.get(k);bonus.put(k,b);balances.merge(email.toLowerCase(Locale.ROOT),b,Integer::sum);guardar();Map<String,Object> r=new LinkedHashMap<>();r.put("multiplicador",m/100.0);r.put("puntosBase",base.get(k));r.put("puntosExtra",b);r.put("saldo",saldo(email));return r;}
}
