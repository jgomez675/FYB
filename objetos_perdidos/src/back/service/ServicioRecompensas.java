package back.service;

import back.model.Estudiante;
import back.model.Recompensa;

/**
 * Servicio encargado de canjear puntos por recompensas.
 * Se deja separado de ServicioPuntos porque, aunque hoy es sencillo,
 * al integrarse con otros programas del repositorio (por ejemplo un
 * catálogo de recompensas conectado a una base de datos) es probable
 * que crezca de forma independiente.
 */
public class ServicioRecompensas {

    /**
     * Intenta canjear una recompensa con los puntos del estudiante.
     *
     * @return true si el canje fue exitoso, false si no tenía puntos suficientes.
     */
    public boolean canjear(Estudiante estudiante, Recompensa recompensa) {
        if (estudiante == null || recompensa == null) {
            throw new IllegalArgumentException("Estudiante y recompensa no pueden ser nulos");
        }

        if (estudiante.getPuntosTotales() < recompensa.getCostoPuntos()) {
            return false;
        }

        estudiante.descontarPuntos(
                recompensa.getCostoPuntos(),
                "Canje de recompensa: " + recompensa.getNombre()
        );
        ServicioEstudiantes.guardarCambios();
        return true;
    }
}
