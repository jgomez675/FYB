package back.api;

import java.util.Base64;

public final class ImagenUtil {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    public record Imagen(byte[] bytes, String extension) {
    }

    private ImagenUtil() {
    }

    public static Imagen decodificar(String dataUrl) {

        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "El archivo seleccionado no es una imagen válida."
            );
        }

        int coma = dataUrl.indexOf(',');

        if (!dataUrl.startsWith("data:image/")
                || coma < 0
                || !dataUrl.substring(0, coma).endsWith(";base64")) {

            throw new IllegalArgumentException(
                    "El archivo seleccionado no es una imagen válida."
            );
        }

        // Se comprueba el tamaño antes de decodificar.
        if (dataUrl.length() - coma > MAX_BYTES * 4L / 3 + 16) {
            throw new IllegalArgumentException(
                    "La imagen pesa demasiado (máximo 5 MB)."
            );
        }

        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(coma + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "El archivo seleccionado no es una imagen válida."
            );
        }

        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "La imagen pesa demasiado (máximo 5 MB)."
            );
        }

        // Se determina el tipo real de la imagen según sus bytes.
        String extension = extensionPorContenido(bytes);

        if (extension == null) {
            throw new IllegalArgumentException(
                    "El archivo seleccionado no es una imagen válida."
            );
        }

        return new Imagen(bytes, extension);
    }

    private static String extensionPorContenido(byte[] b) {

        // JPG
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {

            return ".jpg";
        }

        // PNG
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 'P'
                && b[2] == 'N'
                && b[3] == 'G'
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A) {

            return ".png";
        }

        // GIF
        if (b.length >= 6
                && b[0] == 'G'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == '8'
                && (b[4] == '7' || b[4] == '9')
                && b[5] == 'a') {

            return ".gif";
        }

        // WEBP
        if (b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P') {

            return ".webp";
        }

        return null;
    }
}