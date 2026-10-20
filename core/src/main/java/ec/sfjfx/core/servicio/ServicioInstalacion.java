package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigradorEsquema;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.modulos.PerfilInstalacion;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;
import ec.sfjfx.core.repositorio.RepositorioExcepcion;
import ec.sfjfx.core.repositorio.UsuarioRepositorio;
import ec.sfjfx.core.seguridad.SeguridadPass;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

/**
 * Primer arranque de una instalacion single-tenant.
 *
 * <p>Ejecuta las migraciones, crea la empresa/sucursal/bodega/caja y el administrador inicial, y
 * registra los metadatos en {@code instalacion}. Todo el alta se hace en una sola transaccion.
 * No crea datos de demostracion ni semillas.
 */
public final class ServicioInstalacion {

    public static final String MODO_UNICA_EMPRESA = Instalacion.MODO_UNICA_EMPRESA;
    public static final String MODO_MULTIEMPRESA = Instalacion.MODO_MULTIEMPRESA;

    private static final String ESTABLECIMIENTO = "001";
    private static final String PUNTO_EMISION = "001";
    private static final String ROL_ADMIN = "Administrador";
    private static final Set<String> REGIMENES = Set.of(
            "GENERAL", "RIMPE_EMPRENDEDOR", "RIMPE_NEGOCIO_POPULAR");
    private static final Set<String> AMBIENTES_SRI = Set.of("PRUEBAS", "PRODUCCION");
    private static final List<String> TIPOS_COMPROBANTE = List.of(
            "FACTURA", "NOTA_CREDITO", "NOTA_DEBITO", "RETENCION", "GUIA_REMISION", "LIQUIDACION_COMPRA");

    private final InstalacionRepositorio instalaciones = new InstalacionRepositorio();
    private final EmpresaRepositorio empresas = new EmpresaRepositorio();
    private final UsuarioRepositorio usuarios = new UsuarioRepositorio();

    // ------------------------------------------------------------------ estado

    public record Estado(
            boolean esquemaListo,
            boolean instalada,
            int migracionesPendientes,
            long empresas,
            long usuarios,
            String versionEsquema,
            String nombre,
            String modo) {
    }

    public Estado estado() {
        return Bd.consulta(c -> {
            boolean esquemaListo = existeTabla(c, "instalacion")
                    && existeTabla(c, "empresa")
                    && existeTabla(c, "usuario");
            int pendientes;
            try {
                pendientes = (int) new MigradorEsquema(c).estado().stream()
                        .filter(m -> !m.aplicada())
                        .count();
            } catch (IOException e) {
                throw new SQLException("No se pudo leer el estado de las migraciones", e);
            }
            if (!esquemaListo) {
                return new Estado(false, false, pendientes, 0, 0, null, null, null);
            }
            Optional<Instalacion> instalacion = instalaciones.obtener(c);
            long totalEmpresas = contar(c, "empresa");
            long totalUsuarios = contar(c, "usuario");
            return new Estado(
                    true,
                    instalacion.isPresent(),
                    pendientes,
                    totalEmpresas,
                    totalUsuarios,
                    versionEsquema(c),
                    instalacion.map(Instalacion::nombre).orElse(null),
                    instalacion.map(Instalacion::modo).orElse(null));
        });
    }

    /** Indica si la base esta vacia y necesita el asistente de primer arranque. */
    public boolean requiereInstalacion() {
        Estado estado = estado();
        return !estado.instalada() && estado.empresas() == 0 && estado.usuarios() == 0;
    }

    /** Aplica migraciones pendientes y completa metadatos de instalaciones existentes. */
    public void prepararEsquema() {
        try {
            ConexionBD.asegurarEsquema();
        } catch (SQLException e) {
            throw new RepositorioExcepcion("No se pudo preparar el esquema de la base", e);
        }
        Bd.consulta(c -> {
            try {
                new MigradorEsquema(c).migrar();
                return null;
            } catch (IOException e) {
                throw new SQLException("No se pudieron aplicar las migraciones", e);
            }
        });
        registrarSiFalta();
    }

