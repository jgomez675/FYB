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

    static {
        cargarObjetos();
    }

    private static void cargarObjetos() {
        if (!Files.exists(ARCHIVO_OBJETOS)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_OBJETOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] partes = linea.split("\\|", -1);
                if (partes.length == 7) {
                    try {
                        objetos.add(new ObjetoPerdido(
                                Integer.parseInt(partes[0]), partes[1], partes[2], partes[3],
                                partes[4], partes[5], partes[6]
                        ));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los objetos: " + e.getMessage());
        }
    }

    public static List<ObjetoPerdido> obtenerObjetos() {
        return new ArrayList<>(objetos);
    }

    public static ObjetoPerdido obtenerObjeto(int id) {
        for (ObjetoPerdido objeto : objetos) {
            if (objeto.getId() == id) {
                return objeto;
            }
        }
        return null;
    }

    public static Path carpetaImagenes() {
        return CARPETA_IMAGENES;
    }

    public static final String OBJETO_GUARDADO = "OBJETO_GUARDADO";

    public static String guardarObjeto(String nombre, String descripcion, String lugar,
                                       String fecha, Path imagenOriginal, String correoUsuario) {
        if (nombre == null || nombre.isBlank()) return "El nombre del objeto es obligatorio.";
        if (descripcion == null || descripcion.isBlank()) return "La descripción es obligatoria.";
        if (lugar == null || lugar.isBlank()) return "El lugar es obligatorio.";
        if (fecha == null || fecha.isBlank()) return "La fecha es obligatoria.";
        if (imagenOriginal == null) return "Debes seleccionar una imagen.";

        try {
            Files.createDirectories(CARPETA_IMAGENES);

            int id = obtenerSiguienteId();
            String extension = obtenerExtension(imagenOriginal.getFileName().toString());
            Path destino = CARPETA_IMAGENES.resolve("objeto_" + id + extension);
            Files.copy(imagenOriginal, destino, StandardCopyOption.REPLACE_EXISTING);

            ObjetoPerdido objeto = new ObjetoPerdido(
                    id,
                    limpiar(nombre),
                    limpiar(descripcion),
                    limpiar(lugar),
                    limpiar(fecha),
                    destino.toString(),
                    limpiar(correoUsuario)
            );

            objetos.add(objeto);
            guardarObjetos();
            return "OBJETO_GUARDADO";

        } catch (IOException e) {
            return "No se pudo guardar el objeto: " + e.getMessage();
        }
    }

    private static void guardarObjetos() throws IOException {
        Path carpeta = ARCHIVO_OBJETOS.getParent();
        if (carpeta != null) Files.createDirectories(carpeta);

        try (BufferedWriter escritor = Files.newBufferedWriter(ARCHIVO_OBJETOS)) {
            for (ObjetoPerdido objeto : objetos) {
                escritor.write(
                        objeto.getId() + "|" + objeto.getNombre() + "|" +
                        objeto.getDescripcion() + "|" + objeto.getLugar() + "|" +
                        objeto.getFecha() + "|" + objeto.getImagen() + "|" +
                        objeto.getCorreoUsuario()
                );
                escritor.newLine();
            }
        }
    }

    private static int obtenerSiguienteId() {
        int mayor = 0;
        for (ObjetoPerdido objeto : objetos) {
            if (objeto.getId() > mayor) mayor = objeto.getId();
        }
        return mayor + 1;
    }

    private static String limpiar(String texto) {
        return texto.trim().replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static String obtenerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto >= 0) return nombre.substring(punto).toLowerCase();
        return ".jpg";
    }
}
