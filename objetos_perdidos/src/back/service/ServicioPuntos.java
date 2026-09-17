package back.service;

import back.model.Estudiante;
import back.model.EstadoObjeto;
import back.model.ObjetoPerdido;

/**
 * Implementa la historia de usuario:
 * "Como estudiante de la Sergio, quiero ganar puntos al ver o registrar
 *  objetos y recibir aún más puntos cuando devuelvo un objeto perdido
 *  de gran valor, para poder reclamar recompensas en el futuro."
 *
 * Trabaja sobre ObjetoPerdido (la clase real del módulo de reportes) y
 * Estudiante (que envuelve el Usuario autenticado por AuthService).
 */
public class ServicioPuntos {

    public static final int PUNTOS_POR_VER_OBJETO = 1;
    public static final int PUNTOS_POR_REGISTRAR_OBJETO = 5;
    public static final int PUNTOS_BASE_POR_DEVOLUCION = 20;

    /**
     * Otorga puntos por ver un objeto. Solo la primera vez que un
     * estudiante ve un objeto en particular (evita puntos infinitos
     * recargando la misma vista).
     */
    public void verObjeto(Estudiante estudiante, ObjetoPerdido objeto) {
        validarParametros(estudiante, objeto);
        boolean esPrimeraVez = objeto.registrarVisualizacion(estudiante.getCorreo());
        if (esPrimeraVez) {
            estudiante.otorgarPuntos(
                    PUNTOS_POR_VER_OBJETO,
                    "Visualización del objeto '" + objeto.getNombre() + "'"
            );
            ServicioEstudiantes.guardarCambios();
        }
    }

    /**
     * Otorga puntos por registrar (publicar) un objeto perdido.
     * Se llama justo después de ObjetoPerdidoService.guardarObjeto(...),
     * ya con el ObjetoPerdido recién creado.
     */
    public void registrarObjeto(Estudiante estudiante, ObjetoPerdido objeto) {
        validarParametros(estudiante, objeto);
        estudiante.otorgarPuntos(
                PUNTOS_POR_REGISTRAR_OBJETO,
                "Registro del objeto '" + objeto.getNombre() + "'"
        );
        ServicioEstudiantes.guardarCambios();
    }

    /**
     * Otorga puntos por devolver un objeto perdido a su dueño.
     * Los puntos totales = puntos base + bonificación según el valor
     * del objeto (a mayor valor, mayor bonificación).
     *
     * @throws IllegalStateException si el objeto ya fue devuelto,
     *         o si no está en un estado válido para ser devuelto.
     */
    public void devolverObjetoPerdido(Estudiante estudiante, ObjetoPerdido objeto) {
        validarParametros(estudiante, objeto);

        if (objeto.getEstado() == EstadoObjeto.DEVUELTO) {
            throw new IllegalStateException("El objeto ya fue devuelto previamente");
        }
        if (objeto.getEstado() != EstadoObjeto.PERDIDO
                && objeto.getEstado() != EstadoObjeto.ENCONTRADO) {
            throw new IllegalStateException(
                    "El objeto debe estar en estado PERDIDO o ENCONTRADO para poder devolverse");
        }

        int puntosTotales = PUNTOS_BASE_POR_DEVOLUCION + objeto.getValor().getBonificacionDevolucion();

        objeto.setEstado(EstadoObjeto.DEVUELTO);
        objeto.setCorreoEstudianteDevuelve(estudiante.getCorreo());

        estudiante.otorgarPuntos(
                puntosTotales,
                "Devolución del objeto '" + objeto.getNombre() + "' (valor " + objeto.getValor() + ")"
        );

        ServicioEstudiantes.guardarCambios();
        ObjetoPerdidoService.guardarCambios(); // persiste el nuevo estado del objeto
    }

    private void validarParametros(Estudiante estudiante, ObjetoPerdido objeto) {
        if (estudiante == null) {
            throw new IllegalArgumentException("El estudiante no puede ser nulo");
        }
        if (objeto == null) {
            throw new IllegalArgumentException("El objeto no puede ser nulo");
        }
    }
}
