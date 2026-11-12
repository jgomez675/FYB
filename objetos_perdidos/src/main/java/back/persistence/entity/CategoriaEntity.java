package back.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class CategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** Puntos que recibe quien devuelve un objeto de esta categoría. */
    @Column(nullable = false)
    private int puntosDevolucion;

    protected CategoriaEntity() {
    }

    public CategoriaEntity(String nombre, int puntosDevolucion) {
        this.nombre = nombre;
        this.puntosDevolucion = puntosDevolucion;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPuntosDevolucion() { return puntosDevolucion; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPuntosDevolucion(int puntosDevolucion) { this.puntosDevolucion = puntosDevolucion; }
}
