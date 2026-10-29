package back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CategoriasService {
    private static final Path ARCHIVO = Paths.get("data", "categorias.txt");
    private static final Set<String> categorias = new LinkedHashSet<>();
    static {
        if (Files.exists(ARCHIVO)) {
            try { for (String linea : Files.readAllLines(ARCHIVO)) if (!linea.isBlank()) categorias.add(limpiar(linea)); }
            catch (IOException e) { System.err.println("No se pudieron cargar las categorías."); }
        }
        if (categorias.isEmpty()) {
            categorias.add("Tecnología"); categorias.add("Documentos"); categorias.add("Accesorios");
            categorias.add("Ropa"); categorias.add("Llaves"); categorias.add("Otros");
            guardar();
        }
    }
    private CategoriasService() { }
    public static synchronized List<String> listar() { return new ArrayList<>(categorias); }
    public static synchronized String crear(String nombre) {
        String n = limpiar(nombre);
        if (n.isBlank()) return "Escribe el nombre de la categoría.";
        if (buscar(n) != null) return "Esa categoría ya existe.";
        categorias.add(n); guardar(); return "OK";
    }
    public static synchronized String editar(String anterior, String nuevo) {
        String viejo = buscar(anterior); String n = limpiar(nuevo);
        if (viejo == null) return "No encontramos esa categoría.";
        if (n.isBlank()) return "Escribe el nuevo nombre.";
        if (!viejo.equalsIgnoreCase(n) && buscar(n) != null) return "Ya existe una categoría con ese nombre.";
        List<String> copia = new ArrayList<>(categorias); categorias.clear();
        for (String c : copia) categorias.add(c.equals(viejo) ? n : c);
        actualizarObjetos(viejo, n); guardar(); return "OK";
    }
    public static synchronized String eliminar(String nombre) {
        String encontrada = buscar(nombre);
        if (encontrada == null) return "No encontramos esa categoría.";
        if (categorias.size() == 1) return "Debe existir al menos una categoría.";
        categorias.remove(encontrada);
        String destino = categorias.iterator().next();
        for (back.model.ObjetoPerdido o : ObjetoPerdidoService.obtenerObjetos()) {
            if (o.getCategoria().equals(encontrada)) ObjetoPerdidoService.editarCategoriaInterna(o.getId(), destino);
        }
        guardar(); return "OK";
    }
    public static synchronized boolean existe(String nombre) { return buscar(nombre) != null; }
    private static String buscar(String nombre) { if (nombre == null) return null; for (String c : categorias) if (c.equalsIgnoreCase(nombre.trim())) return c; return null; }
    private static void actualizarObjetos(String anterior, String nuevo) {
        for (back.model.ObjetoPerdido o : ObjetoPerdidoService.obtenerObjetos()) {
            if (o.getCategoria().equals(anterior)) ObjetoPerdidoService.editarCategoriaInterna(o.getId(), nuevo);
        }
    }
    private static String limpiar(String s) { return s == null ? "" : s.trim().replace("|", "/").replace("\n", " ").replace("\r", " "); }
    private static void guardar() { try { Files.createDirectories(ARCHIVO.getParent()); Files.write(ARCHIVO, categorias); } catch (IOException e) { throw new IllegalStateException("No se pudieron guardar las categorías."); } }
}
