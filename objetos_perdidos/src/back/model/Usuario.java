package back.model;

public class Usuario {

    private String nombre;
    private String correo;
    private String contraseña;
    private String telefono;

    public Usuario(String nombre, String correo, String contraseña) {
        this(nombre, correo, contraseña, "");
    }

    public Usuario(String nombre, String correo, String contraseña, String telefono) {
        this.nombre = nombre;
        this.correo = correo;
        this.contraseña = contraseña;
        this.telefono = telefono == null ? "" : telefono;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContraseña() {
        return contraseña;
    }

    /** Teléfono de contacto (opcional): cadena vacía si la persona no lo ha dado. */
    public String getTelefono() {
        return telefono;
    }
}
