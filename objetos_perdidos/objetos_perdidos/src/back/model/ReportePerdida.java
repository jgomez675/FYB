package back.model;

public class ReportePerdida {

    private int id;
    private String nombreObjeto;
    private String descripcion;
    private String lugar;
    private String fecha;
    private String correoUsuario;

    public ReportePerdida(int id, String nombreObjeto, String descripcion,
                          String lugar, String fecha, String correoUsuario) {
        this.id = id;
        this.nombreObjeto = nombreObjeto;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fecha = fecha;
        this.correoUsuario = correoUsuario;
    }

    public int getId() {
        return id;
    }

    public String getNombreObjeto() {
        return nombreObjeto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public String getFecha() {
        return fecha;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }
}