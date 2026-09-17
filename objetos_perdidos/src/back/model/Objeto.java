package back.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa un objeto registrado en el sistema (perdido, encontrado
 * o ya devuelto).
 */
public class Objeto {

    private final String id;
    private final String nombre;
    private final String descripcion;
    private final CategoriaObjeto categoria;
    private final ValorObjeto valor;
    private final LocalDateTime fechaRegistro;

    private EstadoObjeto estado;
    private String correoEstudianteRegistra;
    private String correoEstudianteDevuelve;

    // Guarda los correos de los estudiantes que ya vieron el objeto,
    // para que ver el mismo objeto varias veces no otorgue puntos infinitos.
    private final Set<String> vistoPorCorreos = new HashSet<>();

    // Rutas/URLs de las fotos del objeto. Punto de integración para el
    // módulo de fotos: puede llenar esta lista sin tocar esta clase.
    private final List<String> fotos = new ArrayList<>();

    public Objeto(String id, String nombre, String descripcion,
                  CategoriaObjeto categoria, ValorObjeto valor) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.valor = valor;
        this.estado = EstadoObjeto.PERDIDO;
        this.fechaRegistro = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public CategoriaObjeto getCategoria() {
        return categoria;
    }

    public ValorObjeto getValor() {
        return valor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public EstadoObjeto getEstado() {
        return estado;
    }

    public void setEstado(EstadoObjeto estado) {
        this.estado = estado;
    }

    public String getCorreoEstudianteRegistra() {
        return correoEstudianteRegistra;
    }

    public void setCorreoEstudianteRegistra(String correoEstudianteRegistra) {
        this.correoEstudianteRegistra = correoEstudianteRegistra;
    }

    public String getCorreoEstudianteDevuelve() {
        return correoEstudianteDevuelve;
    }

    public void setCorreoEstudianteDevuelve(String correoEstudianteDevuelve) {
        this.correoEstudianteDevuelve = correoEstudianteDevuelve;
    }

    /**
     * Devuelve las fotos asociadas a este objeto (rutas o URLs).
     * Para agregar fotos usar {@link #agregarFoto(String)}.
     */
    public List<String> getFotos() {
        return Collections.unmodifiableList(fotos);
    }

    /**
     * Agrega una foto (ruta local o URL) al objeto.
     */
    public void agregarFoto(String rutaOUrl) {
        if (rutaOUrl == null || rutaOUrl.isBlank()) {
            throw new IllegalArgumentException("La ruta de la foto no puede estar vacía");
        }
        fotos.add(rutaOUrl);
    }

    /**
     * Marca que un estudiante (identificado por su correo) vio este objeto.
     * @return true si es la primera vez que ese correo lo ve (y por lo
     *         tanto corresponde otorgar puntos), false si ya lo había visto.
     */
    public boolean registrarVisualizacion(String correoEstudiante) {
        return vistoPorCorreos.add(correoEstudiante.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("Objeto[%s] %s (%s, valor %s) - estado: %s",
                id, nombre, categoria, valor, estado);
    }
}
