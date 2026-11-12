package back.persistence.sync;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (la copia periódica a la base de datos). */
@Configuration
@EnableScheduling
public class PersistenciaConfig {
}