    /**
     * Registra los metadatos de instalacion si la base ya tiene empresas pero aun no tiene la fila.
     *
     * <p>Se ejecuta tras aplicar migraciones para que una instalacion existente (creada antes de
     * {@code V8}) quede registrada con su codigo unico de licencia.
     */
    public void registrarSiFalta() {
        backfillInstalacion();
    }

    /** Indica si la instalacion admite mas de una empresa legal. */
    public boolean permiteMultiempresa() {
        try {
            return instalaciones.obtener()
                    .map(Instalacion::esMultiempresa)
                    .orElse(true);
        } catch (RuntimeException e) {
            // Base anterior a V8 o sin migrar: no bloquear la operacion.
            return true;
        }
    }

    // ------------------------------------------------------------------ instalacion

    public record DatosEmpresa(
            String ruc,
            String razonSocial,
            String nombreComercial,
            String regimen,
            String perfil,
            String direccion,
            String telefono,
            String email,
            boolean contribuyenteEspecial,
            boolean obligadoContabilidad) {
    }

    public record DatosOrganizacion(
            String sucursalCodigo,
            String sucursalNombre,
            String sucursalDireccion,
            String sucursalTelefono,
            String bodegaCodigo,
            String bodegaNombre,
            String cajaCodigo,
            String cajaNombre) {
    }

    public record DatosSri(String ambiente) {
    }

    public record DatosAdmin(
            String usuario,
            String nombres,
            String apellidos,
            String email,
            String telefono,
            String password,
            String pin) {
    }

    public record DatosInstalacion(
            String nombreInstalacion,
            String modo,
            DatosEmpresa empresa,
            DatosOrganizacion organizacion,
            DatosSri sri,
            DatosAdmin admin) {
    }

    public record ResultadoInstalacion(
            long empresaId,
            long sucursalId,
            long bodegaId,
            long cajaId,
            long usuarioId,
            String nombreUsuario) {
    }

    public ResultadoInstalacion instalar(DatosInstalacion datos) {
        validar(datos);
        prepararEsquema();

        Estado estado = estado();
        if (estado.instalada() || estado.empresas() > 0 || estado.usuarios() > 0) {
            throw new ValidacionExcepcion("Ya existe una instalacion en esta base de datos");
        }
        if (empresas.existeRuc(datos.empresa().ruc().trim())) {
            throw new ValidacionExcepcion("Ya existe una empresa con ese RUC");
        }
        if (usuarios.existeNombreUsuario(datos.admin().usuario().trim(), null)) {
            throw new ValidacionExcepcion("Ya existe un usuario con ese nombre");
        }

        String modo = normalizarModo(datos.modo());
        String perfilEtiqueta = datos.empresa().perfil().trim();
        PerfilInstalacion perfil = PerfilInstalacion.porEtiqueta(perfilEtiqueta);
        String ambiente = datos.sri().ambiente().trim().toUpperCase(Locale.ROOT);

        return Bd.transaccion(c -> {
            String version = versionEsquema(c);

            long empresaId = insertarEmpresa(c, datos.empresa());
            long sucursalId = insertarSucursal(c, empresaId, datos.organizacion());
            long bodegaId = insertarBodega(c, sucursalId,
                    datos.organizacion().bodegaCodigo(), datos.organizacion().bodegaNombre(), "ALMACEN");
            insertarBodegaVenta(c, sucursalId, datos.organizacion().bodegaCodigo());
            long cajaId = insertarCaja(c, sucursalId, datos.organizacion());
            long usuarioId = insertarAdmin(c, empresaId, datos.admin());

            asignarRolAdmin(c, usuarioId);
            asignarSucursal(c, usuarioId, sucursalId);
            configurarModulos(c, empresaId, perfil);
            configurarSecuencias(c, empresaId, sucursalId);
            configurarParametros(c, empresaId, datos, perfil, ambiente);
            insertarListaPrecioDefault(c, empresaId);
            instalaciones.insertar(c, datos.nombreInstalacion().trim(), modo, version);

            return new ResultadoInstalacion(empresaId, sucursalId, bodegaId, cajaId,
                    usuarioId, datos.admin().usuario().trim());
        });
    }

