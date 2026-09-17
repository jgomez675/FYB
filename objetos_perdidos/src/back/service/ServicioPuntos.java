package back.service;

import back.model.Estudiante;
import back.model.EstadoObjeto;
import back.model.Objeto;

/**
 * Implementa la historia de usuario:
 * "Como estudiante de la Sergio, quiero ganar puntos al ver o registrar
 *  objetos y recibir aún más puntos cuando devuelvo un objeto perdido
 *  de gran valor, para poder reclamar recompensas en el futuro."
 *
 * Trabaja sobre Estudiante (que ya envuelve un Usuario autenticado por
 * AuthService) y Objeto. No decide cómo se persisten los objetos; eso
 * lo maneja el módulo de reportes. Los puntos de los estudiantes sí se
 * guardan en disco a través de ServicioEstudiantes, igual que AuthService
 * guarda los usuarios.
 */
public class ServicioPuntos {

    // Puntos otorgados por acción. Se dejan como constantes públicas
    // para que otros módulos puedan mostrarlos en pantalla si lo desean.
    public static final int PUNTOS_POR_VER_OBJETO = 1;
    public static final int PUNTOS_POR_REGISTRAR_OBJETO = 5;
    public static final int PUNTOS_BASE_POR_DEVOLUCION = 20;

    /**
     * Otorga puntos por ver un objeto. Solo se otorgan la primera vez
     * que un estudiante ve un objeto en particular, para evitar que
     * se generen puntos de forma indefinida recargando la misma vista.
     */
    public void verObjeto(Estudiante estudiante, Objeto objeto) {
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
     * Otorga puntos por registrar un nuevo objeto (perdido o encontrado)
     * en el sistema.
     */
    public void registrarObjeto(Estudiante estudiante, Objeto objeto) {
        validarParametros(estudiante, objeto);
        objeto.setCorreoEstudianteRegistra(estudiante.getCorreo());
        if (objeto.getEstado() == null) {
            objeto.setEstado(EstadoObjeto.REGISTRADO);
        }
        estudiante.otorgarPuntos(
                PUNTOS_POR_REGISTRAR_OBJETO,
                "Registro del objeto '" + objeto.getNombre() + "'"
        );
        ServicioEstudiantes.guardarCambios();
    }

    /**
     * Otorga puntos por devolver un objeto perdido a su dueño.
     * Los puntos totales = puntos base de devolución + bonificación
     * según el valor del objeto (a mayor valor, mayor bonificación).
     *
     * @throws IllegalStateException si el objeto ya fue devuelto,
     *         o si no está en un estado válido para ser devuelto.
     */
    public void devolverObjetoPerdido(Estudiante estudiante, Objeto objeto) {
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
    }

    private void validarParametros(Estudiante estudiante, Objeto objeto) {
        if (estudiante == null) {
            throw new IllegalArgumentException("El estudiante no puede ser nulo");
        }
        if (objeto == null) {
            throw new IllegalArgumentException("El objeto no puede ser nulo");
        }
    }
}
