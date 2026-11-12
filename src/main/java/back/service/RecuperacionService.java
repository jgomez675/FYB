package back.service;

import back.api.SessionManager;
import back.config.AppConfig;
import back.model.Usuario;
import back.validation.EmailValidator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuperación de contraseña con un código de un solo uso enviado al correo institucional.
 *
 * Medidas de seguridad:
 *  - Código aleatorio de 6 dígitos (SecureRandom) que vence a los 15 minutos y sirve una sola vez.
 *  - Solo se guarda un hash del código (en memoria); nunca el código en claro.
 *  - Máximo 5 intentos por código; después hay que pedir uno nuevo.
 *  - Pedir un código nuevo invalida el anterior.
 *  - Límite de solicitudes por correo (una por minuto, cinco por hora) contra el spam de correos.
 *  - La respuesta es la misma exista o no la cuenta, para no revelar qué correos están registrados.
 *  - Al cambiar la contraseña se cierran todas las sesiones abiertas de esa cuenta.
 */
@Service
public class RecuperacionService {

    public static final int MINUTOS_DE_VIGENCIA = 15;

    private static final long VIGENCIA_MS = MINUTOS_DE_VIGENCIA * 60_000L;
    private static final long ESPERA_MS = 60_000L;
    private static final long HORA_MS = 3_600_000L;
    private static final int MAX_SOLICITUDES_POR_HORA = 5;
    private static final int MAX_INTENTOS = 5;
    private static final int MAX_CONTROLES = 5_000;

    private static final String MENSAJE_CODIGO =
            "Código incorrecto o vencido. Si lo necesitas, solicita uno nuevo.";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static final class Solicitud {
        final byte[] sal;
        final byte[] hash;
        final long expira;
        int intentos;

        Solicitud(byte[] sal, byte[] hash, long expira) {
            this.sal = sal;
            this.hash = hash;
            this.expira = expira;
        }
    }

    private static final class Control {
        long ultima;
        long inicioHora;
        int enLaHora;
    }

    private final Map<String, Solicitud> solicitudes = new ConcurrentHashMap<>();
    private final Map<String, Control> controles = new ConcurrentHashMap<>();
    private final CorreoService servicioCorreo;

    public RecuperacionService(CorreoService servicioCorreo) {
        this.servicioCorreo = servicioCorreo;
    }

    /** Paso 1: genera y envía el código. No indica si la cuenta existe. */
    public void solicitar(String correoIngresado) {

        String correo = normalizar(correoIngresado);

        if (!EmailValidator.esCorreoInstitucional(correo)) {
            throw new RuntimeException(
                    "Debes utilizar un correo @" + AppConfig.DOMINIO_CORREO + "."
            );
        }

        limitarSolicitudes(correo);

        Usuario usuario = AuthService.buscarUsuario(correo);

        if (usuario == null) {
            return;
        }

        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));

        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);

        solicitudes.put(
                correo,
                new Solicitud(
                        sal,
                        calcularHash(sal, codigo),
                        System.currentTimeMillis() + VIGENCIA_MS
                )
        );

        String nombre = usuario.getNombre();

        // En segundo plano para que la respuesta tarde igual exista o no la cuenta.
        CompletableFuture.runAsync(
                () -> servicioCorreo.enviarCodigo(correo, nombre, codigo, MINUTOS_DE_VIGENCIA)
        );
    }

    /** Paso 2: comprueba el código y cambia la contraseña. */
    public void restablecer(
            String correoIngresado,
            String codigoIngresado,
            String contraseñaNueva,
            String confirmar) {

        String correo = normalizar(correoIngresado);

        // Primero la contraseña: un error de digitación no gasta intentos del código.
        String errorContraseña = AuthService.errorDeContraseña(contraseñaNueva, confirmar);

        if (errorContraseña != null) {
            throw new RuntimeException(errorContraseña);
        }

        Solicitud solicitud = solicitudes.get(correo);

        if (solicitud == null) {
            throw new RuntimeException(MENSAJE_CODIGO);
        }

        synchronized (solicitud) {

            if (System.currentTimeMillis() > solicitud.expira) {
                solicitudes.remove(correo, solicitud);
                throw new RuntimeException(MENSAJE_CODIGO);
            }

            String codigo = codigoIngresado == null
                    ? ""
                    : codigoIngresado.replaceAll("\\s", "");

            boolean correcto = MessageDigest.isEqual(
                    solicitud.hash,
                    calcularHash(solicitud.sal, codigo)
            );

            if (!correcto) {
                solicitud.intentos++;

                if (solicitud.intentos >= MAX_INTENTOS) {
                    solicitudes.remove(correo, solicitud);
                }

                throw new RuntimeException(MENSAJE_CODIGO);
            }

            // Un código sirve una sola vez.
            solicitudes.remove(correo, solicitud);
        }

        if (!AuthService.cambiarContraseña(correo, contraseñaNueva)) {
            throw new RuntimeException(
                    "No se pudo actualizar la contraseña. Solicita un código nuevo e inténtalo de nuevo."
            );
        }

        SessionManager.cerrarTodas(correo);
    }

    private void limitarSolicitudes(String correo) {

        long ahora = System.currentTimeMillis();

        if (controles.size() > MAX_CONTROLES) {
            controles.values().removeIf(c -> ahora - c.ultima > HORA_MS);
        }

        Control control = controles.computeIfAbsent(correo, k -> new Control());

        synchronized (control) {

            if (ahora - control.inicioHora > HORA_MS) {
                control.inicioHora = ahora;
                control.enLaHora = 0;
            }

            if (ahora - control.ultima < ESPERA_MS
                    || control.enLaHora >= MAX_SOLICITUDES_POR_HORA) {

                throw new RuntimeException(
                        "Demasiados intentos. Espera un momento antes de pedir otro código."
                );
            }

            control.ultima = ahora;
            control.enLaHora++;
        }
    }

    private String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    private byte[] calcularHash(byte[] sal, String codigo) {
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            resumen.update(sal);
            return resumen.digest(codigo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
