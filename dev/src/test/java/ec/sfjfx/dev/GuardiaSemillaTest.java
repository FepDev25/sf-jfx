package ec.sfjfx.dev;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ec.sfjfx.core.config.AppConfig;

/** Las semillas deben rechazarse en produccion y permitirse solo en desarrollo/local. */
class GuardiaSemillaTest {

    @TempDir
    Path directorio;

    @AfterEach
    void limpiar() {
        System.clearProperty("sfjfx.config");
        AppConfig.recargar();
    }

    @Test
    void rechazaSemillasEnProduccion() throws IOException {
        configurar("app.entorno=prod\n");

        assertThrows(IllegalStateException.class, SemillaDesarrollo::ejecutar);
        assertThrows(IllegalStateException.class, SemillaDemo::ejecutar);
    }

    @Test
    void permiteSemillasEnDesarrollo() throws IOException {
        configurar("app.entorno=dev\n");

        assertDoesNotThrow(GuardiaSemilla::verificarEntorno);
    }

    private void configurar(String contenido) throws IOException {
        Path archivo = directorio.resolve("sfjfx.properties");
        Files.writeString(archivo, contenido);
        System.setProperty("sfjfx.config", archivo.toString());
        AppConfig.recargar();
    }
}