    // ------------------------------------------------------------------ validaciones

    private void validar(DatosInstalacion datos) {
        if (datos == null) {
            throw new ValidacionExcepcion("Faltan los datos de instalacion");
        }
        Validaciones.requerido(datos.nombreInstalacion(), "Nombre de la instalacion");
        normalizarModo(datos.modo());

        DatosEmpresa empresa = datos.empresa();
        if (empresa == null) {
            throw new ValidacionExcepcion("Faltan los datos de la empresa");
        }
        Validaciones.requerido(empresa.ruc(), "RUC");
        if (!Validaciones.identificacionEcuador(empresa.ruc())) {
            throw new ValidacionExcepcion("El RUC no es valido");
        }
        Validaciones.requerido(empresa.razonSocial(), "Razon social");
        String regimen = empresa.regimen() == null ? "" : empresa.regimen().trim().toUpperCase(Locale.ROOT);
        if (!REGIMENES.contains(regimen)) {
            throw new ValidacionExcepcion("Seleccione un regimen valido");
        }
        if (!emailValido(empresa.email())) {
            throw new ValidacionExcepcion("El email de la empresa no es valido");
        }
        validarPerfil(empresa.perfil());

        DatosOrganizacion organizacion = datos.organizacion();
        if (organizacion == null) {
            throw new ValidacionExcepcion("Faltan los datos de la organizacion");
        }
        Validaciones.requerido(organizacion.sucursalCodigo(), "Codigo de sucursal");
        Validaciones.requerido(organizacion.sucursalNombre(), "Nombre de sucursal");
        Validaciones.requerido(organizacion.bodegaCodigo(), "Codigo de bodega");
        Validaciones.requerido(organizacion.bodegaNombre(), "Nombre de bodega");
        Validaciones.requerido(organizacion.cajaCodigo(), "Codigo de caja");
        Validaciones.requerido(organizacion.cajaNombre(), "Nombre de caja");

        if (datos.sri() == null) {
            throw new ValidacionExcepcion("Falta el ambiente SRI");
        }
        String ambiente = datos.sri().ambiente() == null ? ""
                : datos.sri().ambiente().trim().toUpperCase(Locale.ROOT);
        if (!AMBIENTES_SRI.contains(ambiente)) {
            throw new ValidacionExcepcion("Seleccione un ambiente SRI valido");
        }

        DatosAdmin admin = datos.admin();
        if (admin == null) {
            throw new ValidacionExcepcion("Faltan los datos del administrador");
        }
        Validaciones.requerido(admin.usuario(), "Usuario del administrador");
        Validaciones.requerido(admin.nombres(), "Nombres del administrador");
        if (!emailValido(admin.email())) {
            throw new ValidacionExcepcion("El email del administrador no es valido");
        }
        Validaciones.requerido(admin.password(), "Contrasena del administrador");
        if (admin.password().length() < 8) {
            throw new ValidacionExcepcion("La contrasena debe tener al menos 8 caracteres");
        }
        if (admin.pin() == null || !admin.pin().matches("\\d{4,8}")) {
            throw new ValidacionExcepcion("El PIN debe tener entre 4 y 8 digitos");
        }
    }

