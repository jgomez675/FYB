package back.service;

import back.model.Estudiante;
import back.model.Usuario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conecta el módulo de login (Usuario / AuthService) con el sistema de
 * puntos: por cada Usuario que inicia sesión, mantiene un Estudiante
 * asociado con sus puntos totales e historial.
 *
 * Sigue el mismo patrón que AuthService: estado estático en memoria +
 * persistencia simple en un archivo de texto con "|" como separador.
 */
public class ServicioEstudiantes {

    private static final Map<String, Estudiante> estudiantesPorCorreo = new LinkedHashMap<>();
    private static final Map<String, Integer> puntosGuardadosPorCorreo = new HashMap<>();
    private static final Path ARCHIVO_PUNTOS = Paths.get("data", "puntos.txt");

    static {
        cargarPuntosGuardados();
    }

    private static void cargarPuntosGuardados() {
        if (!Files.exists(ARCHIVO_PUNTOS)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_PUNTOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split("\\|", -1);
                if (partes.length == 2) {
                    try {
                        puntosGuardadosPorCorreo.put(
                                partes[0].toLowerCase(),
                                Integer.parseInt(partes[1])
                        );
                    } catch (NumberFormatException e) {
                        System.err.println("Línea inválida en " + ARCHIVO_PUNTOS + ": " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los puntos guardados: " + e.getMessage());
        }
    }

    private static void guardarPuntos() {
        try {
            Path carpeta = ARCHIVO_PUNTOS.getParent();
            if (carpeta != null && !Files.exists(carpeta)) {
                Files.createDirectories(carpeta);
            }

            try (BufferedWriter escritor = Files.newBufferedWriter(ARCHIVO_PUNTOS)) {
                for (Estudiante estudiante : estudiantesPorCorreo.values()) {
                    escritor.write(estudiante.getCorreo() + "|" + estudiante.getPuntosTotales());
                    escritor.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron guardar los puntos: " + e.getMessage());
        }
    }

    /**
     * Obtiene el Estudiante asociado a un Usuario ya autenticado
     * (por ejemplo, el que devuelve AuthService.iniciarSesion). Si es
     * la primera vez que este usuario aparece, crea su Estudiante y
     * le restaura los puntos que tuviera guardados en disco.
     */
    public static synchronized Estudiante obtenerOCrearEstudiante(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        String clave = usuario.getCorreo().toLowerCase();
        Estudiante existente = estudiantesPorCorreo.get(clave);
        if (existente != null) {
            return existente;
        }

        Estudiante nuevo = new Estudiante(usuario);
        Integer puntosGuardados = puntosGuardadosPorCorreo.get(clave);
        if (puntosGuardados != null) {
            nuevo.cargarPuntosGuardados(puntosGuardados);
        }

        estudiantesPorCorreo.put(clave, nuevo);
        return nuevo;
    }

    /**
     * Persiste el estado actual de puntos de todos los estudiantes.
     * ServicioPuntos y ServicioRecompensas llaman este método después
     * de cada cambio, igual que AuthService guarda tras cada registro.
     */
    public static synchronized void guardarCambios() {
        guardarPuntos();
    }
}
