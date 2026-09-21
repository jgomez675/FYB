package back.api;

/** Error que se responde tal cual al navegador: código HTTP + mensaje para el usuario. */
final class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    final int estado;

    ApiException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }
}
