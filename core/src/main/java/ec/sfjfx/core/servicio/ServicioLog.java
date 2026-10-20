package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.config.AppConfig;

/**
 * Registro basico de la aplicacion con rotacion por dia.
 *
 * <p>Escribe en {@code ~/.sfjfx/logs/sfjfx-YYYY-MM-DD.log} y conserva los ultimos
 * {@code app.log.dias} archivos. La rotacion es diaria: un archivo por dia, con el tamano
 * acumulado acotado por el numero de dias conservados.
 */
public final class ServicioLog {

    public static final String CLAVE_DIRECTORIO = "app.log.directorio";
    public static final String CLAVE_DIAS = "app.log.dias";
    public static final String CLAVE_HABILITADO = "app.log.habilitado";

    private static final int DIAS_POR_DEFECTO = 30;
    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path directorio;
    private final int dias;
    private final boolean habilitado;
    private final List<String> pendientes = new ArrayList<>();

    public ServicioLog() {
        this(resolverDirectorio(AppConfig.obtener(CLAVE_DIRECTORIO, null)),
                Math.max(1, AppConfig.obtenerInt(CLAVE_DIAS, DIAS_POR_DEFECTO)),
                AppConfig.obtenerBooleano(CLAVE_HABILITADO, true));
    }

    private static Path resolverDirectorio(String configurado) {
        if (configurado == null || configurado.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".sfjfx", "logs");
        }
        String ruta = configurado.trim();
        if (ruta.startsWith("~")) {
            ruta = System.getProperty("user.home") + ruta.substring(1);
        }
        Path path = Path.of(ruta);
        return path.isAbsolute() ? path : Path.of(System.getProperty("user.home")).resolve(path);
    }

    /** Constructor con valores explicitos, usado en pruebas. */
    public ServicioLog(Path directorio, int dias, boolean habilitado) {
        this.habilitado = habilitado;
        this.dias = Math.max(1, dias);
        Path base = directorio == null
                ? Path.of(System.getProperty("user.home"), ".sfjfx", "logs")
                : directorio;
        this.directorio = base;
    }

    /** Escribe una linea de informacion. */
    public void info(String mensaje) {
        escribir("INFO", mensaje);
    }

    /** Escribe una linea de error. */
    public void error(String mensaje, Throwable error) {
        escribir("ERROR", mensaje + (error == null ? "" : " · " + error.getMessage()));
    }

    private synchronized void escribir(String nivel, String mensaje) {
        if (!habilitado) {
            return;
        }
        try {
            Files.createDirectories(directorio);
            Path archivo = directorio.resolve("sfjfx-" + LocalDate.now() + ".log");
            String linea = LocalDateTime.now().format(MARCA) + " [" + nivel + "] " + mensaje
                    + System.lineSeparator();
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            pendientes.add(linea);
            if (pendientes.size() > 200) {
                pendientes.remove(0);
            }
            rotar();
        } catch (IOException e) {
            // El log nunca debe impedir la operacion.
            System.err.println("No se pudo escribir en el log: " + e.getMessage());
        }
    }

    /** Elimina los archivos de log mas antiguos que el limite de dias. */
    private void rotar() {
        try (var archivos = Files.list(directorio)) {
            List<Path> logs = archivos
                    .filter(p -> p.getFileName().toString().startsWith("sfjfx-"))
                    .sorted()
                    .toList();
            int exceso = logs.size() - dias;
            for (int i = 0; i < exceso; i++) {
                Files.deleteIfExists(logs.get(i));
            }
        } catch (IOException e) {
            System.err.println("No se pudo rotar el log: " + e.getMessage());
        }
    }

    public Path directorio() {
        return directorio;
    }

    public boolean habilitado() {
        return habilitado;
    }

    /**
     * Redirige {@code System.out} y {@code System.err} al archivo de log, conservando la salida en
     * consola. Se invoca al inicio de la aplicacion y en la CLI.
     */
    public void redirigirSalida() {
        if (!habilitado) {
            return;
        }
        PrintStream salidaOriginal = System.out;
        PrintStream errorOriginal = System.err;
        System.setOut(new PrintStream(new SalidaTee(salidaOriginal, "INFO"), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new SalidaTee(errorOriginal, "ERROR"), true, StandardCharsets.UTF_8));
    }

    /** Escribe en consola y en el archivo de log. */
    private final class SalidaTee extends java.io.OutputStream {

        private final PrintStream original;
        private final String nivel;
        private final StringBuilder buffer = new StringBuilder();

        SalidaTee(PrintStream original, String nivel) {
            this.original = original;
            this.nivel = nivel;
        }

        @Override
        public void write(int b) {
            original.write(b);
            acumular((char) b);
        }

        @Override
        public void write(byte[] bytes, int off, int len) {
            if (bytes == null) {
                return;
            }
            original.write(bytes, off, len);
            for (int i = off; i < off + len; i++) {
                acumular((char) (bytes[i] & 0xFF));
            }
        }

        @Override
        public void flush() {
            original.flush();
        }

        private void acumular(char caracter) {
            if (caracter == '\n') {
                flushLinea();
            } else {
                buffer.append(caracter);
            }
        }

        private void flushLinea() {
            String linea = buffer.toString().stripTrailing();
            buffer.setLength(0);
            if (!linea.isBlank()) {
                escribir(nivel, linea);
            }
        }
    }
}
