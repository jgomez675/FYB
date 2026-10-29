package back.model;

public class ObjetoPerdido {
    private int id;
    private String nombre;
    private String descripcion;
    private String lugar;
    private String fecha;
    private String imagen;
    private String correoUsuario;
    private String categoria;

    public ObjetoPerdido(int id, String nombre, String descripcion, String lugar, String fecha, String imagen, String correoUsuario) {
        this(id, nombre, descripcion, lugar, fecha, imagen, correoUsuario, "Otros");
    }

    public ObjetoPerdido(int id, String nombre, String descripcion, String lugar, String fecha, String imagen, String correoUsuario, String categoria) {
        this.id = id; this.nombre = nombre; this.descripcion = descripcion; this.lugar = lugar;
        this.fecha = fecha; this.imagen = imagen; this.correoUsuario = correoUsuario;
        this.categoria = categoria == null || categoria.isBlank() ? "Otros" : categoria;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getLugar() { return lugar; }
    public String getFecha() { return fecha; }
    public String getImagen() { return imagen; }
    public String getCorreoUsuario() { return correoUsuario; }
    public String getCategoria() { return categoria; }

    public void actualizar(String nombre, String descripcion, String lugar, String fecha, String categoria) {
        this.nombre = nombre; this.descripcion = descripcion; this.lugar = lugar; this.fecha = fecha;
        this.categoria = categoria == null || categoria.isBlank() ? "Otros" : categoria;
    }
}
