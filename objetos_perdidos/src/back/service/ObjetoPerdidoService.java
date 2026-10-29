package back.service;

import back.model.ObjetoPerdido;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ObjetoPerdidoService {
    private static final List<ObjetoPerdido> objetos = new ArrayList<>();
    private static final Path ARCHIVO_OBJETOS = Paths.get("data", "objetos.txt");
    private static final Path CARPETA_IMAGENES = Paths.get("data", "objetos");
    static { cargarObjetos(); }

    private static void cargarObjetos() {
        if (!Files.exists(ARCHIVO_OBJETOS)) return;
        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_OBJETOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] p = linea.split("\\|", -1);
                if (p.length == 7 || p.length == 8) {
                    try { objetos.add(new ObjetoPerdido(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4], p[5], p[6], p.length == 8 ? p[7] : "Otros")); }
                    catch (NumberFormatException ignored) { }
                }
            }
        } catch (IOException e) { System.err.println("No se pudieron cargar los objetos: " + e.getMessage()); }
    }

    public static synchronized List<ObjetoPerdido> obtenerObjetos() { return new ArrayList<>(objetos); }
    public static synchronized ObjetoPerdido obtenerObjeto(int id) { for (ObjetoPerdido o : objetos) if (o.getId() == id) return o; return null; }
    public static Path carpetaImagenes() { return CARPETA_IMAGENES; }
    public static final String OBJETO_GUARDADO = "OBJETO_GUARDADO";

    public static synchronized String guardarObjeto(String nombre, String descripcion, String lugar, String fecha, Path imagenOriginal, String correoUsuario, String categoria) {
        String error = validar(nombre, descripcion, lugar, fecha);
        if (error != null) return error;
        if (imagenOriginal == null) return "Debes seleccionar una imagen.";
        try {
            Files.createDirectories(CARPETA_IMAGENES);
            int id = obtenerSiguienteId();
            String extension = obtenerExtension(imagenOriginal.getFileName().toString());
            Path destino = CARPETA_IMAGENES.resolve("objeto_" + id + extension);
            Files.copy(imagenOriginal, destino, StandardCopyOption.REPLACE_EXISTING);
            objetos.add(new ObjetoPerdido(id, limpiar(nombre), limpiar(descripcion), limpiar(lugar), limpiar(fecha), destino.toString(), limpiar(correoUsuario), limpiarCategoria(categoria)));
            guardarObjetos();
            return OBJETO_GUARDADO;
        } catch (IOException e) { return "No se pudo guardar el objeto."; }
    }

    public static synchronized String editarObjeto(int id, String correoUsuario, boolean administrador, String nombre, String descripcion, String lugar, String fecha, String categoria) {
        String error = validar(nombre, descripcion, lugar, fecha);
        if (error != null) return error;
        ObjetoPerdido objeto = obtenerObjeto(id);
        if (objeto == null) return "No encontramos este objeto.";
        if (!administrador && !objeto.getCorreoUsuario().equalsIgnoreCase(correoUsuario)) return "Solo puedes editar tus propias publicaciones.";
        objeto.actualizar(limpiar(nombre), limpiar(descripcion), limpiar(lugar), limpiar(fecha), limpiarCategoria(categoria));
        try { guardarObjetos(); return "OBJETO_ACTUALIZADO"; }
        catch (IOException e) { return "No se pudieron guardar los cambios."; }
    }

    public static synchronized void editarCategoriaInterna(int id, String categoria) {
        ObjetoPerdido objeto = obtenerObjeto(id);
        if (objeto == null) return;
        objeto.actualizar(objeto.getNombre(), objeto.getDescripcion(), objeto.getLugar(), objeto.getFecha(), categoria);
        try { guardarObjetos(); } catch (IOException e) { throw new IllegalStateException("No se pudo actualizar la categoría del objeto."); }
    }

    private static String validar(String nombre, String descripcion, String lugar, String fecha) {
        if (nombre == null || nombre.isBlank()) return "El nombre del objeto es obligatorio.";
        if (descripcion == null || descripcion.isBlank()) return "La descripción es obligatoria.";
        if (lugar == null || lugar.isBlank()) return "El lugar es obligatorio.";
        if (fecha == null || fecha.isBlank()) return "La fecha es obligatoria.";
        return null;
    }
    private static void guardarObjetos() throws IOException {
        Path carpeta = ARCHIVO_OBJETOS.getParent(); if (carpeta != null) Files.createDirectories(carpeta);
        try (BufferedWriter w = Files.newBufferedWriter(ARCHIVO_OBJETOS)) {
            for (ObjetoPerdido o : objetos) {
                w.write(o.getId()+"|"+limpiar(o.getNombre())+"|"+limpiar(o.getDescripcion())+"|"+limpiar(o.getLugar())+"|"+limpiar(o.getFecha())+"|"+limpiar(o.getImagen())+"|"+limpiar(o.getCorreoUsuario())+"|"+limpiarCategoria(o.getCategoria())); w.newLine();
            }
        }
    }
    private static int obtenerSiguienteId() { int mayor=0; for (ObjetoPerdido o:objetos) if(o.getId()>mayor) mayor=o.getId(); return mayor+1; }
    private static String limpiar(String s) { return s == null ? "" : s.trim().replace("|", "/").replace("\n", " ").replace("\r", " "); }
    private static String limpiarCategoria(String s) { return s == null || s.isBlank() ? "Otros" : limpiar(s); }
    private static String obtenerExtension(String n) { int p=n.lastIndexOf('.'); if(p>=0) { String e=n.substring(p).toLowerCase(); if(List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(e)) return e; } return ".jpg"; }
}
