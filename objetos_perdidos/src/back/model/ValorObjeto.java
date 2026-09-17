package back.model;

/**
 * Nivel de valor estimado de un objeto (a criterio de quien lo registra
 * o de un administrador). Cada nivel trae asociada una bonificación de
 * puntos que se suma cuando el objeto es devuelto a su dueño.
 */
public enum ValorObjeto {
    BAJO(0),
    MEDIO(15),
    ALTO(40);

    private final int bonificacionDevolucion;

    ValorObjeto(int bonificacionDevolucion) {
        this.bonificacionDevolucion = bonificacionDevolucion;
    }

    public int getBonificacionDevolucion() {
        return bonificacionDevolucion;
    }
}
