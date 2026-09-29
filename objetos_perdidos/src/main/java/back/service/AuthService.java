package back.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import back.config.AppConfig;
import back.model.Usuario;
import back.validation.EmailValidator;

/**
 * Registro e inicio de sesión.
 *
 * Como ahora el servidor está expuesto en red, la contraseña ya no se guarda en texto plano:
 * en la tercera columna de data/usuarios.txt queda "pbkdf2$iteraciones$sal$hash".
 * Las cuentas antiguas (texto plano) siguen funcionando y se convierten solas al iniciar sesión.
 */
public class AuthService {

    public static final String REGISTRO_EXITOSO = "REGISTRO_EXITOSO";

    private static final int MAX_NOMBRE = 120;
    private static final int MAX_CONTRASEÑA = 200;

    private static final String PREFIJO_HASH = "pbkdf2$";
    private static final int ITERACIONES = 120_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static final List<Usuario> usuarios = new ArrayList<>();
    private static final Path ARCHIVO_USUARIOS = Paths.get("data", "usuarios.txt");

    static {
        cargarUsuarios();
    }

    private static void cargarUsuarios() {
        if (!Files.exists(ARCHIVO_USUARIOS)) {
            return;
        }

        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO_USUARIOS)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split("\\|", -1);
                if (partes.length == 3) {
                    usuarios.add(new Usuario(partes[0], partes[1], partes[2]));
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cargar los usuarios guardados: " + e.getMessage());
        }
    }

    // Quien llama debe tener el candado de `usuarios`.
    private static void guardarUsuarios() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            lineas.add(usuario.getNombre() + "|" + usuario.getCorreo() + "|" + usuario.getContraseña());
        }
        Archivos.escribirLineas(ARCHIVO_USUARIOS, lineas);
    }

    public static String registrarUsuario(
            String nombre,
            String correo,
            String contraseña,
            String confirmarContraseña) {

        nombre = limpiar(nombre);
        correo = correo == null ? "" : correo.trim().toLowerCase();

        if (nombre.isBlank()) {
            return "El nombre es obligatorio.";
        }

        if (nombre.length() > MAX_NOMBRE) {
            return "El nombre es demasiado largo (máximo " + MAX_NOMBRE + " caracteres).";
        }

        if (!EmailValidator.esCorreoInstitucional(correo)) {
            return "Debes utilizar un correo @" + AppConfig.DOMINIO_CORREO + ".";
        }

        if (contraseña == null || contraseña.length() < 6) {
            return "La contraseña debe tener mínimo 6 caracteres.";
        }

        if (contraseña.length() > MAX_CONTRASEÑA) {
            return "La contraseña es demasiado larga.";
        }

        if (!contraseña.equals(confirmarContraseña)) {
            return "Las contraseñas no coinciden.";
        }

        if (buscarPorCorreo(correo) != null) {
            return "Este correo ya está registrado.";
        }

        // El hash es lento a propósito: se calcula fuera del candado para no frenar a los demás.
        String guardada = generarHash(contraseña);

        synchronized (usuarios) {
            for (Usuario usuario : usuarios) {
                if (usuario.getCorreo().equalsIgnoreCase(correo)) {
                    return "Este correo ya está registrado.";
                }
            }

            Usuario nuevoUsuario = new Usuario(nombre, correo, guardada);
            usuarios.add(nuevoUsuario);

            try {
                guardarUsuarios();
            } catch (IOException e) {
                usuarios.remove(nuevoUsuario);
                System.err.println("No se pudieron guardar los usuarios: " + e.getMessage());
                return "No se pudo guardar la cuenta. Inténtalo de nuevo.";
            }
        }

        return REGISTRO_EXITOSO;
    }

    public static Usuario iniciarSesion(
            String correo,
            String contraseña) {

        if (correo == null || contraseña == null || contraseña.length() > MAX_CONTRASEÑA) {
            return null;
        }

        Usuario usuario = buscarPorCorreo(correo.trim());
        if (usuario == null) {
            return null;
        }

        String guardada = usuario.getContraseña();

        if (guardada.startsWith(PREFIJO_HASH)) {
            return verificarHash(contraseña, guardada) ? usuario : null;
        }

        // Cuenta antigua con la contraseña en texto plano.
        boolean correcta = MessageDigest.isEqual(
                guardada.getBytes(StandardCharsets.UTF_8),
                contraseña.getBytes(StandardCharsets.UTF_8)
        );

        if (!correcta) {
            return null;
        }

        migrarAHash(usuario, contraseña);
        return usuario;
    }

    private static Usuario buscarPorCorreo(String correo) {
        synchronized (usuarios) {
            for (Usuario usuario : usuarios) {
                if (usuario.getCorreo().equalsIgnoreCase(correo)) {
                    return usuario;
                }
            }
        }
        return null;
    }

    private static void migrarAHash(Usuario antiguo, String contraseña) {
        String guardada = generarHash(contraseña);

        synchronized (usuarios) {
            int posicion = usuarios.indexOf(antiguo);
            if (posicion < 0) {
                return;
            }

            usuarios.set(posicion, new Usuario(antiguo.getNombre(), antiguo.getCorreo(), guardada));

            try {
                guardarUsuarios();
            } catch (IOException e) {
                System.err.println("No se pudo actualizar la contraseña guardada: " + e.getMessage());
            }
        }
    }

    // Los datos van en un archivo con "|" como separador y una cuenta por línea.
    private static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static String generarHash(String contraseña) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        byte[] hash = pbkdf2(contraseña, sal, ITERACIONES, 256);

        return PREFIJO_HASH + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verificarHash(String contraseña, String guardada) {
        String[] partes = guardada.split("\\$");
        if (partes.length != 4) {
            return false;
        }

        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] actual = pbkdf2(contraseña, sal, iteraciones, esperado.length * 8);
            return MessageDigest.isEqual(esperado, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String contraseña, byte[] sal, int iteraciones, int bits) {
        try {
            PBEKeySpec especificacion = new PBEKeySpec(contraseña.toCharArray(), sal, iteraciones, bits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
