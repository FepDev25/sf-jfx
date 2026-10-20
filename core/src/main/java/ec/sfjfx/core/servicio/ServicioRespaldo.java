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
import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
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
        return respaldar(programado, empresa(), SesionActual.requerida().usuario().id());
    }

    /** Resultado de un respaldo generado desde consola (sin sesion de usuario). */
    public record RespaldoResultado(String ruta, long tamanoBytes, boolean verificado, String detalle) {
        public String tamanoLegible() {
            if (tamanoBytes < 1024) {
                return tamanoBytes + " B";
            }
            double kb = tamanoBytes / 1024.0;
            if (kb < 1024) {
                return String.format("%.1f KB", kb);
            }
            return String.format("%.1f MB", kb / 1024.0);
        }
    }

    /**
     * Genera un respaldo sin requerir sesion de usuario.
     *
     * <p>Se usa desde la linea de comandos para las tareas programadas del sistema operativo
     * (cron o Task Scheduler), que corren con la aplicacion cerrada.
     */
    public RespaldoResultado respaldarDesdeConsola() {
        Long empresaId = empresaDeInstalacion();
        Path destino = null;
        try {
            Path directorio = directorioSinSesion();
            Files.createDirectories(directorio);
            DatosConexion datos = datosConexion();
            destino = directorio.resolve("sfjfx-" + datos.baseDatos() + "-"
                    + java.time.LocalDateTime.now().format(MARCA) + ".dump");
            List<String> comando = new ArrayList<>();
            comando.add(pgDumpSinSesion());
            comando.add("--format=custom");
            comando.add("--no-owner");
            comando.add("--no-privileges");
            comando.add("--file=" + destino.toAbsolutePath());
            agregarConexion(comando, datos);
            Ejecucion ejecucion = ejecutar(comando, datos);
            if (ejecucion.codigo() != 0) {
                throw new IllegalStateException("pg_dump fallo (" + ejecucion.codigo() + "): "
                        + recortar(ejecucion.salida()));
            }
            String verificacion = verificarArchivo(destino);
            if (verificacion.startsWith("ERROR")) {
                throw new IllegalStateException(verificacion);
            }
            long tamano = Files.isRegularFile(destino) ? Files.size(destino) : 0;
            registrar(empresaId, null, destino, tamano, "PROGRAMADO", "OK", verificacion);
            return new RespaldoResultado(destino.toAbsolutePath().toString(), tamano, true, verificacion);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo ejecutar el respaldo: " + e.getMessage(), e);
        }
    }

    private Respaldo respaldar(boolean programado, long empresaId, Long usuarioId) {
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
            } else {
                detalle = verificarArchivo(destino);
                if (detalle.startsWith("ERROR")) {
                    estado = "ERROR";
                }
            }
            long tamano = Files.isRegularFile(destino) ? Files.size(destino) : 0;
            Respaldo respaldo = new Respaldo(null, empresaId, destino.toAbsolutePath().toString(), tamano,
                    programado ? "PROGRAMADO" : "MANUAL", estado, usuarioId,
                    java.time.LocalDateTime.now(), detalle);
            long id = registrar(empresaId, usuarioId, destino, tamano, respaldo.tipo(), estado, detalle);
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
            registrar(empresaId, usuarioId, destino, 0, respaldo.tipo(), "ERROR", respaldo.detalle());
            throw new IllegalStateException("No se pudo ejecutar el respaldo: " + e.getMessage(), e);
        }
    }

    /** Registra el respaldo en el historial. La auditoria solo aplica si hay sesion de usuario. */
    private long registrar(Long empresaId, Long usuarioId, Path destino, long tamano, String tipo,
                           String estado, String detalle) {
        Respaldo respaldo = new Respaldo(null, empresaId,
                destino == null ? "" : destino.toAbsolutePath().toString(), tamano,
                tipo, estado, usuarioId, java.time.LocalDateTime.now(), detalle);
        long id = repo.insertar(respaldo);
        if (SesionActual.haySesion()) {
            auditoria.registrar("RESPALDO", "respaldo", id, "CREAR", null,
                    Map.of("estado", estado, "tipo", tipo));
        }
        return id;
    }

    /** Verifica que un archivo de respaldo sea legible por pg_restore. */
    public String verificar(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new ValidacionExcepcion("Seleccione un archivo de respaldo valido");
        }
        String resultado = verificarArchivo(archivo);
        if (resultado.startsWith("ERROR")) {
            throw new IllegalStateException(resultado);
        }
        return resultado;
    }

    private String verificarArchivo(Path archivo) {
        try {
            if (!Files.isRegularFile(archivo) || Files.size(archivo) == 0) {
                return "ERROR: el archivo de respaldo esta vacio";
            }
            List<String> comando = List.of(
                    pgRestore(),
                    "--list",
                    archivo.toAbsolutePath().toString());
            Ejecucion ejecucion = ejecutar(comando, datosConexion());
            if (ejecucion.codigo() != 0) {
                return recortar("ERROR: pg_restore --list fallo (" + ejecucion.codigo() + "): "
                        + ejecucion.salida());
            }
            return "Respaldo verificado";
        } catch (IOException e) {
            return "ERROR: no se pudo verificar el respaldo: " + e.getMessage();
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

    /**
     * Directorio de respaldos sin sesion de usuario.
     *
     * <p>Usa la configuracion de la empresa de la instalacion y, si no existe, el valor por
     * defecto {@code ~/.sfjfx/respaldos}.
     */
    public Path directorioSinSesion() {
        Long empresaId = empresaDeInstalacion();
        if (empresaId == null) {
            return Path.of(System.getProperty("user.home"), ".sfjfx", "respaldos");
        }
        String valor = configuracion.valor(empresaId, CLAVE_DIRECTORIO, null);
        if (valor == null || valor.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".sfjfx", "respaldos");
        }
        return expandir(valor.trim());
    }

    /** Empresa de la instalacion, sin depender de la sesion de usuario. */
    private Long empresaDeInstalacion() {
        return new EmpresaRepositorio().primeraActiva().map(Empresa::id).orElse(null);
    }

    private Path expandir(String ruta) {
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

    /** Ruta de {@code pg_dump}. Sin sesion de usuario usa la de la instalacion o el valor por defecto. */
    public String pgDump() {
        if (!SesionActual.haySesion()) {
            return pgDumpSinSesion();
        }
        return configuracion.valor(empresa(), CLAVE_PG_DUMP, "pg_dump");
    }

    /** Ruta de {@code pg_restore}. Sin sesion de usuario usa la de la instalacion o el valor por defecto. */
    public String pgRestore() {
        if (!SesionActual.haySesion()) {
            return pgRestoreSinSesion();
        }
        return configuracion.valor(empresa(), CLAVE_PG_RESTORE, "pg_restore");
    }

    /** Ruta de {@code pg_dump} sin sesion de usuario. */
    private String pgDumpSinSesion() {
        Long empresaId = empresaDeInstalacion();
        String valor = empresaId == null ? null : configuracion.valor(empresaId, CLAVE_PG_DUMP, null);
        return valor == null || valor.isBlank() ? "pg_dump" : valor.trim();
    }

    /** Ruta de {@code pg_restore} sin sesion de usuario. */
    private String pgRestoreSinSesion() {
        Long empresaId = empresaDeInstalacion();
        String valor = empresaId == null ? null : configuracion.valor(empresaId, CLAVE_PG_RESTORE, null);
        return valor == null || valor.isBlank() ? "pg_restore" : valor.trim();
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
