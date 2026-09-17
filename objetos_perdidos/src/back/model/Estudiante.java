package back.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa a un estudiante de la Sergio dentro del sistema de puntos.
 *
 * A propósito NO duplica nombre/correo: envuelve un {@link Usuario} real
 * (el mismo que devuelve AuthService.iniciarSesion), y le agrega encima
 * lo que necesita el sistema de puntos (puntos totales, historial).
 * El correo del Usuario se usa como identificador único, igual que ya
 * lo hace AuthService para comparar/buscar usuarios.
 */
public class Estudiante {

    private final Usuario usuario;
    private int puntosTotales;
    private final List<MovimientoPuntos> historialPuntos = new ArrayList<>();

    public Estudiante(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        this.usuario = usuario;
        this.puntosTotales = 0;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return usuario.getNombre();
    }

    public String getCorreo() {
        return usuario.getCorreo();
    }

    public int getPuntosTotales() {
        return puntosTotales;
    }

    public List<MovimientoPuntos> getHistorialPuntos() {
        return Collections.unmodifiableList(historialPuntos);
    }

    /**
     * Suma puntos y deja constancia del movimiento en el historial.
     * Este método solo debería ser invocado por la capa de servicio
     * (ServicioPuntos), no directamente desde la UI u otros módulos.
     */
    public void otorgarPuntos(int puntos, String descripcion) {
        if (puntos <= 0) {
            throw new IllegalArgumentException("Los puntos otorgados deben ser positivos");
        }
        this.puntosTotales += puntos;
        this.historialPuntos.add(new MovimientoPuntos(puntos, descripcion));
    }

    /**
     * Descuenta puntos, por ejemplo al canjear una recompensa.
     */
    public void descontarPuntos(int puntos, String descripcion) {
        if (puntos <= 0) {
            throw new IllegalArgumentException("Los puntos a descontar deben ser positivos");
        }
        if (puntos > this.puntosTotales) {
            throw new IllegalStateException("El estudiante no tiene puntos suficientes");
        }
        this.puntosTotales -= puntos;
        this.historialPuntos.add(new MovimientoPuntos(-puntos, descripcion));
    }

    /**
     * Restaura el total de puntos sin generar un movimiento en el
     * historial. Solo debe usarse al cargar los puntos previamente
     * guardados en disco (ver ServicioEstudiantes), no como parte del
     * flujo normal de otorgar/descontar puntos.
     */
    public void cargarPuntosGuardados(int puntosGuardados) {
        this.puntosTotales = puntosGuardados;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %d puntos", getNombre(), getCorreo(), puntosTotales);
    }
}
