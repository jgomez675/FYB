package back.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "objetos", indexes = {
        @Index(name = "idx_objetos_usuario", columnList = "correo_usuario"),
        @Index(name = "idx_objetos_estado", columnList = "estado")
})
public class ObjetoEntity {

    /** Mismo id que ya usa la app (no se genera aquí). */
    @Id
    private Integer id;

    @Column(length = 500)
    private String nombre;

    @Column(length = 4000)
    private String descripcion;

    @Column(length = 500)
    private String lugar;

    @Column(length = 40)
    private String fecha;

    @Column(length = 500)
    private String imagen;

    /** Quien publicó el objeto. Sin llave foránea a propósito: hay datos antiguos de correos sin cuenta. */
    @Column(name = "correo_usuario", length = 254)
    private String correoUsuario;

    @Column(length = 4000)
    private String caracteristicaPrivada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaEntity categoria;

    @Column(length = 30)
    private String estado;

    @Column(length = 254)
    private String correoEstudianteDevuelve;

    @Column(length = 254)
    private String correoDescubridor;

    protected ObjetoEntity() {
    }

    public ObjetoEntity(Integer id) {
        this.id = id;
    }

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getLugar() { return lugar; }
    public String getFecha() { return fecha; }
    public String getImagen() { return imagen; }
    public String getCorreoUsuario() { return correoUsuario; }
    public String getCaracteristicaPrivada() { return caracteristicaPrivada; }
    public CategoriaEntity getCategoria() { return categoria; }
    public String getEstado() { return estado; }
    public String getCorreoEstudianteDevuelve() { return correoEstudianteDevuelve; }
    public String getCorreoDescubridor() { return correoDescubridor; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public void setCorreoUsuario(String correoUsuario) { this.correoUsuario = correoUsuario; }
    public void setCaracteristicaPrivada(String caracteristicaPrivada) { this.caracteristicaPrivada = caracteristicaPrivada; }
    public void setCategoria(CategoriaEntity categoria) { this.categoria = categoria; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setCorreoEstudianteDevuelve(String correo) { this.correoEstudianteDevuelve = correo; }
    public void setCorreoDescubridor(String correo) { this.correoDescubridor = correo; }
}
