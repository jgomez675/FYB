package back.model;

import java.time.LocalDateTime;

/**
 * Registra un movimiento individual de puntos (ganancia o descuento) para
 * que quede trazabilidad de por qué un estudiante tiene los puntos que tiene.
 * Es inmutable a propósito, para servir como registro histórico confiable.
 */
public class MovimientoPuntos {

    private final int puntos;
    private final String descripcion;
    private final LocalDateTime fecha;

    public MovimientoPuntos(int puntos, String descripcion) {
        this.puntos = puntos;
        this.descripcion = descripcion;
        this.fecha = LocalDateTime.now();
    }

    public int getPuntos() {
        return puntos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return String.format("[%s] %+d pts - %s", fecha, puntos, descripcion);
    }
}
