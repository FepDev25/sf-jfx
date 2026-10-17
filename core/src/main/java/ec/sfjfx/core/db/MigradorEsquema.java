package ec.sfjfx.core.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Aplica las migraciones versionadas del esquema.
 *
 * <p>Los archivos viven en el classpath bajo {@code db/migracion} con el formato
 * {@code V<version>__<descripcion>.sql} (por ejemplo {@code V1__nucleo.sql}).
 * Cada migracion se ejecuta dentro de una transaccion y se registra en la tabla
 * {@code migracion_esquema}. Las ya aplicadas se omiten.
 */
public final class MigradorEsquema {

    private static final String DIRECTORIO = "db/migracion";
    private static final String TABLA_CONTROL = "migracion_esquema";

    private final Connection conexion;

    public MigradorEsquema(Connection conexion) {
        this.conexion = conexion;
    }

    /** Aplica todas las migraciones pendientes. */
    public void migrar() throws SQLException, IOException {
        crearTablaControl();
        Set<String> aplicadas = versionesAplicadas();

        List<RecursoMigracion> pendientes = new ArrayList<>();
        for (RecursoMigracion recurso : descubrirRecursos()) {
            if (!aplicadas.contains(recurso.version())) {
                pendientes.add(recurso);
            }
        }
        pendientes.sort(Comparator.comparing(RecursoMigracion::version, MigradorEsquema::compararVersiones));

        for (RecursoMigracion recurso : pendientes) {
            aplicar(recurso);
        }
    }

    /** Versiones ya aplicadas, en orden de aplicacion. */
    public List<String> historial() throws SQLException {
        crearTablaControl();
        List<String> versiones = new ArrayList<>();
        String sql = "SELECT version FROM " + TABLA_CONTROL + " ORDER BY aplicada_en";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                versiones.add(rs.getString(1));
            }
        }
        return versiones;
    }

    /** Estado completo: migraciones disponibles, aplicadas y pendientes. */
    public List<MigracionInfo> estado() throws SQLException, IOException {
        crearTablaControl();
        Map<String, LocalDateTime> aplicadas = new HashMap<>();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT version, aplicada_en FROM " + TABLA_CONTROL)) {
            while (rs.next()) {
                aplicadas.put(rs.getString("version"), rs.getObject("aplicada_en", LocalDateTime.class));
            }
        }

        List<RecursoMigracion> recursos = new ArrayList<>(descubrirRecursos());
        recursos.sort(Comparator.comparing(RecursoMigracion::version, MigradorEsquema::compararVersiones));
        List<MigracionInfo> resultado = new ArrayList<>();
        for (RecursoMigracion recurso : recursos) {
            LocalDateTime aplicadaEn = aplicadas.get(recurso.version());
            resultado.add(new MigracionInfo(recurso.version(), recurso.descripcion(),
                    aplicadaEn != null, aplicadaEn));
        }
        return resultado;
    }

    private void crearTablaControl() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLA_CONTROL + " ("
                + "version VARCHAR(50) PRIMARY KEY, "
                + "descripcion TEXT, "
                + "aplicada_en TIMESTAMP NOT NULL DEFAULT now())";
        try (Statement st = conexion.createStatement()) {
            st.execute(sql);
        }
    }

    private Set<String> versionesAplicadas() throws SQLException {
        Set<String> versiones = new HashSet<>();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM " + TABLA_CONTROL)) {
            while (rs.next()) {
                versiones.add(rs.getString(1));
            }
        }
        return versiones;
    }

    private void aplicar(RecursoMigracion recurso) throws SQLException, IOException {
        String contenido = leer(recurso);
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            try (Statement st = conexion.createStatement()) {
                st.execute(contenido);
            }
            try (PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO " + TABLA_CONTROL + " (version, descripcion) VALUES (?, ?)")) {
                ps.setString(1, recurso.version());
                ps.setString(2, recurso.descripcion());
                ps.executeUpdate();
            }
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    private String leer(RecursoMigracion recurso) throws IOException {
        try (InputStream entrada = recurso.url().openStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<RecursoMigracion> descubrirRecursos() throws IOException {
        URL directorio = MigradorEsquema.class.getClassLoader().getResource(DIRECTORIO);
        if (directorio == null) {
            return List.of();
        }
        String protocolo = directorio.getProtocol();
        try {
            if ("file".equals(protocolo)) {
                return descubrirEnDisco(Path.of(directorio.toURI()));
            }
            if ("jar".equals(protocolo)) {
                return descubrirEnJar(directorio);
            }
        } catch (URISyntaxException e) {
            throw new IOException("Ruta de migraciones invalida", e);
        }
        return List.of();
    }

    private List<RecursoMigracion> descubrirEnDisco(Path directorio) throws IOException {
        List<RecursoMigracion> recursos = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(directorio)) {
            for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                RecursoMigracion recurso = interpretar(archivo.getFileName().toString(), archivo.toUri().toURL());
                if (recurso != null) {
                    recursos.add(recurso);
                }
            }
        }
        return recursos;
    }

    private List<RecursoMigracion> descubrirEnJar(URL directorio) throws IOException {
        List<RecursoMigracion> recursos = new ArrayList<>();
        String rutaJar = directorio.getPath().substring(0, directorio.getPath().indexOf('!'));
        try (JarFile jar = new JarFile(Path.of(rutaJar).toFile())) {
            var entradas = jar.entries();
            while (entradas.hasMoreElements()) {
                JarEntry entrada = entradas.nextElement();
                if (entrada.isDirectory()) {
                    continue;
                }
                String nombre = entrada.getName();
                if (!nombre.startsWith(DIRECTORIO + "/")) {
                    continue;
                }
                String simple = nombre.substring(nombre.lastIndexOf('/') + 1);
                RecursoMigracion recurso = interpretar(simple, new URL(directorio, simple));
                if (recurso != null) {
                    recursos.add(recurso);
                }
            }
        }
        return recursos;
    }

    private RecursoMigracion interpretar(String nombreArchivo, URL url) {
        if (!nombreArchivo.matches("V\\d+__.*\\.sql")) {
            return null;
        }
        int separador = nombreArchivo.indexOf("__");
        String version = nombreArchivo.substring(1, separador);
        String descripcion = nombreArchivo.substring(separador + 2, nombreArchivo.length() - 4).replace('_', ' ');
        return new RecursoMigracion(version, descripcion, url);
    }

    private static int compararVersiones(String a, String b) {
        return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
    }

    private record RecursoMigracion(String version, String descripcion, URL url) {
    }
}
