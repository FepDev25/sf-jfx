package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.modelo.Respaldo;
import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.repositorio.RespaldoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Respaldo y restauracion de la base de datos con pg_dump/pg_restore. */
public final class ServicioRespaldo {

    public static final String CLAVE_DIRECTORIO = "respaldo.directorio";
    public static final String CLAVE_PROGRAMADO = "respaldo.programado";
    public static final String CLAVE_HORA = "respaldo.hora";
    public static final String CLAVE_PG_DUMP = "respaldo.pg_dump";
    public static final String CLAVE_PG_RESTORE = "respaldo.pg_restore";

    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final RespaldoRepositorio repo = new RespaldoRepositorio();
    private final ConfiguracionRepositorio configuracion = new ConfiguracionRepositorio();
    private final ServicioConfiguracion configuracionServicio = new ServicioConfiguracion();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Respaldo> historial(int limite) {
        return repo.listar(empresa(), limite);
    }

    public Respaldo respaldar(boolean programado) {
        long empresaId = empresa();
        Long usuarioId = SesionActual.requerida().usuario().id();
        Path destino = null;
        String detalle = "Respaldo completado";
        String estado = "OK";
        try {
            Path directorio = directorio();
            Files.createDirectories(directorio);
            DatosConexion datos = datosConexion();
            destino = directorio.resolve("sfjfx-" + datos.baseDatos() + "-"
                    + java.time.LocalDateTime.now().format(MARCA) + ".dump");
            List<String> comando = new ArrayList<>();
            comando.add(pgDump());
            comando.add("--format=custom");
            comando.add("--no-owner");
            comando.add("--no-privileges");
            comando.add("--file=" + destino.toAbsolutePath());
            agregarConexion(comando, datos);
            Ejecucion ejecucion = ejecutar(comando, datos);
            if (ejecucion.codigo() != 0) {
                estado = "ERROR";
                detalle = recortar("pg_dump fallo (" + ejecucion.codigo() + "): " + ejecucion.salida());
            }
            long tamano = Files.isRegularFile(destino) ? Files.size(destino) : 0;
            Respaldo respaldo = new Respaldo(null, empresaId, destino.toAbsolutePath().toString(), tamano,
                    programado ? "PROGRAMADO" : "MANUAL", estado, usuarioId,
                    java.time.LocalDateTime.now(), detalle);
            long id = repo.insertar(respaldo);
            auditoria.registrar("RESPALDO", "respaldo", id, "CREAR", null,
                    Map.of("estado", estado, "tipo", respaldo.tipo()));
            if ("ERROR".equals(estado)) {
                throw new IllegalStateException(detalle);
            }
            return new Respaldo(id, respaldo.empresaId(), respaldo.rutaArchivo(), respaldo.tamanoBytes(),
                    respaldo.tipo(), respaldo.estado(), respaldo.usuarioId(), respaldo.creadoEn(),
                    respaldo.detalle());
        } catch (IOException e) {
            Respaldo respaldo = new Respaldo(null, empresaId, destino == null ? "" : destino.toString(), 0,
                    programado ? "PROGRAMADO" : "MANUAL", "ERROR", usuarioId,
                    java.time.LocalDateTime.now(), recortar(e.getMessage()));
            long id = repo.insertar(respaldo);
            auditoria.registrar("RESPALDO", "respaldo", id, "CREAR", null, Map.of("estado", "ERROR"));
            throw new IllegalStateException("No se pudo ejecutar el respaldo: " + e.getMessage(), e);
        }
    }

