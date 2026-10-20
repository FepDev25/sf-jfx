package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El registro debe escribir en archivo y rotar por dias conservando el limite configurado. */
class ServicioLogTest {

    @TempDir
    Path directorio;

    @Test
    void escribeLineasYRotaArchivosAntiguos() throws IOException {
        ServicioLog log = new ServicioLog(directorio, 2, true);
        assertTrue(log.habilitado());

        log.info("primera linea");
        log.error("segunda linea", new IllegalStateException("boom"));

        Path archivo = directorio.resolve("sfjfx-" + java.time.LocalDate.now() + ".log");
        assertTrue(Files.isRegularFile(archivo), "Debe crear el archivo del dia");
        String contenido = Files.readString(archivo);
        assertTrue(contenido.contains("primera linea"));
        assertTrue(contenido.contains("[ERROR] segunda linea"));
        assertTrue(contenido.contains("boom"));

        // Con limite 2 se eliminan los archivos que excedan el maximo.
        for (int i = 1; i <= 4; i++) {
            Files.writeString(directorio.resolve("sfjfx-2020-01-0" + i + ".log"), "viejo");
        }
        log.info("forzar rotacion");
        List<Path> archivos;
        try (var lista = Files.list(directorio)) {
            archivos = lista.filter(p -> p.getFileName().toString().startsWith("sfjfx-")).toList();
        }
        assertTrue(archivos.size() <= 2, "Debe conservar como maximo 2 archivos: " + archivos);
    }

    @Test
    void puedeDeshabilitarse() throws IOException {
        ServicioLog log = new ServicioLog(directorio, 5, false);
        assertFalse(log.habilitado());
        log.info("no debe escribirse");
        try (var lista = Files.list(directorio)) {
            assertEquals(0, lista.count());
        }
    }
}
