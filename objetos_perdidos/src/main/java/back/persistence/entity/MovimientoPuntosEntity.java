package back.persistence.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Un movimiento del historial de puntos (el saldo es la suma de los movimientos de un correo). */
@Entity
@Table(name = "movimientos_puntos", indexes = {
        @Index(name = "idx_movimientos_correo", columnList = "correo")
})
public class MovimientoPuntosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 500)
    private String motivo;

    private LocalDate fecha;

    protected MovimientoPuntosEntity() {
    }

    public MovimientoPuntosEntity(String correo, int cantidad, String motivo, LocalDate fecha) {
        this.correo = correo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public LocalDate getFecha() { return fecha; }
}
