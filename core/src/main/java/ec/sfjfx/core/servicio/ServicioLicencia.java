package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.modelo.Licencia;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;
import ec.sfjfx.core.util.ValidacionExcepcion;

/**
 * Validacion y carga de la licencia perpetua con mantenimiento anual.
 *
 * <p>La licencia es un archivo firmado offline. La aplicacion solo necesita la clave publica;
 * nunca se distribuye la clave privada. Si no hay licencia, la aplicacion sigue operando en modo
 * de evaluacion y muestra el aviso correspondiente.
 */
public final class ServicioLicencia {

    public static final String CLAVE_ARCHIVO = "licencia.archivo";
    public static final String CLAVE_CLAVE_PUBLICA = "licencia.clave_publica";
    public static final String CLAVE_GRACIA_DIAS = "licencia.gracia.dias";
    public static final String CLAVE_PRUEBA_DIAS = "licencia.prueba.dias";

    /** Dias de gracia despues de vencido el mantenimiento, antes de pasar a modo consulta. */
    public static final int GRACIA_POR_DEFECTO = 30;
    /** Dias de operacion sin licencia (periodo de prueba) antes de pasar a modo consulta. */
    public static final int PRUEBA_POR_DEFECTO = 30;

    private static final Set<String> EDICIONES = Set.of("SUPERMERCADO", "MINIMERCADO", "TIENDA");

    private final InstalacionRepositorio instalaciones = new InstalacionRepositorio();

    /** Cache breve del estado de licencia para no releer el archivo en cada operacion. */
    private static final class Cache {
        private EstadoLicencia estado;
        private String token;
        private long generado;

        EstadoLicencia valor(String tokenActual, Supplier<EstadoLicencia> calculo) {
            long ahora = System.currentTimeMillis();
            if (estado == null || !token.equals(tokenActual) || ahora - generado > 60_000) {
                estado = calculo.get();
                token = tokenActual;
                generado = ahora;
            }
            return estado;
        }

        void invalidar() {
            estado = null;
            token = null;
        }
    }

    private static final Cache CACHE = new Cache();

    /**
     * Identifica la configuracion vigente. Si cambia (por ejemplo, al pasar de una base a otra),
     * la cache se invalida para no mezclar estados entre instalaciones.
     */
    private String tokenConfiguracion() {
        return AppConfig.entorno()
                + "|" + AppConfig.obtener("db.url", "")
                + "|" + AppConfig.obtener("db.esquema", "public")
                + "|" + archivo().toAbsolutePath()
                + "|" + AppConfig.obtener(CLAVE_CLAVE_PUBLICA, "");
    }

    public record EstadoLicencia(
            boolean presente,
            boolean valida,
            String mensaje,
            String cliente,
            String edicion,
            int maxSucursales,
            int maxCajas,
            LocalDate mantenimientoHasta,
            boolean mantenimientoVigente,
            boolean modoConsulta,
            String motivo) {

        public static EstadoLicencia sinLicencia(String mensaje) {
            return new EstadoLicencia(false, false, mensaje, null, null, 0, 0, null, false, false, null);
        }

        public static EstadoLicencia invalida(String mensaje) {
            return new EstadoLicencia(true, false, mensaje, null, null, 0, 0, null, false, false, null);
        }
    }

    public EstadoLicencia estado() {
        return CACHE.valor(tokenConfiguracion(), this::evaluarEstado);
    }

