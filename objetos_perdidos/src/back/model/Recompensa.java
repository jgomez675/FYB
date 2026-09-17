package back.model;

/**
 * Representa una recompensa canjeable con puntos.
 */
public class Recompensa {

    private final String id;
    private final String nombre;
    private final int costoPuntos;

    public Recompensa(String id, String nombre, int costoPuntos) {
        if (costoPuntos <= 0) {
            throw new IllegalArgumentException("El costo en puntos debe ser positivo");
        }
        this.id = id;
        this.nombre = nombre;
        this.costoPuntos = costoPuntos;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCostoPuntos() {
        return costoPuntos;
    }

    @Override
    public String toString() {
        return String.format("%s (%d pts)", nombre, costoPuntos);
    }
}
