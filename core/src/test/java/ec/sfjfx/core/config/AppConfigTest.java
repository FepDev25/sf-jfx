package ec.sfjfx.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    @TempDir
    Path directorio;

    @BeforeEach
    void aislar() {
        // Evita depender de ~/.sfjfx/sfjfx.properties del entorno.
        System.setProperty("sfjfx.config", directorio.resolve("no-existe.properties").toString());
        AppConfig.recargar();
    }

    @AfterEach
    void limpiar() {
        System.clearProperty("sfjfx.config");
        AppConfig.recargar();
    }

    @Test
    void usaValoresPorDefectoCuandoNoHayArchivo() {
        assertEquals("jdbc:postgresql://localhost:5432/sfjfx", AppConfig.obtener("db.url"));
        assertEquals(5432, AppConfig.obtenerInt("db.puerto", 5432));
        assertFalse(AppConfig.obtenerBooleano("db.usarSsl", false));
        assertEquals("prod", AppConfig.entorno());
        assertTrue(AppConfig.esProduccion());
        assertFalse(AppConfig.permiteSemillas());
    }

    @Test
    void entornoDevPermiteSemillas() throws IOException {
        Path archivo = directorio.resolve("dev.properties");
        Files.writeString(archivo, "app.entorno=dev\n");
        System.setProperty("sfjfx.config", archivo.toString());
        AppConfig.recargar();

        assertEquals("dev", AppConfig.entorno());
        assertFalse(AppConfig.esProduccion());
        assertTrue(AppConfig.permiteSemillas());
    }

    @Test
    void entornoDesconocidoSeTrataComoProduccion() throws IOException {
        Path archivo = directorio.resolve("staging.properties");
        Files.writeString(archivo, "app.entorno=staging\n");
        System.setProperty("sfjfx.config", archivo.toString());
        AppConfig.recargar();

        assertTrue(AppConfig.esProduccion());
        assertFalse(AppConfig.permiteSemillas());
    }

    @Test
    void valorPorDefectoSeUsaSiFaltaLaClave() {
        assertEquals("sin-valor", AppConfig.obtener("clave.inexistente", "sin-valor"));
    }

    @Test
    void claveRequeridaAusenteLanzaExcepcion() {
        assertThrows(IllegalStateException.class, () -> AppConfig.obtener("clave.inexistente"));
    }

    @Test
    void archivoExternoSeCargaYSobrescribeDefaults() throws IOException {
        Path archivo = directorio.resolve("sfjfx.properties");
        Files.writeString(archivo,
                "db.url=jdbc:postgresql://otro-host:6000/otra\napp.entorno=test\n");
        System.setProperty("sfjfx.config", archivo.toString());
        AppConfig.recargar();

        assertEquals("jdbc:postgresql://otro-host:6000/otra", AppConfig.obtener("db.url"));
        assertEquals("test", AppConfig.obtener("app.entorno"));
    }
}
