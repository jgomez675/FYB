package back.model;

public class ReportePublicacion {

    private final int id;
    private final int objetoId;
    private final String correoUsuario;
    private final String motivo;
    private final String detalle;
    private final String fecha;

    public ReportePublicacion(int id, int objetoId, String correoUsuario,
                              String motivo, String detalle, String fecha) {
        this.id = id;
        this.objetoId = objetoId;
        this.correoUsuario = correoUsuario;
        this.motivo = motivo;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public int getObjetoId() { return objetoId; }
    public String getCorreoUsuario() { return correoUsuario; }
    public String getMotivo() { return motivo; }
    public String getDetalle() { return detalle; }
    public String getFecha() { return fecha; }
}
