package back.service;

public class PuntosService {

    public int puntosPorCategoria(String categoria) {
        if (categoria == null) {
            return 0;
        }

        String categoriaNormalizada = categoria.trim().toLowerCase();

        switch (categoriaNormalizada) {
            case "laptop":
                return 500;

            case "audífonos":
            case "audifonos":
                return 200;

            case "cargador":
                return 50;

            default:
                return 0;
        }
    }
}
