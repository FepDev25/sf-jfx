package ec.sfjfx.dev;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.seguridad.SeguridadPass;

/**
 * Semilla de desarrollo: crea una empresa demo con sucursal, bodegas, caja,
 * modulos activos, consecutivos y un usuario administrador.
 *
 * <p><strong>Herramienta de desarrollo.</strong> No forma parte del producto
 * instalable ni del artefacto de produccion. Se ejecuta manualmente en la PC
 * del desarrollador, nunca en una instalacion de cliente. Requiere que
 * {@code core} este instalado en el repositorio local
 * ({@code mvn -pl core install -DskipTests}).
 *
 * <p>Es idempotente: si la empresa demo ya existe, no hace nada.
 * Credenciales por defecto: {@code admin / admin} (PIN {@code 1234}).
 */
public final class SemillaDesarrollo {

    public static final String RUC_DEMO = "1790011678001";
    public static final String USUARIO_ADMIN = "admin";
    public static final String PASSWORD_ADMIN = "admin";
    public static final String PIN_ADMIN = "1234";

    private static final List<String> TIPOS_COMPROBANTE = List.of(
            "FACTURA", "NOTA_CREDITO", "NOTA_DEBITO", "RETENCION", "GUIA_REMISION", "LIQUIDACION_COMPRA");

    private SemillaDesarrollo() {
    }

    /** Ejecuta la semilla. Devuelve {@code true} si sembro datos, {@code false} si ya existian. */
    public static boolean ejecutar() throws SQLException {
        GuardiaSemilla.verificarEntorno();
        try (Connection c = ConexionBD.abrirTransaccional()) {
            try {
                if (empresaExiste(c)) {
                    c.rollback();
                    return false;
                }
                long empresaId = crearEmpresa(c);
                long sucursalId = crearSucursal(c, empresaId);
                long bodegaAlmacen = crearBodega(c, sucursalId, "ALM", "Almacen principal", "ALMACEN");
                crearBodega(c, sucursalId, "PISO", "Area de venta", "VENTA");
                crearCaja(c, sucursalId, "CAJA01", "Caja 1");
                long usuarioId = crearAdmin(c, empresaId);

                asignarRol(c, usuarioId, "Administrador");
                asignarSucursal(c, usuarioId, sucursalId);
                activarModulos(c, empresaId);
                crearConsecutivos(c, empresaId, sucursalId);
                crearConfiguracion(c, empresaId);

                c.commit();
                System.out.println("Semilla aplicada. Empresa demo id=" + empresaId
                        + ", bodega almacen id=" + bodegaAlmacen
                        + ", usuario=" + USUARIO_ADMIN);
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static boolean empresaExiste(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM empresa WHERE ruc = ?")) {
            ps.setString(1, RUC_DEMO);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static long crearEmpresa(Connection c) throws SQLException {
        String sql = "INSERT INTO empresa (ruc, razon_social, nombre_comercial, direccion_matriz, "
                + "telefono, email, regimen, moneda) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, RUC_DEMO);
            ps.setString(2, "Supermercado Demo S.A.");
            ps.setString(3, "Supermercado Demo");
            ps.setString(4, "Av. Principal y Calle 1, Quito");
            ps.setString(5, "022345678");
            ps.setString(6, "contacto@demo.ec");
            ps.setString(7, "GENERAL");
            ps.setString(8, "USD");
            return insertarYRetornarId(ps);
        }
    }

    private static long crearSucursal(Connection c, long empresaId) throws SQLException {
        String sql = "INSERT INTO sucursal (empresa_id, codigo, nombre, direccion, es_matriz) "
                + "VALUES (?, ?, ?, ?, TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, "001");
            ps.setString(3, "Matriz Quito");
            ps.setString(4, "Av. Principal y Calle 1, Quito");
            return insertarYRetornarId(ps);
        }
    }

    private static long crearBodega(Connection c, long sucursalId, String codigo, String nombre, String tipo)
            throws SQLException {
        String sql = "INSERT INTO bodega (sucursal_id, codigo, nombre, tipo) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sucursalId);
            ps.setString(2, codigo);
            ps.setString(3, nombre);
            ps.setString(4, tipo);
            return insertarYRetornarId(ps);
        }
    }

    private static long crearCaja(Connection c, long sucursalId, String codigo, String nombre) throws SQLException {
        String sql = "INSERT INTO caja (sucursal_id, codigo, nombre) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sucursalId);
            ps.setString(2, codigo);
            ps.setString(3, nombre);
            return insertarYRetornarId(ps);
        }
    }