    private void validarPerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            throw new ValidacionExcepcion("Seleccione un perfil de instalacion");
        }
        for (PerfilInstalacion p : PerfilInstalacion.values()) {
            if (p.etiqueta().equalsIgnoreCase(perfil.trim()) || p.name().equalsIgnoreCase(perfil.trim())) {
                return;
            }
        }
        throw new ValidacionExcepcion("Seleccione un perfil de instalacion valido");
    }

    private boolean emailValido(String email) {
        return email == null || email.isBlank() || Validaciones.email(email);
    }

    private String normalizarModo(String modo) {
        if (modo == null || modo.isBlank()) {
            return MODO_UNICA_EMPRESA;
        }
        String valor = modo.trim().toUpperCase(Locale.ROOT);
        if (!MODO_UNICA_EMPRESA.equals(valor) && !MODO_MULTIEMPRESA.equals(valor)) {
            throw new ValidacionExcepcion("Modo de instalacion invalido");
        }
        return valor;
    }

    // ------------------------------------------------------------------ escritura

    private long insertarEmpresa(Connection c, DatosEmpresa e) throws SQLException {
        String sql = "INSERT INTO empresa (ruc, razon_social, nombre_comercial, direccion_matriz, "
                + "telefono, email, contribuyente_especial, obligado_llevar_contabilidad, regimen, moneda, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'USD', TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.ruc().trim());
            ps.setString(2, e.razonSocial().trim());
            ps.setString(3, nuloSiVacio(e.nombreComercial()));
            ps.setString(4, nuloSiVacio(e.direccion()));
            ps.setString(5, nuloSiVacio(e.telefono()));
            ps.setString(6, nuloSiVacio(e.email()));
            ps.setBoolean(7, e.contribuyenteEspecial());
            ps.setBoolean(8, e.obligadoContabilidad());
            ps.setString(9, e.regimen().trim().toUpperCase(Locale.ROOT));
            return id(ps);
        }
    }

    private long insertarSucursal(Connection c, long empresaId, DatosOrganizacion o) throws SQLException {
        String sql = "INSERT INTO sucursal (empresa_id, codigo, nombre, direccion, telefono, es_matriz, activo) "
                + "VALUES (?, ?, ?, ?, ?, TRUE, TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, o.sucursalCodigo().trim());
            ps.setString(3, o.sucursalNombre().trim());
            ps.setString(4, nuloSiVacio(o.sucursalDireccion()));
            ps.setString(5, nuloSiVacio(o.sucursalTelefono()));
            return id(ps);
        }
    }

    private long insertarBodega(Connection c, long sucursalId, String codigo, String nombre, String tipo)
            throws SQLException {
        String sql = "INSERT INTO bodega (sucursal_id, codigo, nombre, tipo, activo) "
                + "VALUES (?, ?, ?, ?, TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sucursalId);
            ps.setString(2, codigo.trim());
            ps.setString(3, nombre.trim());
            ps.setString(4, tipo);
            return id(ps);
        }
    }

    private void insertarBodegaVenta(Connection c, long sucursalId, String codigoAlmacen) throws SQLException {
        String codigo = "PISO".equalsIgnoreCase(codigoAlmacen.trim()) ? "VENTA" : "PISO";
        insertarBodega(c, sucursalId, codigo, "Area de venta", "VENTA");
    }

    private long insertarCaja(Connection c, long sucursalId, DatosOrganizacion o) throws SQLException {
        String sql = "INSERT INTO caja (sucursal_id, codigo, nombre, estado, activo) "
                + "VALUES (?, ?, ?, 'DISPONIBLE', TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sucursalId);
            ps.setString(2, o.cajaCodigo().trim());
            ps.setString(3, o.cajaNombre().trim());
            return id(ps);
        }
    }

    private long insertarAdmin(Connection c, long empresaId, DatosAdmin a) throws SQLException {
        String sql = "INSERT INTO usuario (empresa_id, nombre_usuario, nombres, apellidos, email, telefono, "
                + "password_hash, pin_hash, es_superadmin, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, a.usuario().trim());
            ps.setString(3, a.nombres().trim());
            ps.setString(4, nuloSiVacio(a.apellidos()));
            ps.setString(5, nuloSiVacio(a.email()));
            ps.setString(6, nuloSiVacio(a.telefono()));
            ps.setString(7, SeguridadPass.hashear(a.password()));
            ps.setString(8, SeguridadPass.hashear(a.pin()));
            return id(ps);
        }
    }

    private void asignarRolAdmin(Connection c, long usuarioId) throws SQLException {
        String sql = "INSERT INTO usuario_rol (usuario_id, rol_id) "
                + "SELECT ?, id FROM rol WHERE nombre = ? AND empresa_id IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setString(2, ROL_ADMIN);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No existe el rol " + ROL_ADMIN + "; aplique los datos base (V2)");
            }
        }
    }

    private void asignarSucursal(Connection c, long usuarioId, long sucursalId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO usuario_sucursal (usuario_id, sucursal_id) VALUES (?, ?)")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, sucursalId);
            ps.executeUpdate();
        }
    }

    private void configurarModulos(Connection c, long empresaId, PerfilInstalacion perfil) throws SQLException {
        String sql = "INSERT INTO empresa_modulo (empresa_id, modulo_codigo, activo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (ModuloSistema modulo : ModuloSistema.values()) {
                ps.setLong(1, empresaId);
                ps.setString(2, modulo.codigo());
                ps.setBoolean(3, perfil.incluye(modulo));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void configurarSecuencias(Connection c, long empresaId, long sucursalId) throws SQLException {
        String sql = "INSERT INTO secuencia_documento "
                + "(empresa_id, sucursal_id, tipo_comprobante, establecimiento, punto_emision, secuencial_actual) "
                + "VALUES (?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String tipo : TIPOS_COMPROBANTE) {
                ps.setLong(1, empresaId);
                ps.setLong(2, sucursalId);
                ps.setString(3, tipo);
                ps.setString(4, ESTABLECIMIENTO);
                ps.setString(5, PUNTO_EMISION);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void configurarParametros(Connection c, long empresaId, DatosInstalacion datos,
                                      PerfilInstalacion perfil, String ambiente) throws SQLException {
        String sql = "INSERT INTO configuracion (empresa_id, clave, valor, tipo, grupo, descripcion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            insertarConfig(ps, empresaId, "empresa.perfil", perfil.etiqueta(), "TEXTO", "empresa",
                    "Perfil de instalacion");
            insertarConfig(ps, empresaId, "emisor.ambiente", ambiente, "TEXTO", "facturacion",
                    "Ambiente SRI");
            insertarConfig(ps, empresaId, "emisor.obligado_contabilidad",
                    String.valueOf(datos.empresa().obligadoContabilidad()), "BOOLEAN", "facturacion", "");
            insertarConfig(ps, empresaId, "pos.permitir_stock_negativo", "true", "BOOLEAN", "pos", "");
            ps.executeBatch();
        }
    }

    private void insertarConfig(PreparedStatement ps, long empresaId, String clave, String valor,
                                String tipo, String grupo, String descripcion) throws SQLException {
        ps.setLong(1, empresaId);
        ps.setString(2, clave);
        ps.setString(3, valor);
        ps.setString(4, tipo);
        ps.setString(5, grupo);
        ps.setString(6, descripcion);
        ps.addBatch();
    }

    private void insertarListaPrecioDefault(Connection c, long empresaId) throws SQLException {
        String sql = "INSERT INTO lista_precio (empresa_id, nombre, tipo, moneda, incluye_iva, es_default, activo) "
                + "VALUES (?, 'Publico', 'PUBLICO', 'USD', FALSE, TRUE, TRUE)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.executeUpdate();
        }
    }

    private void backfillInstalacion() {
        if (instalaciones.existe()) {
            return;
        }
        Bd.transaccion(c -> {
            long totalEmpresas = contar(c, "empresa");
            if (totalEmpresas == 0) {
                return null;
            }
            String nombre = "Instalacion existente";
            String nombreComercial = null;
            String razonSocial = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT nombre_comercial, razon_social FROM empresa ORDER BY id LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nombreComercial = rs.getString("nombre_comercial");
                    razonSocial = rs.getString("razon_social");
                }
            }
            if (nombreComercial != null && !nombreComercial.isBlank()) {
                nombre = nombreComercial;
            } else if (razonSocial != null && !razonSocial.isBlank()) {
                nombre = razonSocial;
            }
            String modo = totalEmpresas > 1 ? MODO_MULTIEMPRESA : MODO_UNICA_EMPRESA;
            instalaciones.insertar(c, nombre, modo, versionEsquema(c));
            return null;
        });
    }

    // ------------------------------------------------------------------ helpers

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_name = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private static long contar(Connection c, String tabla) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String versionEsquema(Connection c) {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(version::int)::text FROM migracion_esquema")) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private static long id(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String nuloSiVacio(String valor) {
        return Validaciones.opcional(valor);
    }
}