    /** Restaura un respaldo. Debe usarse con precaucion: reemplaza objetos de la base actual. */
    public String restaurar(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new ValidacionExcepcion("Seleccione un archivo de respaldo valido");
        }
        try {
            DatosConexion datos = datosConexion();
            List<String> comando = new ArrayList<>();
            comando.add(pgRestore());
            comando.add("--clean");
            comando.add("--if-exists");
            comando.add("--no-owner");
            comando.add("--no-privileges");
            agregarConexion(comando, datos);
            comando.add(archivo.toAbsolutePath().toString());
            Ejecucion ejecucion = ejecutar(comando, datos);
            if (ejecucion.codigo() != 0) {
                throw new IllegalStateException("pg_restore fallo (" + ejecucion.codigo() + "): "
                        + recortar(ejecucion.salida()));
            }
            auditoria.registrar("RESPALDO", "restauracion", null, "CREAR", null,
                    Map.of("archivo", archivo.getFileName().toString()));
            return ejecucion.salida().isBlank() ? "Restauracion completada" : ejecucion.salida();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo ejecutar la restauracion: " + e.getMessage(), e);
        }
    }

    public Path directorio() {
        String valor = configuracion.valor(empresa(), CLAVE_DIRECTORIO, null);
        if (valor == null || valor.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".sfjfx", "respaldos");
        }
        String ruta = valor.trim();
        if (ruta.startsWith("~")) {
            ruta = System.getProperty("user.home") + ruta.substring(1);
        }
        Path path = Path.of(ruta);
        return path.isAbsolute() ? path : Path.of(System.getProperty("user.home")).resolve(path);
    }

    public boolean programadoActivo() {
        return Boolean.parseBoolean(configuracion.valor(empresa(), CLAVE_PROGRAMADO, "false"));
    }

    public String horaProgramada() {
        return configuracion.valor(empresa(), CLAVE_HORA, "23:00");
    }

    public String pgDump() {
        return configuracion.valor(empresa(), CLAVE_PG_DUMP, "pg_dump");
    }

    public String pgRestore() {
        return configuracion.valor(empresa(), CLAVE_PG_RESTORE, "pg_restore");
    }

    public void guardarProgramacion(boolean activo, String hora, String directorio) {
        String horaNormalizada = hora == null || hora.isBlank() ? "23:00" : hora.trim();
        try {
            LocalTime.parse(horaNormalizada, HORA);
        } catch (DateTimeParseException e) {
            throw new ValidacionExcepcion("La hora debe tener el formato HH:mm");
        }
        configuracionServicio.guardar(CLAVE_PROGRAMADO, Boolean.toString(activo), "BOOLEAN",
                "Respaldos", "Ejecutar respaldos automaticos");
        configuracionServicio.guardar(CLAVE_HORA, horaNormalizada, "TEXTO",
                "Respaldos", "Hora diaria del respaldo automatico");
        configuracionServicio.guardar(CLAVE_DIRECTORIO,
                directorio == null || directorio.isBlank() ? directorio().toString() : directorio.trim(),
                "TEXTO", "Respaldos", "Directorio donde se guardan los respaldos");
    }

    /** Ejecuta el respaldo diario si esta activo, ya paso la hora y no se hizo hoy. */
    public boolean ejecutarProgramadoSiCorresponde() {
        if (!programadoActivo()) {
            return false;
        }
        LocalTime hora;
        try {
            hora = LocalTime.parse(horaProgramada(), HORA);
        } catch (DateTimeParseException e) {
            return false;
        }
        if (LocalTime.now().isBefore(hora)) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        boolean yaHecho = repo.ultimoProgramado(empresa())
                .map(Respaldo::creadoEn)
                .map(fecha -> fecha.toLocalDate().equals(hoy))
                .orElse(false);
        if (yaHecho) {
            return false;
        }
        respaldar(true);
        return true;
    }

    public void eliminarRegistro(long id) {
        repo.eliminar(id, empresa());
        auditoria.registrar("RESPALDO", "respaldo", id, "ELIMINAR", null, null);
    }

    private DatosConexion datosConexion() {
        String url = AppConfig.obtener("db.url");
        URI uri = URI.create(url.substring("jdbc:".length()));
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int puerto = uri.getPort() <= 0 ? 5432 : uri.getPort();
        String base = uri.getPath() == null || uri.getPath().isBlank()
                ? "sfjfx" : uri.getPath().substring(1);
        return new DatosConexion(host, puerto, base,
                AppConfig.obtener("db.usuario"), AppConfig.obtener("db.password"));
    }

    private void agregarConexion(List<String> comando, DatosConexion datos) {
        comando.add("--host=" + datos.host());
        comando.add("--port=" + datos.puerto());
        comando.add("--username=" + datos.usuario());
        comando.add("--dbname=" + datos.baseDatos());
    }

    private Ejecucion ejecutar(List<String> comando, DatosConexion datos) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(comando);
        builder.redirectErrorStream(true);
        builder.environment().put("PGPASSWORD", datos.password());
        builder.environment().put("PGCONNECT_TIMEOUT", "15");
        Process proceso = builder.start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            int codigo = proceso.waitFor();
            return new Ejecucion(codigo, salida);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proceso.destroyForcibly();
            throw new IOException("Ejecucion interrumpida", e);
        }
    }

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.trim();
        return limpio.length() > 500 ? limpio.substring(0, 500) : limpio;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private record DatosConexion(String host, int puerto, String baseDatos, String usuario, String password) {
    }

    private record Ejecucion(int codigo, String salida) {
    }
}
