package ec.sfjfx.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carga la configuracion de la aplicacion (conexion a base de datos, rutas, etc.).
 *
 * <p>Orden de busqueda del archivo {@code sfjfx.properties}:
 * <ol>
 *   <li>Variable de entorno {@code SFJFX_CONFIG} (ruta a un archivo).</li>
 *   <li>Propiedad de sistema {@code sfjfx.config} (ruta a un archivo).</li>
 *   <li>{@code $HOME/.sfjfx/sfjfx.properties}.</li>
 *   <li>Recurso empaquetado {@code /sfjfx.properties} en el classpath.</li>
 * </ol>
 * Los valores encontrados sobrescriben los valores por defecto.
 */
public final class AppConfig {

    public static final String ARCHIVO = "sfjfx.properties";

    private static final Properties PROPIEDADES = new Properties();
    private static boolean cargada = false;

    private AppConfig() {
    }

    /** Carga la configuracion una sola vez. Es idempotente. */
    public static synchronized void cargar() {
        if (cargada) {
            return;
        }
        aplicarValoresPorDefecto();

        Path externo = resolverRutaExterna();
        if (externo != null && Files.isRegularFile(externo)) {
            try (InputStream entrada = Files.newInputStream(externo)) {
                PROPIEDADES.load(entrada);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + externo, e);
            }
        } else {
            cargarDesdeClasspath();
        }
        cargada = true;
    }

    public static synchronized void recargar() {
        cargada = false;
        PROPIEDADES.clear();
        cargar();
    }

    public static String obtener(String clave) {
        cargar();
        String valor = PROPIEDADES.getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la propiedad requerida: " + clave);
        }
        return valor.trim();
    }

    public static String obtener(String clave, String porDefecto) {
        cargar();
        String valor = PROPIEDADES.getProperty(clave);
        return valor == null ? porDefecto : valor.trim();
    }

    public static int obtenerInt(String clave, int porDefecto) {
        String valor = obtener(clave, null);
        return valor == null ? porDefecto : Integer.parseInt(valor);
    }

    public static boolean obtenerBooleano(String clave, boolean porDefecto) {
        String valor = obtener(clave, null);
        return valor == null ? porDefecto : Boolean.parseBoolean(valor);
    }

    private static void aplicarValoresPorDefecto() {
        PROPIEDADES.clear();
        PROPIEDADES.setProperty("db.url", "jdbc:postgresql://localhost:5432/sfjfx");
        PROPIEDADES.setProperty("db.usuario", "sfjfx");
        PROPIEDADES.setProperty("db.password", "sfjfx");
        PROPIEDADES.setProperty("db.esquema", "public");
        PROPIEDADES.setProperty("app.entorno", "dev");
    }

    private static Path resolverRutaExterna() {
        String ruta = System.getProperty("sfjfx.config");
        if (ruta == null || ruta.isBlank()) {
            ruta = System.getenv("SFJFX_CONFIG");
        }
        if (ruta != null && !ruta.isBlank()) {
            return Path.of(ruta);
        }
        return Path.of(System.getProperty("user.home"), ".sfjfx", ARCHIVO);
    }

    private static void cargarDesdeClasspath() {
        try (InputStream entrada = AppConfig.class.getResourceAsStream("/" + ARCHIVO)) {
            if (entrada != null) {
                PROPIEDADES.load(entrada);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ARCHIVO + " del classpath", e);
        }
    }
}
