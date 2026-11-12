package back.model;

public class ReportePublicacion {
    private final int id, objetoId;
    private final String correoUsuario, motivo, detalle, fecha;
    private String estado;

    public ReportePublicacion(int id, int objetoId, String correoUsuario, String motivo, String detalle, String fecha) {
        this(id, objetoId, correoUsuario, motivo, detalle, fecha, "PENDIENTE");
    }
    public ReportePublicacion(int id, int objetoId, String correoUsuario, String motivo, String detalle, String fecha, String estado) {
        this.id=id; this.objetoId=objetoId; this.correoUsuario=correoUsuario;
        this.motivo=motivo; this.detalle=detalle; this.fecha=fecha;
        this.estado=estado == null || estado.isBlank() ? "PENDIENTE" : estado;
    }
    public int getId(){return id;}
    public int getObjetoId(){return objetoId;}
    public String getCorreoUsuario(){return correoUsuario;}
    public String getMotivo(){return motivo;}
    public String getDetalle(){return detalle;}
    public String getFecha(){return fecha;}
    public String getEstado(){return estado;}
    public void setEstado(String estado){this.estado=estado;}
}
