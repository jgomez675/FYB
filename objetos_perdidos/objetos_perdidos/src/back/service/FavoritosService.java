package back.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lista de favoritos de cada persona. Se guarda en data/favoritos.txt, una línea por favorito:
 * correo|idDelObjeto (en el orden en que se fueron guardando).
 */
public class FavoritosService {

    public static final String FAVORITO_GUARDADO = "FAVORITO_GUARDADO";
    public static final String FAVORITO_QUITADO = "FAVORITO_QUITADO";
    public static final String OBJETO_NO_ENCONTRADO = "No encontramos este objeto.";

    private static final Path ARCHIVO_FAVORITOS = Paths.get("data", "favoritos.txt");

    // correo (en minúsculas) -> ids de objetos, en el orden en que se guardaron
    private static final Map<String, Set<Integer>> favoritos = new HashMap<>();

    static {
        cargarFavoritos();
    }

    private FavoritosService() {
    }

    private static void cargarFavoritos() {
        if (!Files.exists(ARCHIVO_FAVORITOS)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_FAVORITOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] partes = linea.split("\\|", -1);
                if (partes.length != 2) continue;

                try {
                    favoritos.computeIfAbsent(clave(partes[0]), k -> new LinkedHashSet<>())
                            .add(Integer.parseInt(partes[1].trim()));
                } catch (NumberFormatException ignorada) {
                    // línea dañada: se salta
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los favoritos: " + e.getMessage());
        }
    }

    /** Ids de los objetos favoritos de la persona, del más reciente al más antiguo. */
    public static List<Integer> idsDe(String correo) {
        synchronized (favoritos) {
            Set<Integer> ids = favoritos.get(clave(correo));
            if (ids == null) return new ArrayList<>();

            List<Integer> lista = new ArrayList<>(ids);
            Collections.reverse(lista);
            return lista;
        }
    }

    /** Guardar dos veces el mismo favorito no hace nada: el resultado es el mismo. */
    public static String agregar(String correo, int objetoId) {
        if (ObjetoPerdidoService.obtenerObjeto(objetoId) == null) {
            return OBJETO_NO_ENCONTRADO;
        }

        synchronized (favoritos) {
            Set<Integer> ids = favoritos.computeIfAbsent(clave(correo), k -> new LinkedHashSet<>());
            if (!ids.add(objetoId)) {
                return FAVORITO_GUARDADO;
            }

            try {
                guardarFavoritos();
            } catch (IOException e) {
                ids.remove(objetoId);
                System.err.println("No se pudo guardar el favorito: " + e.getMessage());
                return "No se pudo guardar el favorito. Inténtalo de nuevo.";
            }
        }
        return FAVORITO_GUARDADO;
    }

    /** Quitar un objeto que no estaba en la lista tampoco da error. */
    public static String quitar(String correo, int objetoId) {
        synchronized (favoritos) {
            Set<Integer> ids = favoritos.get(clave(correo));
            if (ids == null || !ids.remove(objetoId)) {
                return FAVORITO_QUITADO;
            }

            try {
                guardarFavoritos();
            } catch (IOException e) {
                ids.add(objetoId);
                System.err.println("No se pudo quitar el favorito: " + e.getMessage());
                return "No se pudo quitar el favorito. Inténtalo de nuevo.";
            }
        }
        return FAVORITO_QUITADO;
    }

    /** Se llama cuando se retira una publicación: desaparece de la lista de todas las personas. */
    public static void quitarObjeto(int objetoId) {
        synchronized (favoritos) {
            boolean cambio = false;
            for (Set<Integer> ids : favoritos.values()) {
                if (ids.remove(objetoId)) cambio = true;
            }

            if (!cambio) return;

            try {
                guardarFavoritos();
            } catch (IOException e) {
                // Si falla, la lista de favoritos solo muestra objetos que siguen publicados, así que no se ve.
                System.err.println("No se pudieron limpiar los favoritos del objeto " + objetoId + ": " + e.getMessage());
            }
        }
    }

    // Quien llama debe tener el candado de `favoritos`.
    private static void guardarFavoritos() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entrada : favoritos.entrySet()) {
            for (int id : entrada.getValue()) {
                lineas.add(entrada.getKey() + "|" + id);
            }
        }
        Archivos.escribirLineas(ARCHIVO_FAVORITOS, lineas);
    }

    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
