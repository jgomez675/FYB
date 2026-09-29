package back.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Escritura de los archivos de datos sin dejarlos a medias si el servidor se cae. */
final class Archivos {

    private Archivos() {
    }

    static void escribirLineas(Path destino, Iterable<String> lineas) throws IOException {
        Path carpeta = destino.toAbsolutePath().getParent();
        Files.createDirectories(carpeta);

        Path temporal = Files.createTempFile(carpeta, "escritura_", ".tmp");
        try {
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal)) {
                for (String linea : lineas) {
                    escritor.write(linea);
                    escritor.newLine();
                }
            }
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