    private EstadoLicencia evaluarEstado() {
        Path archivo = archivo();
        if (!Files.isRegularFile(archivo)) {
            return evaluar(EstadoLicencia.sinLicencia(
                    "No hay licencia instalada. La aplicacion opera en modo de evaluacion."));
        }
        try {
            return evaluar(validar(Files.readString(archivo, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return evaluar(EstadoLicencia.invalida("No se pudo leer la licencia: " + e.getMessage()));
        } catch (RuntimeException e) {
            return evaluar(EstadoLicencia.invalida("Licencia invalida: " + e.getMessage()));
        }
    }

    /**
     * Indica si la instalacion esta en modo consulta: no se registran operaciones comerciales
     * (ventas, compras, comprobantes ni movimientos de stock), pero si se consultan y exportan.
     */
    public boolean modoConsulta() {
        return estado().modoConsulta();
    }

    /** Bloquea la operacion si la instalacion esta en modo consulta. */
    public void exigirOperacion(String operacion) {
        EstadoLicencia estado = estado();
        if (estado.modoConsulta()) {
            throw new ValidacionExcepcion("La instalacion esta en modo consulta: no se puede "
                    + operacion + ". " + (estado.motivo() == null ? "" : estado.motivo()));
        }
    }

    /** Limpia la cache del estado de licencia (tras cargar o renovar una licencia). */
    public void invalidarCache() {
        CACHE.invalidar();
    }

    /**
     * Calcula si la instalacion debe operar en modo consulta.
     *
     * <p>Reglas: una licencia vigente habilita todo. Una licencia vencida da un periodo de
     * gracia ({@code licencia.gracia.dias}) antes de pasar a modo consulta. Sin licencia valida
     * se concede un periodo de prueba ({@code licencia.prueba.dias}) desde la instalacion.
     *
     * <p>El modo consulta solo aplica en produccion: en desarrollo el sistema nunca se bloquea.
     */
    private EstadoLicencia evaluar(EstadoLicencia base) {
        if (!AppConfig.esProduccion()) {
            return conModo(base, false, null);
        }
        if (base.valida() && base.mantenimientoVigente()) {
            return conModo(base, false, null);
        }
        int gracia = Math.max(0, AppConfig.obtenerInt(CLAVE_GRACIA_DIAS, GRACIA_POR_DEFECTO));
        if (base.valida()) {
            LocalDate vencida = base.mantenimientoHasta();
            long dias = vencida == null ? Integer.MAX_VALUE
                    : java.time.temporal.ChronoUnit.DAYS.between(vencida, LocalDate.now());
            if (dias <= gracia) {
                return conModo(base, false, "Mantenimiento vencido: quedan "
                        + Math.max(0, gracia - dias) + " dia(s) de gracia.");
            }
            return conModo(base, true, "El mantenimiento vencio el " + vencida + ".");
        }
        int prueba = Math.max(0, AppConfig.obtenerInt(CLAVE_PRUEBA_DIAS, PRUEBA_POR_DEFECTO));
        LocalDate inicio = inicioInstalacion();
        if (inicio == null) {
            return conModo(base, true, "No hay licencia valida para esta instalacion.");
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, LocalDate.now());
        if (dias <= prueba) {
            return conModo(base, false, "Sin licencia: periodo de prueba, quedan "
                    + Math.max(0, prueba - dias) + " dia(s).");
        }
        return conModo(base, true, "Sin licencia valida: el periodo de prueba de " + prueba
                + " dias termino el " + inicio.plusDays(prueba) + ".");
    }

    private EstadoLicencia conModo(EstadoLicencia base, boolean modoConsulta, String motivo) {
        return new EstadoLicencia(base.presente(), base.valida(),
                motivo == null ? base.mensaje() : base.mensaje() + " " + motivo,
                base.cliente(), base.edicion(), base.maxSucursales(), base.maxCajas(),
                base.mantenimientoHasta(), base.mantenimientoVigente(), modoConsulta, motivo);
    }

    private LocalDate inicioInstalacion() {
        return instalaciones.obtener()
                .map(Instalacion::creadoEn)
                .filter(fecha -> fecha != null)
                .map(fecha -> fecha.toLocalDate())
                .orElse(null);
    }

    /**
     * Valida una licencia y, si es correcta, la copia al archivo configurado.
     */
    public EstadoLicencia instalar(Path origen) {
        if (origen == null || !Files.isRegularFile(origen)) {
            throw new ValidacionExcepcion("Seleccione un archivo de licencia valido");
        }
        String contenido;
        try {
            contenido = Files.readString(origen, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidacionExcepcion("No se pudo leer la licencia: " + e.getMessage());
        }
        EstadoLicencia estado = validar(contenido);
        if (!estado.valida()) {
            throw new ValidacionExcepcion(estado.mensaje());
        }
        try {
            Path destino = archivo();
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la licencia: " + e.getMessage(), e);
        }
        CACHE.invalidar();
        return estado();
    }

    /** Valida el contenido de un archivo de licencia sin instalarlo. */
    public EstadoLicencia validar(String contenido) {
        Licencia licencia = parse(contenido);

        Optional<Instalacion> instalacion = instalaciones.obtener();
        if (instalacion.isEmpty()) {
            return EstadoLicencia.invalida("No hay una instalacion registrada en esta base de datos");
        }
        if (licencia.idInstalacion() == null
                || !licencia.idInstalacion().equals(instalacion.get().codigoUnico())) {
            return EstadoLicencia.invalida(
                    "La licencia no corresponde a esta instalacion (" + instalacion.get().codigoUnico() + ")");
        }

        PublicKey clave = cargarClavePublica();
        if (clave == null) {
            return EstadoLicencia.invalida(
                    "Falta la clave publica de licencias. Configure " + CLAVE_CLAVE_PUBLICA + ".");
        }
        if (!verificar(licencia, clave)) {
            return EstadoLicencia.invalida("La firma de la licencia no es valida");
        }

        boolean vigente = licencia.mantenimientoVigente();
        String mensaje = vigente
                ? "Licencia valida; mantenimiento vigente"
                : "Licencia valida; mantenimiento vencido.";
        return new EstadoLicencia(true, true, mensaje, licencia.cliente(), licencia.edicion(),
                licencia.maxSucursales(), licencia.maxCajas(), licencia.mantenimientoHasta(), vigente,
                false, null);
    }

    /** Codigo unico de la instalacion, usado para emitir la licencia. */
    public String codigoInstalacion() {
        return instalaciones.obtener().map(Instalacion::codigoUnico).orElse("");
    }

    public Path archivo() {
        return rutaConfigurada(CLAVE_ARCHIVO,
                Path.of(System.getProperty("user.home"), ".sfjfx", "licencia", "licencia.properties"));
    }

    public Path clavePublica() {
        return rutaConfigurada(CLAVE_CLAVE_PUBLICA,
                Path.of(System.getProperty("user.home"), ".sfjfx", "licencia", "public-key.pem"));
    }

    // ------------------------------------------------------------------ helpers

    private Licencia parse(String contenido) {
        Properties props = new Properties();
        try (StringReader reader = new StringReader(contenido)) {
            props.load(reader);
        } catch (IOException e) {
            throw new ValidacionExcepcion("No se pudo parsear la licencia: " + e.getMessage());
        }
        try {
            int version = Integer.parseInt(requerido(props, "versionLicencia"));
            if (version != 1) {
                throw new ValidacionExcepcion("Version de licencia no soportada: " + version);
            }
            String idInstalacion = requerido(props, "idInstalacion");
            String cliente = requerido(props, "cliente");
            String ruc = props.getProperty("ruc", "");
            String edicion = requerido(props, "edicion").toUpperCase(java.util.Locale.ROOT);
            if (!EDICIONES.contains(edicion)) {
                throw new ValidacionExcepcion("Edicion de licencia invalida: " + edicion);
            }
            int maxSucursales = Integer.parseInt(props.getProperty("maxSucursales", "0"));
            int maxCajas = Integer.parseInt(props.getProperty("maxCajas", "0"));
            LocalDate emitida = LocalDate.parse(requerido(props, "emitidaEn"));
            LocalDate mantenimiento = LocalDate.parse(requerido(props, "mantenimientoHasta"));
            String firma = requerido(props, "firma");
            return new Licencia(version, idInstalacion, cliente, ruc, edicion,
                    maxSucursales, maxCajas, emitida, mantenimiento, firma);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ValidacionExcepcion("Formato de licencia invalido: " + e.getMessage());
        }
    }

    private String requerido(Properties props, String clave) {
        String valor = props.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new ValidacionExcepcion("Falta el campo '" + clave + "' en la licencia");
        }
        return valor.trim();
    }

    private boolean verificar(Licencia licencia, PublicKey clave) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(clave);
            signature.update(licencia.payload().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(licencia.firma()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private PublicKey cargarClavePublica() {
        Path ruta = clavePublica();
        String pem = null;
        try {
            if (Files.isRegularFile(ruta)) {
                pem = Files.readString(ruta, StandardCharsets.UTF_8);
            } else {
                try (var entrada = ServicioLicencia.class.getResourceAsStream("/licencia/public-key.pem")) {
                    if (entrada != null) {
                        pem = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        if (pem == null || pem.isBlank()) {
            return null;
        }
        try {
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] bytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    private Path rutaConfigurada(String clave, Path porDefecto) {
        String valor = AppConfig.obtener(clave, null);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        String ruta = valor.trim();
        if (ruta.startsWith("~")) {
            ruta = System.getProperty("user.home") + ruta.substring(1);
        }
        Path path = Path.of(ruta);
        return path.isAbsolute() ? path : Path.of(System.getProperty("user.home")).resolve(path);
    }
}
