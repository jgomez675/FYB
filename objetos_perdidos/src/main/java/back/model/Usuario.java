package back.model;

public class Usuario {
    private String nombre;
    private String correo;
    private String contraseña;
    private boolean bloqueado;

    public Usuario(String nombre, String correo, String contraseña) {
        this(nombre, correo, contraseña, false);
    }

    public Usuario(String nombre, String correo, String contraseña, boolean bloqueado) {
        this.nombre = nombre;
        this.correo = correo;
        this.contraseña = contraseña;
        this.bloqueado = bloqueado;
    }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContraseña() { return contraseña; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
}
