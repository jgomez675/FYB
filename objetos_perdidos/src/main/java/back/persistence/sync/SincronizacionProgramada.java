package back.persistence.sync;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Dispara la copia a la base de datos: al arrancar, cada cierto tiempo
 * (fyb.db.sync-ms) y al apagar el servidor. Un fallo aquí se imprime pero
 * nunca tumba la app: los .txt siguen funcionando igual.
 */
@Component
public class SincronizacionProgramada {

    private final SincronizacionService servicio;

    public SincronizacionProgramada(SincronizacionService servicio) {
        this.servicio = servicio;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar();
    }

    @Scheduled(
            initialDelayString = "${fyb.db.sync-ms:30000}",
            fixedDelayString = "${fyb.db.sync-ms:30000}")
    public void periodica() {
        ejecutar();
    }

    @PreDestroy
    public void alApagar() {
        ejecutar();
    }

    private synchronized void ejecutar() {
        try {
            servicio.sincronizar();
        } catch (RuntimeException e) {
            System.err.println("No se pudo sincronizar con la base de datos: " + e.getMessage());
        }
    }
}
