package back.service;

import back.config.AppConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío del código de recuperación.
 *
 * Si el servidor de correo está configurado (variables SPRING_MAIL_HOST, SPRING_MAIL_USERNAME...,
 * ver README) el código sale por correo. Si no, solo se imprime en la consola del servidor:
 * sirve para desarrollar y probar, pero NO debe usarse así en producción.
 */
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> proveedor;
    private final String remitente;

    public CorreoService(
            ObjectProvider<JavaMailSender> proveedor,
            @Value("${fyb.mail.from:${spring.mail.username:}}") String remitente) {

        this.proveedor = proveedor;
        this.remitente = remitente == null ? "" : remitente.trim();
    }

    public void enviarCodigo(String destino, String nombre, String codigo, int minutosDeVigencia) {

        JavaMailSender enviador = proveedor.getIfAvailable();

        if (enviador == null) {
            System.out.println(
                    "[RECUPERACIÓN] Correo no configurado. Código para "
                            + destino + ": " + codigo
                            + " (vence en " + minutosDeVigencia + " minutos)"
            );
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();

            if (!remitente.isEmpty()) {
                mensaje.setFrom(remitente);
            }

            mensaje.setTo(destino);
            mensaje.setSubject("Código para recuperar tu contraseña - " + AppConfig.NOMBRE);
            mensaje.setText(
                    "Hola, " + nombre + ".\n\n"
                            + "Tu código para recuperar la contraseña es: " + codigo + "\n\n"
                            + "Vence en " + minutosDeVigencia + " minutos y solo sirve una vez.\n"
                            + "Si no lo pediste tú, ignora este mensaje: tu contraseña sigue igual.\n"
                            + "Nunca compartas este código con nadie."
            );

            enviador.send(mensaje);

        } catch (Exception e) {
            System.err.println("No se pudo enviar el correo de recuperación: " + e.getMessage());
        }
    }
}
