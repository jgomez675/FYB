package back.model;

public class ObjetoPerdido {

    private int id;
    private String nombre;
    private String descripcion;
    private String lugar;
    private String fecha;
    private String imagen;
    private String correoUsuario;
    private String caracteristicaPrivada;
    private String categoria;
    private String estado;

    public ObjetoPerdido(int id, String nombre, String descripcion, String lugar,
                         String fecha, String imagen, String correoUsuario,
                         String caracteristicaPrivada, String categoria, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fecha = fecha;
        this.imagen = imagen;
        this.correoUsuario = correoUsuario;
        this.caracteristicaPrivada = caracteristicaPrivada;
        this.categoria = categoria;
        this.estado = estado;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getLugar() { return lugar; }
    public String getFecha() { return fecha; }
    public String getImagen() { return imagen; }
    public String getCorreoUsuario() { return correoUsuario; }
    public String getCaracteristicaPrivada() { return caracteristicaPrivada; }
    public String getCategoria() { return categoria; }
    public String getEstado() { return estado; }
}
