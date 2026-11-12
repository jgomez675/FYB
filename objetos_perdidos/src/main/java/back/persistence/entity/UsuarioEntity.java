package back.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    /** El correo institucional identifica al usuario (ya es único en la app). */
    @Id
    @Column(length = 254)
    private String correo;

    @Column(nullable = false, length = 200)
    private String nombre;

    /** Siempre el valor guardado por AuthService ("pbkdf2$..."), nunca texto que el usuario escriba. */
    @Column(nullable = false, length = 400)
    private String contrasena;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(String correo, String nombre, String contrasena) {
        this.correo = correo;
        this.nombre = nombre;
        this.contrasena = contrasena;
    }

    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public String getContrasena() { return contrasena; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