    private static long crearAdmin(Connection c, long empresaId) throws SQLException {
        String sql = "INSERT INTO usuario (empresa_id, nombre_usuario, nombres, apellidos, email, "
                + "password_hash, pin_hash, es_superadmin) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, USUARIO_ADMIN);
            ps.setString(3, "Administrador");
            ps.setString(4, "del Sistema");
            ps.setString(5, "admin@demo.ec");
            ps.setString(6, SeguridadPass.hashear(PASSWORD_ADMIN));
            ps.setString(7, SeguridadPass.hashear(PIN_ADMIN));
            return insertarYRetornarId(ps);
        }
    }

    private static void asignarRol(Connection c, long usuarioId, String nombreRol) throws SQLException {
        String sql = "INSERT INTO usuario_rol (usuario_id, rol_id) "
                + "SELECT ?, id FROM rol WHERE nombre = ? AND empresa_id IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setString(2, nombreRol);
            ps.executeUpdate();
        }
    }

    private static void asignarSucursal(Connection c, long usuarioId, long sucursalId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO usuario_sucursal (usuario_id, sucursal_id) VALUES (?, ?)")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, sucursalId);
            ps.executeUpdate();
        }
    }

    private static void activarModulos(Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO empresa_modulo (empresa_id, modulo_codigo, activo) VALUES (?, ?, TRUE)")) {
            for (ModuloSistema m : ModuloSistema.values()) {
                ps.setLong(1, empresaId);
                ps.setString(2, m.codigo());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void crearConsecutivos(Connection c, long empresaId, long sucursalId) throws SQLException {
        String sql = "INSERT INTO secuencia_documento "
                + "(empresa_id, sucursal_id, tipo_comprobante, establecimiento, punto_emision, secuencial_actual) "
                + "VALUES (?, ?, ?, '001', '001', 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String tipo : TIPOS_COMPROBANTE) {
                ps.setLong(1, empresaId);
                ps.setLong(2, sucursalId);
                ps.setString(3, tipo);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void crearConfiguracion(Connection c, long empresaId) throws SQLException {
        String sql = "INSERT INTO configuracion (empresa_id, clave, valor, tipo, grupo, descripcion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            insertarConfig(ps, empresaId, "perfil", "SUPERMERCADO", "TEXTO", "general", "Perfil de instalacion");
            insertarConfig(ps, empresaId, "emisor.ambiente", "PRUEBAS", "TEXTO", "facturacion", "Ambiente SRI");
            insertarConfig(ps, empresaId, "emisor.obligado_contabilidad", "true", "BOOLEAN", "facturacion", "");
            insertarConfig(ps, empresaId, "pos.permitir_stock_negativo", "true", "BOOLEAN", "pos", "");
        }
    }

    private static void insertarConfig(PreparedStatement ps, long empresaId, String clave, String valor,
                                       String tipo, String grupo, String descripcion) throws SQLException {
        ps.setLong(1, empresaId);
        ps.setString(2, clave);
        ps.setString(3, valor);
        ps.setString(4, tipo);
        ps.setString(5, grupo);
        ps.setString(6, descripcion);
        ps.addBatch();
    }

    private static long insertarYRetornarId(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public static void main(String[] args) throws Exception {
        AppConfig.cargar();
        boolean sembrado = ejecutar();
        System.out.println(sembrado
                ? "Semilla de desarrollo creada (admin/admin)."
                : "La empresa demo ya existe; no se hizo nada.");
    }
}
