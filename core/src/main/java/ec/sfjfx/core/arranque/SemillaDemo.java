package ec.sfjfx.core.arranque;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.seguridad.SeguridadPass;
import ec.sfjfx.core.sri.CatalogoSri;
import ec.sfjfx.core.sri.ClaveAcceso;

/**
 * Semilla de demostracion masiva: usuarios con distintos roles y credenciales,
 * sucursales, catalogo, stock, clientes, proveedores, ventas, compras,
 * promociones, cupones, dispositivos y reportes guardados.
 *
 * <p>Es idempotente mediante la clave de configuracion {@code demo.seed_version}.
 * Se ejecuta automaticamente desde {@link Arranque} o con
 * {@code mvn -pl core exec:java -Dexec.mainClass=ec.sfjfx.core.arranque.SemillaDemo}.
 */
public final class SemillaDemo {

    public static final String VERSION = "1";
    private static final String RUC = SemillaDesarrollo.RUC_DEMO;
    private static final Random RND = new Random(20261017L);

    public record Credencial(String usuario, String password, String pin, String rol) {
    }

    public static final List<Credencial> CREDENCIALES = List.of(
            new Credencial("admin", "admin", "1234", "Administrador (superadmin)"),
            new Credencial("gerente", "gerente123", "1111", "Gerente"),
            new Credencial("supervisor", "supervisor123", "2222", "Supervisor"),
            new Credencial("cajero1", "cajero123", "3333", "Cajero · Matriz"),
            new Credencial("cajero2", "cajero123", "3333", "Cajero · Norte"),
            new Credencial("bodeguero", "bodeguero123", "4444", "Bodeguero"),
            new Credencial("contador", "contador123", "5555", "Contador"),
            new Credencial("consulta", "consulta123", "6666", "Consulta (solo lectura)"));

    private record ProductoSeed(String codigo, String nombre, String categoria, String marca, String unidad,
                                String costo, String margen, boolean iva15, int stockMatriz, int stockNorte,
                                int diasVence) {
    }

    private record ProductoCreado(long id, String codigo, String nombre, BigDecimal costo, BigDecimal precio,
                                  Long impuestoId, BigDecimal ivaPorcentaje) {
    }

    private record ClienteCreado(long id, String tipoIdentificacion, String numeroIdentificacion,
                                 String razonSocial, String email, boolean credito, BigDecimal limite) {
    }

    private record ProveedorCreado(long id, String razonSocial) {
    }

    private record LineaVenta(ProductoCreado producto, BigDecimal cantidad, BigDecimal precio,
                              BigDecimal iva, BigDecimal subtotal, BigDecimal impuesto, BigDecimal total,
                              BigDecimal costo) {
    }

    private static final List<ProductoSeed> PRODUCTOS = List.of(
            new ProductoSeed("P001", "Arroz Diana 5 kg", "GRANOS", "La Favorita", "UNIDAD", "4.20", "1.30", false, 80, 40, 0),
            new ProductoSeed("P002", "Arroz integral 2 kg", "GRANOS", "Marca Blanca", "UNIDAD", "2.80", "1.30", false, 40, 20, 0),
            new ProductoSeed("P003", "Azucar blanca 2 kg", "GRANOS", "La Favorita", "UNIDAD", "2.10", "1.25", false, 60, 30, 0),
            new ProductoSeed("P004", "Aceite de girasol 1 L", "ACEITES", "La Favorita", "UNIDAD", "2.35", "1.30", true, 50, 25, 0),
            new ProductoSeed("P005", "Aceite de oliva 500 ml", "ACEITES", "Marca Blanca", "UNIDAD", "6.80", "1.35", true, 2, 1, 0),
            new ProductoSeed("P006", "Atun en agua 170 g", "ABARROTES", "Marca Blanca", "UNIDAD", "1.10", "1.35", true, 100, 50, 0),
            new ProductoSeed("P007", "Fideos tallarin 500 g", "ABARROTES", "La Favorita", "UNIDAD", "0.75", "1.30", true, 90, 45, 0),
            new ProductoSeed("P008", "Sal refinada 1 kg", "ABARROTES", "Marca Blanca", "UNIDAD", "0.45", "1.25", false, 70, 35, 0),
            new ProductoSeed("P009", "Cafe molido 250 g", "ABARROTES", "Nestle", "UNIDAD", "3.20", "1.35", true, 30, 15, 0),
            new ProductoSeed("P010", "Leche entera 1 L", "LACTEOS", "Dos Pinos", "LT", "1.05", "1.25", false, 100, 50, 10),
            new ProductoSeed("P011", "Yogurt natural 1 L", "LACTEOS", "Tonicorp", "LT", "2.40", "1.30", true, 30, 15, 15),
            new ProductoSeed("P012", "Queso fresco 500 g", "LACTEOS", "Pronaca", "UNIDAD", "3.10", "1.30", true, 25, 12, 15),
            new ProductoSeed("P013", "Huevos 12 unidades", "LACTEOS", "Pronaca", "UNIDAD", "2.80", "1.25", false, 40, 20, 0),
            new ProductoSeed("P014", "Pollo entero 1 kg", "CARNES", "Pronaca", "KG", "3.20", "1.25", false, 20, 10, 5),
            new ProductoSeed("P015", "Carne molida 1 kg", "CARNES", "Pronaca", "KG", "5.60", "1.25", false, 15, 8, 5),
            new ProductoSeed("P016", "Salchichas 500 g", "CARNES", "Pronaca", "UNIDAD", "2.90", "1.30", true, 35, 18, 0),
            new ProductoSeed("P017", "Gaseosa cola 3 L", "GASEOSAS", "Coca-Cola", "LT", "1.85", "1.30", true, 60, 30, 0),
            new ProductoSeed("P018", "Gaseosa limon 2 L", "GASEOSAS", "PepsiCo", "LT", "1.40", "1.30", true, 45, 22, 0),
            new ProductoSeed("P019", "Agua mineral 5 L", "AGUAS", "Marca Blanca", "LT", "1.20", "1.25", false, 50, 25, 0),
            new ProductoSeed("P020", "Agua con gas 1.5 L", "AGUAS", "Coca-Cola", "LT", "0.85", "1.30", true, 40, 20, 0),
            new ProductoSeed("P021", "Jugo de naranja 1 L", "BEBIDAS", "Tonicorp", "LT", "1.60", "1.30", true, 30, 15, 0),
            new ProductoSeed("P022", "Cerveza six pack", "BEBIDAS", "Colombiana", "UNIDAD", "5.80", "1.30", true, 20, 10, 0),
            new ProductoSeed("P023", "Detergente 3 kg", "LIMPIEZA", "P&G", "UNIDAD", "4.90", "1.30", true, 25, 12, 0),
            new ProductoSeed("P024", "Jabon de bano 3 pack", "LIMPIEZA", "Unilever", "UNIDAD", "2.70", "1.30", true, 40, 20, 0),
            new ProductoSeed("P025", "Papel higienico 12 rollos", "LIMPIEZA", "P&G", "UNIDAD", "5.20", "1.30", true, 30, 15, 0),
            new ProductoSeed("P026", "Shampoo 750 ml", "CUIDADO", "Unilever", "UNIDAD", "4.10", "1.35", true, 4, 2, 0),
            new ProductoSeed("P027", "Pasta dental 150 g", "CUIDADO", "P&G", "UNIDAD", "1.90", "1.30", true, 30, 15, 0),
            new ProductoSeed("P028", "Galletas de chocolate 300 g", "SNACKS", "Nestle", "UNIDAD", "1.25", "1.35", true, 50, 25, 0),
            new ProductoSeed("P029", "Papas fritas 250 g", "SNACKS", "PepsiCo", "UNIDAD", "1.10", "1.35", true, 45, 22, 0),
            new ProductoSeed("P030", "Helado de vainilla 1 L", "CONGELADOS", "Nestle", "LT", "3.50", "1.30", true, 3, 2, 30),
            new ProductoSeed("P031", "Manzana roja 1 kg", "FRUTAS", "Marca Blanca", "KG", "1.40", "1.25", false, 25, 12, 0),
            new ProductoSeed("P032", "Platano 1 kg", "FRUTAS", "Marca Blanca", "KG", "0.55", "1.20", false, 35, 18, 0));

    private SemillaDemo() {
    }

    /** Ejecuta la semilla masiva. Devuelve {@code true} si inserto datos nuevos. */
    public static boolean ejecutar() throws SQLException {
        if (empresaId() == 0) {
            SemillaDesarrollo.ejecutar();
        }
        try (Connection c = ConexionBD.abrirTransaccional()) {
            try {
                long empresaId = buscarId(c, "SELECT id FROM empresa WHERE ruc = ?", RUC);
                if (empresaId == 0) {
                    throw new SQLException("No existe la empresa demo");
                }
                if (marcador(c, empresaId)) {
                    c.rollback();
                    return false;
                }

                long matriz = sucursal(c, empresaId, "001", "Matriz Quito");
                long norte = sucursal(c, empresaId, "002", "Sucursal Norte");
                long bodegaMatriz = bodega(c, matriz, "ALM", "Almacen principal", "ALMACEN");
                long bodegaNorte = bodega(c, norte, "ALM-N", "Almacen Norte", "ALMACEN");
                bodega(c, matriz, "PISO", "Area de venta", "VENTA");
                bodega(c, norte, "PISO-N", "Area de venta Norte", "VENTA");
                long caja1 = caja(c, matriz, "CAJA01", "Caja 1");
                long caja2 = caja(c, norte, "CAJA02", "Caja Norte");

                Map<String, Long> usuarios = crearUsuarios(c, empresaId, matriz, norte);
                Map<String, Long> categorias = crearCategorias(c, empresaId);
                Map<String, Long> marcas = crearMarcas(c, empresaId);
                long listaPublico = lista(c, empresaId, "Publico", "PUBLICO", true);
                long listaMayorista = lista(c, empresaId, "Mayorista", "MAYORISTA", false);
                long listaCorporativo = lista(c, empresaId, "Corporativo", "CORPORATIVO", false);
                long iva15 = buscarId(c, "SELECT id FROM impuesto WHERE nombre = 'IVA 15%'");
                long iva0 = buscarId(c, "SELECT id FROM impuesto WHERE nombre = 'IVA 0%'");
                long tipoInicial = buscarId(c, "SELECT id FROM tipo_movimiento_inventario WHERE codigo = 'INICIAL'");
                long tipoVenta = buscarId(c, "SELECT id FROM tipo_movimiento_inventario WHERE codigo = 'VENTA'");

                List<ProductoCreado> productos = crearProductos(c, empresaId, categorias, marcas, listaPublico,
                        listaMayorista, iva15, iva0, tipoInicial, bodegaMatriz, bodegaNorte, matriz, norte);
                crearDispositivos(c, empresaId, matriz, norte, caja1, caja2);
                List<ClienteCreado> clientes = crearClientes(c, empresaId, matriz);
                List<ProveedorCreado> proveedores = crearProveedores(c, empresaId);
                crearFidelidad(c, empresaId, clientes);
                crearPromociones(c, empresaId, categorias, marcas, productos);
                crearCupones(c, empresaId);

                long cajero1 = usuarios.get("cajero1");
                long cajero2 = usuarios.get("cajero2");
                long sesion1 = sesion(c, empresaId, caja1, cajero1, LocalDateTime.now().minusDays(30),
                        LocalDateTime.now().minusDays(1), "CERRADA", "180.00");
                long sesion2 = sesion(c, empresaId, caja2, cajero2, LocalDateTime.now().minusDays(29),
                        LocalDateTime.now().minusHours(4), "CERRADA", "120.00");

                generarVentas(c, empresaId, matriz, norte, caja1, caja2, sesion1, sesion2,
                        bodegaMatriz, bodegaNorte, productos, clientes, cajero1, cajero2);
                actualizarSecuencias(c, empresaId, matriz);
                actualizarSecuencias(c, empresaId, norte);
                generarCompras(c, empresaId, matriz, proveedores, productos, usuarios.get("contador"));
                generarAbonos(c, empresaId);
                actualizarSesiones(c);

                abrirSesionSiFalta(c, empresaId, caja1, cajero1, "100.00");
                abrirSesionSiFalta(c, empresaId, caja2, cajero2, "80.00");
                reportesGuardados(c, empresaId, usuarios.get("admin") == null ? 1 : usuarios.get("admin"));

                actualizar(c, "INSERT INTO configuracion (empresa_id, clave, valor, tipo, grupo, descripcion) "
                        + "VALUES (?, 'demo.seed_version', ?, 'TEXTO', 'demo', 'Datos de demostracion masivos') "
                        + "ON CONFLICT (empresa_id, clave) DO UPDATE SET valor = EXCLUDED.valor",
                        empresaId, VERSION);

                c.commit();
                imprimirResumen(productos.size(), clientes.size(), proveedores.size());
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static long empresaId() {
        try (Connection c = ConexionBD.abrir()) {
            return buscarId(c, "SELECT id FROM empresa WHERE ruc = ?", RUC);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la empresa demo", e);
        }
    }

    private static boolean marcador(Connection c, long empresaId) throws SQLException {
        long id = buscarId(c, "SELECT id FROM configuracion WHERE empresa_id = ? "
                + "AND clave = 'demo.seed_version' AND valor = ?", empresaId, VERSION);
        return id > 0;
    }

    // ------------------------------------------------------------------ organizacion y usuarios

    private static Map<String, Long> crearUsuarios(Connection c, long empresaId, long matriz, long norte)
            throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        long consultaRol = asegurarRolConsulta(c, empresaId);
        ids.put("gerente", usuario(c, empresaId, "gerente", "gerente123", "1111",
                "Gabriela", "Mendieta", "gerente@demo.ec", "Gerente", matriz, norte));
        ids.put("supervisor", usuario(c, empresaId, "supervisor", "supervisor123", "2222",
                "Mauricio", "Salazar", "supervisor@demo.ec", "Supervisor", matriz, norte));
        ids.put("cajero1", usuario(c, empresaId, "cajero1", "cajero123", "3333",
                "Daniela", "Cevallos", "cajero1@demo.ec", "Cajero", matriz));
        ids.put("cajero2", usuario(c, empresaId, "cajero2", "cajero123", "3333",
                "Andres", "Vinueza", "cajero2@demo.ec", "Cajero", norte));
        ids.put("bodeguero", usuario(c, empresaId, "bodeguero", "bodeguero123", "4444",
                "Patricio", "Guaman", "bodega@demo.ec", "Bodeguero", matriz, norte));
        ids.put("contador", usuario(c, empresaId, "contador", "contador123", "5555",
                "Veronica", "Paredes", "contador@demo.ec", "Contador", matriz, norte));
        ids.put("consulta", usuario(c, empresaId, "consulta", "consulta123", "6666",
                "Lector", "Invitado", "consulta@demo.ec", null, matriz, norte));
        ids.put("admin", buscarId(c, "SELECT id FROM usuario WHERE nombre_usuario = 'admin'"));
        return ids;
    }

    private static long asegurarRolConsulta(Connection c, long empresaId) throws SQLException {
        long id = buscarId(c, "SELECT id FROM rol WHERE empresa_id = ? AND nombre = 'Consulta'", empresaId);
        if (id == 0) {
            id = insertar(c, "INSERT INTO rol (empresa_id, nombre, descripcion, es_sistema) "
                    + "VALUES (?, 'Consulta', 'Acceso de solo lectura para demostracion', FALSE) RETURNING id",
                    empresaId);
        }
        actualizar(c, "INSERT INTO rol_permiso (rol_id, permiso_id) "
                + "SELECT ?, id FROM permiso WHERE accion = 'VER' "
                + "AND modulo IN ('POS','PRODUCTOS','INVENTARIO','CLIENTES','REPORTES','FACTURACION') "
                + "ON CONFLICT DO NOTHING", id);
        return id;
    }

    private static long usuario(Connection c, long empresaId, String username, String password, String pin,
                                String nombres, String apellidos, String email, String rol, long... sucursales)
            throws SQLException {
        long id = insertar(c, "INSERT INTO usuario (empresa_id, nombre_usuario, nombres, apellidos, email, "
                + "password_hash, pin_hash) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (nombre_usuario) DO UPDATE SET empresa_id = EXCLUDED.empresa_id, "
                + "nombres = EXCLUDED.nombres, apellidos = EXCLUDED.apellidos, email = EXCLUDED.email, "
                + "password_hash = EXCLUDED.password_hash, pin_hash = EXCLUDED.pin_hash, "
                + "activo = TRUE, bloqueado = FALSE, intentos_fallidos = 0 RETURNING id",
                empresaId, username, nombres, apellidos, email,
                SeguridadPass.hashear(password), SeguridadPass.hashear(pin));
        if (rol != null) {
            actualizar(c, "INSERT INTO usuario_rol (usuario_id, rol_id) "
                    + "SELECT ?, id FROM rol WHERE nombre = ? AND empresa_id IS NULL ON CONFLICT DO NOTHING",
                    id, rol);
        } else {
            long consulta = buscarId(c, "SELECT id FROM rol WHERE empresa_id = ? AND nombre = 'Consulta'", empresaId);
            actualizar(c, "INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    id, consulta);
        }
        for (long sucursal : sucursales) {
            actualizar(c, "INSERT INTO usuario_sucursal (usuario_id, sucursal_id) VALUES (?, ?) "
                    + "ON CONFLICT DO NOTHING", id, sucursal);
        }
        return id;
    }

    private static long sucursal(Connection c, long empresaId, String codigo, String nombre) throws SQLException {
        return insertar(c, "INSERT INTO sucursal (empresa_id, codigo, nombre, direccion, es_matriz) "
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (empresa_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre "
                + "RETURNING id", empresaId, codigo, nombre, "Sucursal " + nombre, "002".equals(codigo));
    }

    private static long bodega(Connection c, long sucursalId, String codigo, String nombre, String tipo)
            throws SQLException {
        long id = buscarId(c, "SELECT id FROM bodega WHERE sucursal_id = ? AND codigo = ?", sucursalId, codigo);
        if (id == 0) {
            id = insertar(c, "INSERT INTO bodega (sucursal_id, codigo, nombre, tipo) VALUES (?, ?, ?, ?) RETURNING id",
                    sucursalId, codigo, nombre, tipo);
        }
        return id;
    }

    private static long caja(Connection c, long sucursalId, String codigo, String nombre) throws SQLException {
        long id = buscarId(c, "SELECT id FROM caja WHERE sucursal_id = ? AND codigo = ?", sucursalId, codigo);
        if (id == 0) {
            id = insertar(c, "INSERT INTO caja (sucursal_id, codigo, nombre) VALUES (?, ?, ?) RETURNING id",
                    sucursalId, codigo, nombre);
        }
        return id;
    }

    // ------------------------------------------------------------------ catalogo

    private static Map<String, Long> crearCategorias(Connection c, long empresaId) throws SQLException {
        Map<String, Long> categorias = new LinkedHashMap<>();
        long abarrotes = categoria(c, empresaId, null, "ABARROTES", "Abarrotes");
        long bebidas = categoria(c, empresaId, null, "BEBIDAS", "Bebidas");
        long lacteos = categoria(c, empresaId, null, "LACTEOS", "Lacteos y huevos");
        long carnes = categoria(c, empresaId, null, "CARNES", "Carnes y embutidos");
        long limpieza = categoria(c, empresaId, null, "LIMPIEZA", "Limpieza");
        long cuidado = categoria(c, empresaId, null, "CUIDADO", "Cuidado personal");
        long snacks = categoria(c, empresaId, null, "SNACKS", "Snacks");
        long congelados = categoria(c, empresaId, null, "CONGELADOS", "Congelados");
        long frutas = categoria(c, empresaId, null, "FRUTAS", "Frutas y verduras");
        categorias.put("ABARROTES", abarrotes);
        categorias.put("GRANOS", categoria(c, empresaId, abarrotes, "GRANOS", "Arroz, azucar y granos"));
        categorias.put("ACEITES", categoria(c, empresaId, abarrotes, "ACEITES", "Aceites y condimentos"));
        categorias.put("BEBIDAS", bebidas);
        categorias.put("GASEOSAS", categoria(c, empresaId, bebidas, "GASEOSAS", "Gaseosas"));
        categorias.put("AGUAS", categoria(c, empresaId, bebidas, "AGUAS", "Aguas"));
        categorias.put("LACTEOS", lacteos);
        categorias.put("CARNES", carnes);
        categorias.put("LIMPIEZA", limpieza);
        categorias.put("CUIDADO", cuidado);
        categorias.put("SNACKS", snacks);
        categorias.put("CONGELADOS", congelados);
        categorias.put("FRUTAS", frutas);
        return categorias;
    }

    private static long categoria(Connection c, long empresaId, Long padreId, String codigo, String nombre)
            throws SQLException {
        return insertar(c, "INSERT INTO categoria (empresa_id, padre_id, codigo, nombre) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (empresa_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, "
                + "padre_id = EXCLUDED.padre_id RETURNING id", empresaId, padreId, codigo, nombre);
    }

    private static Map<String, Long> crearMarcas(Connection c, long empresaId) throws SQLException {
        Map<String, Long> marcas = new LinkedHashMap<>();
        for (String nombre : List.of("Marca Blanca", "La Favorita", "Nestle", "Coca-Cola", "PepsiCo",
                "P&G", "Unilever", "Pronaca", "Dos Pinos", "Colombiana", "Tonicorp")) {
            marcas.put(nombre, insertar(c, "INSERT INTO marca (empresa_id, nombre) VALUES (?, ?) "
                    + "ON CONFLICT (empresa_id, nombre) DO UPDATE SET activo = TRUE RETURNING id", empresaId, nombre));
        }
        return marcas;
    }

    private static long lista(Connection c, long empresaId, String nombre, String tipo, boolean defecto)
            throws SQLException {
        return insertar(c, "INSERT INTO lista_precio (empresa_id, nombre, tipo, es_default) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (empresa_id, nombre) DO UPDATE SET tipo = EXCLUDED.tipo, "
                + "es_default = EXCLUDED.es_default, activo = TRUE RETURNING id",
                empresaId, nombre, tipo, defecto);
    }

    private static List<ProductoCreado> crearProductos(Connection c, long empresaId, Map<String, Long> categorias,
                                                       Map<String, Long> marcas, long listaPublico, long listaMayorista,
                                                       long iva15, long iva0, long tipoInicial, long bodegaMatriz,
                                                       long bodegaNorte, long matriz, long norte) throws SQLException {
        Map<String, Long> unidades = new HashMap<>();
        for (String codigo : List.of("UNIDAD", "KG", "LT")) {
            unidades.put(codigo, buscarId(c, "SELECT id FROM unidad_medida WHERE codigo = ?", codigo));
        }
        long listaCorporativo = lista(c, empresaId, "Corporativo", "CORPORATIVO", false);
        List<ProductoCreado> productos = new ArrayList<>();
        int indice = 1;
        for (ProductoSeed p : PRODUCTOS) {
            BigDecimal costo = new BigDecimal(p.costo());
            BigDecimal precio = costo.multiply(new BigDecimal(p.margen())).setScale(2, RoundingMode.HALF_UP);
            Long impuesto = p.iva15() ? iva15 : iva0;
            BigDecimal porcentaje = p.iva15() ? new BigDecimal("15") : BigDecimal.ZERO;
            long id = insertar(c, "INSERT INTO producto (empresa_id, codigo, nombre, categoria_id, marca_id, "
                    + "unidad_medida_id, tipo, costo, costo_promedio, controla_lote, controla_serie, es_granel, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 'SIMPLE', ?, ?, ?, FALSE, FALSE, TRUE) "
                    + "ON CONFLICT (empresa_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, "
                    + "categoria_id = EXCLUDED.categoria_id, marca_id = EXCLUDED.marca_id, "
                    + "unidad_medida_id = EXCLUDED.unidad_medida_id, costo = EXCLUDED.costo, "
                    + "costo_promedio = EXCLUDED.costo_promedio, controla_lote = EXCLUDED.controla_lote, "
                    + "activo = TRUE RETURNING id",
                    empresaId, p.codigo(), p.nombre(), categorias.get(p.categoria()), marcas.get(p.marca()),
                    unidades.get(p.unidad()), costo, costo, p.diasVence() > 0);
            actualizar(c, "INSERT INTO producto_impuesto (producto_id, impuesto_id) VALUES (?, ?) "
                    + "ON CONFLICT DO NOTHING", id, impuesto);
            actualizar(c, "INSERT INTO producto_codigo_barra (producto_id, codigo, tipo, es_principal) "
                    + "VALUES (?, ?, 'EAN13', TRUE) ON CONFLICT (codigo) DO NOTHING", id, ean13(indice));
            precio(c, listaPublico, id, precio);
            precio(c, listaMayorista, id, costo.multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP));
            precio(c, listaCorporativo, id, costo.multiply(new BigDecimal("1.15")).setScale(2, RoundingMode.HALF_UP));

            stockInicial(c, empresaId, matriz, bodegaMatriz, id, BigDecimal.valueOf(p.stockMatriz()), costo,
                    tipoInicial);
            stockInicial(c, empresaId, norte, bodegaNorte, id, BigDecimal.valueOf(p.stockNorte()), costo,
                    tipoInicial);
            if (p.diasVence() > 0) {
                long lote = insertar(c, "INSERT INTO lote (producto_id, codigo, fecha_vencimiento) "
                        + "VALUES (?, ?, ?) RETURNING id", id, "L-" + p.codigo(),
                        LocalDate.now().plusDays(p.diasVence()));
                stockLote(c, empresaId, matriz, bodegaMatriz, id, lote, BigDecimal.valueOf(Math.min(10, p.stockMatriz())),
                        costo, tipoInicial);
                stockLote(c, empresaId, norte, bodegaNorte, id, lote, BigDecimal.valueOf(Math.min(5, p.stockNorte())),
                        costo, tipoInicial);
            }
            productos.add(new ProductoCreado(id, p.codigo(), p.nombre(), costo, precio, impuesto, porcentaje));
            indice++;
        }
        return productos;
    }

    private static void precio(Connection c, long lista, long producto, BigDecimal precio) throws SQLException {
        actualizar(c, "INSERT INTO precio_producto (lista_precio_id, producto_id, precio, vigente_desde) "
                + "SELECT ?, ?, ?, CURRENT_DATE - 90 "
                + "WHERE NOT EXISTS (SELECT 1 FROM precio_producto WHERE lista_precio_id = ? AND producto_id = ? "
                + "AND sucursal_id IS NULL AND vigente_hasta IS NULL)",
                lista, producto, precio, lista, producto);
    }

    private static void stockInicial(Connection c, long empresaId, long sucursalId, long bodegaId, long productoId,
                                     BigDecimal cantidad, BigDecimal costo, long tipoInicial) throws SQLException {
        long stockId = buscarId(c, "SELECT id FROM stock WHERE bodega_id = ? AND producto_id = ? AND lote_id IS NULL",
                bodegaId, productoId);
        if (stockId == 0) {
            insertar(c, "INSERT INTO stock (bodega_id, producto_id, cantidad) VALUES (?, ?, ?) RETURNING id",
                    bodegaId, productoId, cantidad);
            movimiento(c, empresaId, sucursalId, bodegaId, productoId, null, tipoInicial, cantidad, costo,
                    cantidad, cantidad.multiply(costo), "INICIAL", null, "Carga inicial demo");
        }
    }

    private static void stockLote(Connection c, long empresaId, long sucursalId, long bodegaId, long productoId,
                                  long loteId, BigDecimal cantidad, BigDecimal costo, long tipoInicial)
            throws SQLException {
        long stockId = buscarId(c, "SELECT id FROM stock WHERE bodega_id = ? AND producto_id = ? AND lote_id = ?",
                bodegaId, productoId, loteId);
        if (stockId == 0) {
            insertar(c, "INSERT INTO stock (bodega_id, producto_id, lote_id, cantidad) VALUES (?, ?, ?, ?) RETURNING id",
                    bodegaId, productoId, loteId, cantidad);
            movimiento(c, empresaId, sucursalId, bodegaId, productoId, loteId, tipoInicial, cantidad, costo,
                    cantidad, cantidad.multiply(costo), "INICIAL", null, "Lote inicial demo");
        }
    }

    // ------------------------------------------------------------------ terceros

    private static List<ClienteCreado> crearClientes(Connection c, long empresaId, long sucursal)
            throws SQLException {
        List<ClienteCreado> clientes = new ArrayList<>();
        clientes.add(cliente(c, empresaId, "CONSUMIDOR_FINAL", "CONSUMIDOR_FINAL", "9999999999",
                null, null, "CONSUMIDOR FINAL", null, null, null, false, BigDecimal.ZERO, 0));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("171003406"),
                "Maria Fernanda", "Lopez", null, "maria.lopez@demo.ec", "0991111111",
                "Av. Amazonas 123", true, new BigDecimal("800"), 30));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("171003407"),
                "Carlos Andres", "Perez", null, "carlos.perez@demo.ec", "0992222222",
                "Calle Bolivar 456", true, new BigDecimal("500"), 30));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("010203040"),
                "Ana Belen", "Zambrano", null, "ana.zambrano@demo.ec", "0993333333",
                "Av. 10 de Agosto 789", false, BigDecimal.ZERO, 0));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("092345678"),
                "Jorge Luis", "Mendoza", null, "jorge.mendoza@demo.ec", "0994444444",
                "Calle Sucre 321", false, BigDecimal.ZERO, 0));
        clientes.add(cliente(c, empresaId, "EMPRESA", "RUC", ruc("179001234"),
                null, null, "Distribuidora Andina S.A.", "compras@andina.ec", "022555555",
                "Av. Naciones Unidas 100", true, new BigDecimal("2500"), 45));
        clientes.add(cliente(c, empresaId, "EMPRESA", "RUC", ruc("179005678"),
                null, null, "Comercial El Sol Cia. Ltda.", "pagos@elsol.ec", "022666666",
                "Av. Eloy Alfaro 250", true, new BigDecimal("1500"), 30));
        clientes.add(cliente(c, empresaId, "EMPRESA", "RUC", ruc("019012345"),
                null, null, "Hotel Sierra Azul", "compras@sierraazul.ec", "072777777",
                "Calle Larga 10-20, Cuenca", false, BigDecimal.ZERO, 0));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("110234567"),
                "Sofia", "Cordero", null, "sofia.cordero@demo.ec", "0995555555",
                "Av. Universitaria 50", false, BigDecimal.ZERO, 0));
        clientes.add(cliente(c, empresaId, "PERSONA_NATURAL", "CEDULA", cedula("060345678"),
                "Pedro", "Chimbo", null, "pedro.chimbo@demo.ec", "0996666666",
                "Barrio Centro 12", false, BigDecimal.ZERO, 0));
        for (ClienteCreado cl : clientes) {
            actualizar(c, "INSERT INTO cliente_puntos (cliente_id, saldo_puntos) VALUES (?, 0) "
                    + "ON CONFLICT (cliente_id) DO NOTHING", cl.id());
        }
        return clientes;
    }

    private static ClienteCreado cliente(Connection c, long empresaId, String tipo, String tipoId, String numero,
                                         String nombres, String apellidos, String razon, String email, String telefono,
                                         String direccion, boolean credito, BigDecimal limite, int dias)
            throws SQLException {
        long id = insertar(c, "INSERT INTO cliente (empresa_id, tipo, tipo_identificacion, numero_identificacion, "
                + "nombres, apellidos, razon_social, email, telefono, direccion, es_credito, limite_credito, "
                + "dias_credito, saldo, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, TRUE) "
                + "ON CONFLICT (empresa_id, numero_identificacion) DO UPDATE SET nombres = EXCLUDED.nombres, "
                + "apellidos = EXCLUDED.apellidos, razon_social = EXCLUDED.razon_social, email = EXCLUDED.email, "
                + "telefono = EXCLUDED.telefono, direccion = EXCLUDED.direccion, es_credito = EXCLUDED.es_credito, "
                + "limite_credito = EXCLUDED.limite_credito, dias_credito = EXCLUDED.dias_credito, activo = TRUE "
                + "RETURNING id", empresaId, tipo, tipoId, numero, nombres, apellidos, razon, email, telefono,
                direccion, credito, limite, dias);
        return new ClienteCreado(id, tipoId, numero, razon == null
                ? (nombres + " " + apellidos).trim() : razon, email, credito, limite);
    }

    private static List<ProveedorCreado> crearProveedores(Connection c, long empresaId) throws SQLException {
        List<ProveedorCreado> proveedores = new ArrayList<>();
        proveedores.add(proveedor(c, empresaId, "Distribuidora La Favorita S.A.", ruc("179003456")));
        proveedores.add(proveedor(c, empresaId, "Pronaca S.A.", ruc("179004567")));
        proveedores.add(proveedor(c, empresaId, "Nestle Ecuador S.A.", ruc("179005678")));
        proveedores.add(proveedor(c, empresaId, "Tonicorp S.A.", ruc("179006789")));
        proveedores.add(proveedor(c, empresaId, "P&G Ecuador Cia. Ltda.", ruc("179007890")));
        proveedores.add(proveedor(c, empresaId, "Coca-Cola Ecuador S.A.", ruc("179008901")));
        return proveedores;
    }

    private static ProveedorCreado proveedor(Connection c, long empresaId, String razon, String numero)
            throws SQLException {
        long id = insertar(c, "INSERT INTO proveedor (empresa_id, tipo_identificacion, numero_identificacion, "
                + "razon_social, nombre_comercial, direccion, telefono, email, tipo_contribuyente, dias_credito, "
                + "aplica_retencion_iva, aplica_retencion_renta, activo) "
                + "VALUES (?, 'RUC', ?, ?, ?, ?, ?, ?, 'CONTRIBUYENTE_GENERAL', 30, TRUE, TRUE, TRUE) "
                + "ON CONFLICT (empresa_id, numero_identificacion) DO UPDATE SET razon_social = EXCLUDED.razon_social, "
                + "email = EXCLUDED.email, activo = TRUE RETURNING id",
                empresaId, numero, razon, razon, "Av. Industrial 100", "022888888",
                razon.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").substring(0, 6) + "@proveedor.ec");
        return new ProveedorCreado(id, razon);
    }

    // ------------------------------------------------------------------ fidelidad, promociones y dispositivos

    private static void crearFidelidad(Connection c, long empresaId, List<ClienteCreado> clientes)
            throws SQLException {
        long programa = buscarId(c, "SELECT id FROM programa_fidelidad WHERE empresa_id = ? AND nombre = 'Puntos Demo'",
                empresaId);
        if (programa == 0) {
            insertar(c, "INSERT INTO programa_fidelidad (empresa_id, nombre, puntos_por_monto, valor_punto, "
                    + "vigencia_puntos_dias) VALUES (?, 'Puntos Demo', 1, 0.01, 365) RETURNING id", empresaId);
        }
        for (ClienteCreado cl : clientes) {
            actualizar(c, "INSERT INTO cliente_puntos (cliente_id, saldo_puntos) VALUES (?, 0) "
                    + "ON CONFLICT (cliente_id) DO NOTHING", cl.id());
        }
    }

    private static void crearPromociones(Connection c, long empresaId, Map<String, Long> categorias,
                                         Map<String, Long> marcas, List<ProductoCreado> productos) throws SQLException {
        long abarrotes = categorias.get("ABARROTES");
        long gaseosas = categorias.get("GASEOSAS");
        long snacks = categorias.get("SNACKS");
        long nestle = marcas.get("Nestle");
        long promoAbarrotes = promocion(c, empresaId, "10% en abarrotes", "PORCENTAJE", "10", null, null, 10,
                "CATEGORIA", abarrotes);
        promocion(c, empresaId, "2x1 en gaseosas", "NXM", "0", 2, 1, 20, "CATEGORIA", gaseosas);
        promocion(c, empresaId, "Snacks a precio fijo $1.50", "PRECIO_FIJO", "1.50", null, null, 5,
                "CATEGORIA", snacks);
        promocion(c, empresaId, "5% en productos Nestle", "PORCENTAJE", "5", null, null, 8,
                "MARCA", nestle);
        if (promoAbarrotes > 0 && !productos.isEmpty()) {
            ProductoCreado arroz = productos.get(0);
            actualizar(c, "INSERT INTO promocion_alcance (promocion_id, tipo_alcance, referencia_id) "
                    + "SELECT ?, 'PRODUCTO', ? WHERE NOT EXISTS (SELECT 1 FROM promocion_alcance "
                    + "WHERE promocion_id = ? AND tipo_alcance = 'PRODUCTO' AND referencia_id = ?)",
                    promoAbarrotes, arroz.id(), promoAbarrotes, arroz.id());
        }
    }

    private static long promocion(Connection c, long empresaId, String nombre, String tipo, String valor,
                                  Integer requerida, Integer pagada, int prioridad, String alcance, long ref)
            throws SQLException {
        long id = buscarId(c, "SELECT id FROM promocion WHERE empresa_id = ? AND nombre = ?", empresaId, nombre);
        if (id == 0) {
            id = insertar(c, "INSERT INTO promocion (empresa_id, nombre, tipo, valor, cantidad_requerida, "
                    + "cantidad_pagada, prioridad, vigente_desde, vigente_hasta, estado, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE - 30, CURRENT_DATE + 90, 'ACTIVA', TRUE) "
                    + "RETURNING id", empresaId, nombre, tipo, new BigDecimal(valor), requerida, pagada, prioridad);
        }
        actualizar(c, "INSERT INTO promocion_alcance (promocion_id, tipo_alcance, referencia_id) "
                + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM promocion_alcance WHERE promocion_id = ? "
                + "AND tipo_alcance = ? AND referencia_id = ?)",
                id, alcance, ref, id, alcance, ref);
        return id;
    }

    private static void crearCupones(Connection c, long empresaId) throws SQLException {
        cupon(c, empresaId, "BIENVENIDO10", "PORCENTAJE", "10", 100);
        cupon(c, empresaId, "AHORRA5", "MONTO", "5", 50);
        cupon(c, empresaId, "DEMO2026", "PORCENTAJE", "5", 200);
    }

    private static void cupon(Connection c, long empresaId, String codigo, String tipo, String valor, int usos)
            throws SQLException {
        actualizar(c, "INSERT INTO cupon (empresa_id, codigo, tipo, valor, vigente_desde, vigente_hasta, "
                + "usos_maximos, usos_actuales, activo) VALUES (?, ?, ?, ?, CURRENT_DATE - 30, CURRENT_DATE + 90, "
                + "?, 0, TRUE) ON CONFLICT (codigo) DO UPDATE SET activo = TRUE, usos_maximos = EXCLUDED.usos_maximos",
                empresaId, codigo, tipo, new BigDecimal(valor), usos);
    }

    private static void crearDispositivos(Connection c, long empresaId, long matriz, long norte,
                                          long caja1, long caja2) throws SQLException {
        dispositivo(c, empresaId, matriz, caja1, "IMPRESORA_TICKET", "Impresora ticketera 80mm Caja 1",
                "{\"ancho_mm\":80,\"caracteres\":48,\"cortar\":true}");
        dispositivo(c, empresaId, norte, caja2, "IMPRESORA_TICKET", "Impresora ticketera 80mm Caja Norte",
                "{\"ancho_mm\":80,\"caracteres\":48,\"cortar\":true}");
        dispositivo(c, empresaId, matriz, null, "IMPRESORA_A4", "Impresora A4 administracion",
                "{\"tamano\":\"A4\",\"orientacion\":\"PORTRAIT\"}");
        dispositivo(c, empresaId, matriz, caja1, "LECTOR_BARRAS", "Lector codigo de barras HID",
                "{\"modo\":\"HID\",\"sufijo\":\"ENTER\"}");
        dispositivo(c, empresaId, matriz, null, "BALANZA", "Balanza de mostrador",
                "{\"puerto\":\"/dev/ttyUSB0\",\"baudios\":9600,\"formato\":\"PLU_PESO\"}");
        dispositivo(c, empresaId, matriz, caja1, "GAVETA", "Gaveta de efectivo Caja 1",
                "{\"pin\":0,\"abrir_al_cobrar\":true}");
    }

    private static void dispositivo(Connection c, long empresaId, Long sucursal, Long caja, String tipo,
                                    String nombre, String json) throws SQLException {
        actualizar(c, "INSERT INTO dispositivo (empresa_id, sucursal_id, caja_id, tipo, nombre, configuracion) "
                + "SELECT ?, ?, ?, ?, ?, CAST(? AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM dispositivo "
                + "WHERE empresa_id = ? AND nombre = ?)", empresaId, sucursal, caja, tipo, nombre, json,
                empresaId, nombre);
    }

    // ------------------------------------------------------------------ ventas

    private static void generarVentas(Connection c, long empresaId, long matriz, long norte, long caja1, long caja2,
                                      long sesion1, long sesion2, long bodegaMatriz, long bodegaNorte,
                                      List<ProductoCreado> productos, List<ClienteCreado> clientes,
                                      long cajero1, long cajero2) throws SQLException {
        long efectivo = medioPago(c, "EFECTIVO");
        long debito = medioPago(c, "DEBITO");
        long transferencia = medioPago(c, "TRANSF");
        long credito = medioPago(c, "CREDITO");
        long tipoVenta = buscarId(c, "SELECT id FROM tipo_movimiento_inventario WHERE codigo = 'VENTA'");
        int secuencialFactura = siguienteSecuencialFactura(c, empresaId);
        List<Long> ventasCredito = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            boolean enNorte = i % 4 == 0;
            long sucursal = enNorte ? norte : matriz;
            long caja = enNorte ? caja2 : caja1;
            long sesion = enNorte ? sesion2 : sesion1;
            long bodega = enNorte ? bodegaNorte : bodegaMatriz;
            long cajero = enNorte ? cajero2 : cajero1;
            LocalDate dia = LocalDate.now().minusDays(RND.nextInt(30));
            LocalDateTime fecha = dia.atTime(8 + RND.nextInt(12), RND.nextInt(60));

            Set<Integer> elegidos = new HashSet<>();
            List<LineaVenta> lineas = new ArrayList<>();
            int cantidadLineas = 1 + RND.nextInt(4);
            while (elegidos.size() < cantidadLineas) {
                elegidos.add(RND.nextInt(productos.size()));
            }
            for (int indice : elegidos) {
                ProductoCreado p = productos.get(indice);
                BigDecimal cantidad = BigDecimal.valueOf(1 + RND.nextInt(3));
                BigDecimal subtotal = p.precio().multiply(cantidad).setScale(4, RoundingMode.HALF_UP);
                BigDecimal impuesto = subtotal.multiply(p.ivaPorcentaje())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                lineas.add(new LineaVenta(p, cantidad, p.precio(), p.ivaPorcentaje(), subtotal, impuesto,
                        subtotal.add(impuesto), p.costo().multiply(cantidad).setScale(4, RoundingMode.HALF_UP)));
            }
            BigDecimal subtotal = lineas.stream().map(LineaVenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal impuesto = lineas.stream().map(LineaVenta::impuesto).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = subtotal.add(impuesto);
            BigDecimal costo = lineas.stream().map(LineaVenta::costo).reduce(BigDecimal.ZERO, BigDecimal::add);

            int indiceCliente = RND.nextInt(100) < 30 ? 0 : 1 + RND.nextInt(clientes.size() - 1);
            ClienteCreado cliente = clientes.get(indiceCliente);
            boolean aCredito = cliente.credito() && RND.nextInt(100) < 22;
            List<BigDecimal> pagos = new ArrayList<>();
            List<Long> medios = new ArrayList<>();
            if (aCredito) {
                medios.add(credito);
                pagos.add(total);
            } else if (i % 11 == 0 && i > 0) {
                BigDecimal parte = total.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
                medios.add(efectivo);
                pagos.add(parte);
                medios.add(debito);
                pagos.add(total.subtract(parte));
            } else {
                int selector = RND.nextInt(10);
                medios.add(selector < 6 ? efectivo : selector < 8 ? debito : transferencia);
                pagos.add(total);
            }

            long ventaId = insertar(c, "INSERT INTO venta (empresa_id, sucursal_id, caja_id, sesion_caja_id, numero, "
                    + "cliente_id, usuario_id, tipo, estado, fecha, subtotal, descuento_total, impuesto_total, "
                    + "propina, total, costo_total, utilidad) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, 'VENTA', 'PAGADA', ?, ?, 0, ?, 0, ?, ?, ?) RETURNING id",
                    empresaId, sucursal, caja, sesion, "V-" + (10000 + i), indiceCliente == 0 ? null : cliente.id(),
                    cajero, fecha, subtotal, impuesto, total, costo, total.subtract(costo));

            for (LineaVenta l : lineas) {
                insertar(c, "INSERT INTO venta_detalle (venta_id, producto_id, descripcion, cantidad, "
                        + "precio_unitario, descuento, impuesto_id, impuesto_valor, subtotal, total, costo_unitario) "
                        + "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?) RETURNING id",
                        ventaId, l.producto().id(), l.producto().nombre(), l.cantidad(), l.precio(),
                        l.producto().impuestoId(), l.impuesto(), l.subtotal(), l.total(),
                        l.costo().divide(l.cantidad(), 4, RoundingMode.HALF_UP));
                descontarStock(c, empresaId, sucursal, bodega, l.producto().id(), l.cantidad(), l.costo(),
                        tipoVenta, ventaId, cajero);
            }
            for (int j = 0; j < medios.size(); j++) {
                insertar(c, "INSERT INTO venta_pago (venta_id, medio_pago_id, monto, cambio, usuario_id) "
                        + "VALUES (?, ?, ?, 0, ?) RETURNING id", ventaId, medios.get(j), pagos.get(j), cajero);
            }
            if (aCredito && indiceCliente > 0) {
                BigDecimal saldo = actualizarSaldoCliente(c, cliente.id(), total);
                insertar(c, "INSERT INTO cxc_movimiento (empresa_id, cliente_id, venta_id, tipo, monto, "
                        + "saldo_resultante, usuario_id, nota, creado_en) "
                        + "VALUES (?, ?, ?, 'CARGO', ?, ?, ?, 'Venta a credito demo', ?) RETURNING id",
                        empresaId, cliente.id(), ventaId, total, saldo, cajero, fecha);
                ventasCredito.add(ventaId);
            }
            if (indiceCliente > 0) {
                acumularPuntos(c, cliente.id(), total, ventaId, fecha);
            }
            if (i < 32) {
                long comprobanteId = crearFactura(c, empresaId, sucursal, ventaId, fecha, secuencialFactura++,
                        cliente, lineas, medios, pagos, subtotal, impuesto, total);
                if (comprobanteId == 0) {
                    secuencialFactura--;
                }
            }
        }
    }

    private static void descontarStock(Connection c, long empresaId, long sucursalId, long bodegaId, long productoId,
                                       BigDecimal cantidad, BigDecimal costo, long tipoVenta, long ventaId,
                                       long usuarioId) throws SQLException {
        long stockId = buscarId(c, "SELECT id FROM stock WHERE bodega_id = ? AND producto_id = ? AND lote_id IS NULL",
                bodegaId, productoId);
        BigDecimal saldo;
        if (stockId == 0) {
            saldo = cantidad.negate();
            insertar(c, "INSERT INTO stock (bodega_id, producto_id, cantidad) VALUES (?, ?, ?) RETURNING id",
                    bodegaId, productoId, saldo);
        } else {
            saldo = actualizarDecimal(c, "UPDATE stock SET cantidad = cantidad - ?, actualizado_en = now() "
                    + "WHERE id = ? RETURNING cantidad", cantidad, stockId);
        }
        movimiento(c, empresaId, sucursalId, bodegaId, productoId, null, tipoVenta, cantidad.negate(), costo,
                saldo, saldo.multiply(costo), "VENTA", ventaId, "Venta demo", usuarioId);
    }

    private static long crearFactura(Connection c, long empresaId, long sucursalId, long ventaId, LocalDateTime fecha,
                                     int secuencial, ClienteCreado cliente, List<LineaVenta> lineas,
                                     List<Long> medios, List<BigDecimal> pagos, BigDecimal subtotal,
                                     BigDecimal impuesto, BigDecimal total) throws SQLException {
        String sec = String.format("%09d", secuencial);
        if (buscarId(c, "SELECT id FROM comprobante WHERE empresa_id = ? AND sucursal_id = ? AND tipo = 'FACTURA' "
                + "AND establecimiento = '001' AND punto_emision = '001' AND secuencial = ?",
                empresaId, sucursalId, sec) > 0) {
            return 0;
        }
        String numero = "001-001-" + sec;
        String clave = ClaveAcceso.generar(fecha.toLocalDate(), "01", RUC, "1", "001", "001", sec, "1");
        String tipoId = CatalogoSri.tipoIdentificacion(cliente.tipoIdentificacion());
        long comprobanteId = insertar(c, "INSERT INTO comprobante (empresa_id, sucursal_id, tipo, establecimiento, "
                + "punto_emision, secuencial, numero_completo, clave_acceso, fecha_emision, ambiente, tipo_emision, "
                + "emisor_ruc, emisor_razon_social, emisor_direccion, receptor_tipo_id, receptor_numero_id, "
                + "receptor_razon_social, receptor_email, subtotal, descuento, iva, propina, total, estado, venta_id) "
                + "VALUES (?, ?, 'FACTURA', '001', '001', ?, ?, ?, ?, 'PRUEBAS', 'NORMAL', ?, "
                + "'Supermercado Demo S.A.', 'Av. Principal y Calle 1, Quito', ?, ?, ?, ?, ?, 0, ?, 0, ?, "
                + "'EMITIDO', ?) RETURNING id",
                empresaId, sucursalId, sec, numero, clave, fecha.toLocalDate(), RUC, tipoId,
                cliente.numeroIdentificacion(), cliente.razonSocial(), cliente.email(), subtotal, impuesto, total,
                ventaId);
        for (LineaVenta l : lineas) {
            long detalleId = insertar(c, "INSERT INTO comprobante_detalle (comprobante_id, codigo_principal, "
                    + "descripcion, cantidad, precio_unitario, descuento, subtotal, impuesto_total, total) "
                    + "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?) RETURNING id",
                    comprobanteId, l.producto().codigo(), l.producto().nombre(), l.cantidad(), l.precio(),
                    l.subtotal(), l.impuesto(), l.total());
            insertar(c, "INSERT INTO comprobante_impuesto (comprobante_id, comprobante_detalle_id, codigo_impuesto, "
                    + "codigo_porcentaje, tarifa, base_imponible, valor) "
                    + "VALUES (?, ?, '2', ?, ?, ?, ?) RETURNING id",
                    comprobanteId, detalleId,
                    CatalogoSri.codigoPorcentajeIva(l.iva()), l.iva(), l.subtotal(), l.impuesto());
        }
        for (int i = 0; i < medios.size(); i++) {
            String forma = buscarTexto(c, "SELECT codigo_sri_forma_pago FROM medio_pago WHERE id = ?", medios.get(i));
            insertar(c, "INSERT INTO comprobante_pago (comprobante_id, forma_pago_codigo, total) "
                    + "VALUES (?, ?, ?) RETURNING id", comprobanteId, forma == null ? "01" : forma, pagos.get(i));
        }
        return comprobanteId;
    }

    private static void acumularPuntos(Connection c, long clienteId, BigDecimal total, long ventaId,
                                       LocalDateTime fecha) throws SQLException {
        BigDecimal puntos = total.setScale(0, RoundingMode.DOWN);
        if (puntos.signum() <= 0) {
            return;
        }
        actualizar(c, "INSERT INTO cliente_puntos (cliente_id, saldo_puntos) VALUES (?, 0) "
                + "ON CONFLICT (cliente_id) DO NOTHING", clienteId);
        BigDecimal saldo = actualizarDecimal(c, "UPDATE cliente_puntos SET saldo_puntos = saldo_puntos + ? "
                + "WHERE cliente_id = ? RETURNING saldo_puntos", puntos, clienteId);
        insertar(c, "INSERT INTO puntos_movimiento (cliente_id, tipo, puntos, referencia_tipo, referencia_id, "
                + "saldo_resultante, fecha, nota) VALUES (?, 'ACUMULA', ?, 'VENTA', ?, ?, ?, 'Acumulacion demo') "
                + "RETURNING id", clienteId, puntos, ventaId, saldo, fecha);
    }

    private static void generarAbonos(Connection c, long empresaId) throws SQLException {
        long efectivo = medioPago(c, "EFECTIVO");
        List<Long> clientes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, saldo FROM cliente WHERE empresa_id = ? "
                + "AND saldo > 0 ORDER BY saldo DESC LIMIT 3")) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal saldo = rs.getBigDecimal("saldo");
                    BigDecimal abono = saldo.multiply(new BigDecimal("0.35")).setScale(2, RoundingMode.HALF_UP);
                    long clienteId = rs.getLong("id");
                    BigDecimal nuevo = actualizarDecimal(c, "UPDATE cliente SET saldo = saldo - ? WHERE id = ? "
                            + "RETURNING saldo", abono, clienteId);
                    insertar(c, "INSERT INTO cxc_movimiento (empresa_id, cliente_id, tipo, monto, saldo_resultante, "
                            + "medio_pago_id, usuario_id, nota, creado_en) "
                            + "VALUES (?, ?, 'ABONO', ?, ?, ?, 1, 'Abono demo', now() - INTERVAL '3 days') RETURNING id",
                            empresaId, clienteId, abono, nuevo, efectivo);
                    clientes.add(clienteId);
                }
            }
        }
    }

    private static BigDecimal actualizarSaldoCliente(Connection c, long clienteId, BigDecimal monto)
            throws SQLException {
        return actualizarDecimal(c, "UPDATE cliente SET saldo = saldo + ? WHERE id = ? RETURNING saldo",
                monto, clienteId);
    }

    // ------------------------------------------------------------------ compras

    private static void generarCompras(Connection c, long empresaId, long matriz, List<ProveedorCreado> proveedores,
                                       List<ProductoCreado> productos, Long contadorId) throws SQLException {
        long efectivo = medioPago(c, "EFECTIVO");
        long transferencia = medioPago(c, "TRANSF");
        for (int i = 0; i < 8; i++) {
            ProveedorCreado prov = proveedores.get(i % proveedores.size());
            ProductoCreado p1 = productos.get((i * 3) % productos.size());
            ProductoCreado p2 = productos.get((i * 5 + 1) % productos.size());
            BigDecimal c1 = BigDecimal.valueOf(3 + i);
            BigDecimal c2 = BigDecimal.valueOf(2 + (i % 4));
            BigDecimal sub1 = p1.costo().multiply(c1);
            BigDecimal sub2 = p2.costo().multiply(c2);
            BigDecimal subtotal = sub1.add(sub2);
            BigDecimal iva1 = sub1.multiply(p1.ivaPorcentaje()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal iva2 = sub2.multiply(p2.ivaPorcentaje()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal iva = iva1.add(iva2);
            BigDecimal total = subtotal.add(iva);
            LocalDate emision = LocalDate.now().minusDays(5 + i * 3);
            long factura = insertar(c, "INSERT INTO factura_proveedor (empresa_id, proveedor_id, tipo_documento, "
                    + "numero, fecha_emision, fecha_vencimiento, subtotal, descuento, iva, total, saldo, estado_pago, "
                    + "usuario_id) VALUES (?, ?, 'FACTURA', ?, ?, ?, ?, 0, ?, ?, ?, 'PENDIENTE', ?) RETURNING id",
                    empresaId, prov.id(), "FP-" + (100 + i), emision, emision.plusDays(30), subtotal, iva, total,
                    total, contadorId);
            insertar(c, "INSERT INTO factura_proveedor_detalle (factura_proveedor_id, producto_id, descripcion, "
                    + "cantidad, precio_unitario, impuesto_id, total) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    factura, p1.id(), p1.nombre(), c1, p1.costo(), p1.impuestoId(), sub1.add(iva1));
            insertar(c, "INSERT INTO factura_proveedor_detalle (factura_proveedor_id, producto_id, descripcion, "
                    + "cantidad, precio_unitario, impuesto_id, total) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    factura, p2.id(), p2.nombre(), c2, p2.costo(), p2.impuestoId(), sub2.add(iva2));
            if (i % 2 == 0) {
                BigDecimal pago = i % 4 == 0 ? total : total.multiply(new BigDecimal("0.5"))
                        .setScale(2, RoundingMode.HALF_UP);
                long pagoId = insertar(c, "INSERT INTO pago_proveedor (empresa_id, proveedor_id, numero, fecha, "
                        + "total, medio_pago_id, referencia, usuario_id, nota) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'PAGO-DEMO', ?, 'Pago demo') RETURNING id",
                        empresaId, prov.id(), "PP-" + (200 + i), emision.plusDays(5), pago,
                        i % 4 == 0 ? transferencia : efectivo, contadorId);
                insertar(c, "INSERT INTO pago_proveedor_factura (pago_id, factura_proveedor_id, monto) "
                        + "VALUES (?, ?, ?) RETURNING id", pagoId, factura, pago);
                BigDecimal saldo = actualizarDecimal(c, "UPDATE factura_proveedor SET saldo = saldo - ? WHERE id = ? "
                        + "RETURNING saldo", pago, factura);
                actualizar(c, "UPDATE factura_proveedor SET estado_pago = ? WHERE id = ?",
                        saldo.signum() <= 0 ? "PAGADA" : "PARCIAL", factura);
            }
            if (i < 2) {
                long retencion = insertar(c, "INSERT INTO retencion (empresa_id, proveedor_id, factura_proveedor_id, "
                        + "establecimiento, punto_emision, secuencial, fecha_emision, base_iva, valor_iva, "
                        + "valor_retencion_iva, base_renta, valor_retencion_renta, total_retenido, estado) "
                        + "VALUES (?, ?, ?, '001', '001', ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDO') RETURNING id",
                        empresaId, prov.id(), factura, String.format("%09d", 300 + i), emision,
                        subtotal, iva, iva.multiply(new BigDecimal("0.30")).setScale(4, RoundingMode.HALF_UP),
                        subtotal, subtotal.multiply(new BigDecimal("0.01")).setScale(4, RoundingMode.HALF_UP),
                        iva.multiply(new BigDecimal("0.30")).add(subtotal.multiply(new BigDecimal("0.01")))
                                .setScale(4, RoundingMode.HALF_UP));
                insertar(c, "INSERT INTO retencion_detalle (retencion_id, tipo_impuesto, codigo_retencion, "
                        + "base_imponible, porcentaje, valor_retenido) VALUES (?, 'IVA', '725', ?, 30, ?) RETURNING id",
                        retencion, subtotal, iva.multiply(new BigDecimal("0.30")).setScale(4, RoundingMode.HALF_UP));
            }
        }

        // Solicitudes y ordenes de compra de ejemplo
        long solicitud = insertar(c, "INSERT INTO solicitud_compra (empresa_id, sucursal_id, numero, estado, "
                + "usuario_solicita_id, fecha, nota) VALUES (?, ?, 'SC-DEMO-001', 'APROBADA', ?, "
                + "CURRENT_DATE - 10, 'Reposicion de abarrotes') RETURNING id", empresaId, matriz, contadorId);
        insertar(c, "INSERT INTO solicitud_compra_detalle (solicitud_id, producto_id, cantidad, justificacion) "
                + "VALUES (?, ?, 20, 'Stock bajo') RETURNING id", solicitud, productos.get(0).id());
        long orden = insertar(c, "INSERT INTO orden_compra (empresa_id, sucursal_id, proveedor_id, numero, estado, "
                + "fecha_emision, fecha_entrega_esperada, subtotal, descuento, impuestos, total, usuario_id, nota) "
                + "VALUES (?, ?, ?, 'OC-DEMO-001', 'APROBADA', CURRENT_DATE - 5, CURRENT_DATE + 3, 500, 0, 75, 575, "
                + "?, 'Orden demo') RETURNING id", empresaId, matriz, proveedores.get(0).id(), contadorId);
        insertar(c, "INSERT INTO orden_compra_detalle (orden_compra_id, producto_id, descripcion, cantidad, "
                + "cantidad_recibida, precio_unitario, impuesto_id, descuento, subtotal, total) "
                + "VALUES (?, ?, ?, 50, 0, 10, ?, 0, 500, 575) RETURNING id",
                orden, productos.get(0).id(), productos.get(0).nombre(), productos.get(0).impuestoId());
    }

    // ------------------------------------------------------------------ sesiones y reportes

    private static long sesion(Connection c, long empresaId, long cajaId, long usuarioId, LocalDateTime apertura,
                               LocalDateTime cierre, String estado, String inicial) throws SQLException {
        return insertar(c, "INSERT INTO sesion_caja (empresa_id, caja_id, usuario_apertura_id, usuario_cierre_id, "
                + "fecha_apertura, fecha_cierre, monto_inicial, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                empresaId, cajaId, usuarioId, "CERRADA".equals(estado) ? usuarioId : null,
                apertura, cierre, new BigDecimal(inicial), estado);
    }

    private static void actualizarSesiones(Connection c) throws SQLException {
        actualizar(c, "UPDATE sesion_caja s SET "
                + "monto_esperado_efectivo = s.monto_inicial + COALESCE((SELECT SUM(vp.monto - vp.cambio) "
                + "FROM venta v JOIN venta_pago vp ON vp.venta_id = v.id JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                + "WHERE v.sesion_caja_id = s.id AND mp.tipo = 'EFECTIVO'), 0), "
                + "monto_contado_efectivo = s.monto_inicial + COALESCE((SELECT SUM(vp.monto - vp.cambio) "
                + "FROM venta v JOIN venta_pago vp ON vp.venta_id = v.id JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                + "WHERE v.sesion_caja_id = s.id AND mp.tipo = 'EFECTIVO'), 0), diferencia = 0 "
                + "WHERE s.estado = 'CERRADA' AND s.fecha_apertura >= CURRENT_DATE - 60");
    }

    private static void abrirSesionSiFalta(Connection c, long empresaId, long cajaId, long usuarioId, String inicial)
            throws SQLException {
        long abierta = buscarId(c, "SELECT id FROM sesion_caja WHERE caja_id = ? AND estado = 'ABIERTA'", cajaId);
        if (abierta == 0) {
            insertar(c, "INSERT INTO sesion_caja (empresa_id, caja_id, usuario_apertura_id, fecha_apertura, "
                    + "monto_inicial, estado) VALUES (?, ?, ?, now(), ?, 'ABIERTA') RETURNING id",
                    empresaId, cajaId, usuarioId, new BigDecimal(inicial));
        }
    }

    private static void reportesGuardados(Connection c, long empresaId, long usuarioId) throws SQLException {
        guardarReporte(c, empresaId, usuarioId, "Ventas del mes", "ventas_por_dia", 30);
        guardarReporte(c, empresaId, usuarioId, "Stock bajo", "stock_bajo", 30);
        guardarReporte(c, empresaId, usuarioId, "CxC del periodo", "cuentas_por_cobrar", 60);
    }

    private static void guardarReporte(Connection c, long empresaId, long usuarioId, String nombre, String codigo,
                                       int dias) throws SQLException {
        actualizar(c, "INSERT INTO reporte_guardado (empresa_id, usuario_id, codigo, nombre, desde, hasta) "
                + "SELECT ?, ?, ?, ?, CURRENT_DATE - ?, CURRENT_DATE "
                + "WHERE NOT EXISTS (SELECT 1 FROM reporte_guardado WHERE empresa_id = ? AND usuario_id = ? "
                + "AND nombre = ?)", empresaId, usuarioId, codigo, nombre, dias, empresaId, usuarioId, nombre);
    }

    // ------------------------------------------------------------------ helpers

    private static long medioPago(Connection c, String codigo) throws SQLException {
        return buscarId(c, "SELECT id FROM medio_pago WHERE codigo = ?", codigo);
    }

    private static void actualizarSecuencias(Connection c, long empresaId, long sucursalId) throws SQLException {
        for (String tipo : List.of("FACTURA", "NOTA_CREDITO", "NOTA_DEBITO", "RETENCION",
                "GUIA_REMISION", "LIQUIDACION_COMPRA")) {
            long max = buscarId(c, "SELECT COALESCE(MAX(secuencial::int), 0) FROM comprobante "
                    + "WHERE empresa_id = ? AND sucursal_id = ? AND tipo = ? "
                    + "AND establecimiento = '001' AND punto_emision = '001'", empresaId, sucursalId, tipo);
            actualizar(c, "INSERT INTO secuencia_documento (empresa_id, sucursal_id, tipo_comprobante, "
                    + "establecimiento, punto_emision, secuencial_actual) VALUES (?, ?, ?, '001', '001', ?) "
                    + "ON CONFLICT (empresa_id, sucursal_id, tipo_comprobante, establecimiento, punto_emision) "
                    + "DO UPDATE SET secuencial_actual = GREATEST(secuencia_documento.secuencial_actual, "
                    + "EXCLUDED.secuencial_actual)", empresaId, sucursalId, tipo, max);
        }
    }

    private static int siguienteSecuencialFactura(Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(secuencial::int), 0) + 1 "
                + "FROM comprobante WHERE empresa_id = ? AND tipo = 'FACTURA' AND establecimiento = '001' "
                + "AND punto_emision = '001'")) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String ean13(int numero) {
        String base = String.format("7861000%05d", numero);
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(base.charAt(i));
            suma += i % 2 == 0 ? digito : digito * 3;
        }
        int check = (10 - suma % 10) % 10;
        return base + check;
    }

    private static String cedula(String primeros9) {
        int[] pesos = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int producto = Character.getNumericValue(primeros9.charAt(i)) * pesos[i];
            suma += producto > 9 ? producto - 9 : producto;
        }
        int residuo = suma % 10;
        return primeros9 + (residuo == 0 ? 0 : 10 - residuo);
    }

    private static String ruc(String primeros9) {
        return cedula(primeros9) + "001";
    }

    private static void movimiento(Connection c, long empresaId, long sucursalId, long bodegaId, long productoId,
                                   Long loteId, long tipoId, BigDecimal cantidad, BigDecimal costo,
                                   BigDecimal saldoCantidad, BigDecimal saldoCosto, String referenciaTipo,
                                   Long referenciaId, String nota) throws SQLException {
        movimiento(c, empresaId, sucursalId, bodegaId, productoId, loteId, tipoId, cantidad, costo, saldoCantidad,
                saldoCosto, referenciaTipo, referenciaId, nota, null);
    }

    private static void movimiento(Connection c, long empresaId, long sucursalId, long bodegaId, long productoId,
                                   Long loteId, long tipoId, BigDecimal cantidad, BigDecimal costo,
                                   BigDecimal saldoCantidad, BigDecimal saldoCosto, String referenciaTipo,
                                   Long referenciaId, String nota, Long usuarioId) throws SQLException {
        insertar(c, "INSERT INTO movimiento_inventario (empresa_id, sucursal_id, bodega_id, producto_id, lote_id, "
                + "tipo_movimiento_id, cantidad, costo_unitario, saldo_cantidad, saldo_costo, referencia_tipo, "
                + "referencia_id, usuario_id, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                empresaId, sucursalId, bodegaId, productoId, loteId, tipoId, cantidad, costo, saldoCantidad,
                saldoCosto, referenciaTipo, referenciaId, usuarioId, nota);
    }

    private static void imprimirResumen(int productos, int clientes, int proveedores) {
        System.out.println("Semilla demo aplicada: " + productos + " productos, " + clientes + " clientes, "
                + proveedores + " proveedores, ventas y compras de ejemplo.");
        System.out.println("Credenciales de prueba:");
        for (Credencial credencial : CREDENCIALES) {
            System.out.printf("  %-10s / %-14s  PIN %s  (%s)%n", credencial.usuario(), credencial.password(),
                    credencial.pin(), credencial.rol());
        }
    }

    private static long insertar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void actualizar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, parametros);
            ps.executeUpdate();
        }
    }

    private static long buscarId(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private static String buscarTexto(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static BigDecimal actualizarDecimal(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            Object valor = parametros[i];
            if (valor instanceof LocalDateTime fecha) {
                ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(fecha));
            } else if (valor instanceof LocalDate fecha) {
                ps.setDate(i + 1, java.sql.Date.valueOf(fecha));
            } else {
                ps.setObject(i + 1, valor);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ec.sfjfx.core.config.AppConfig.cargar();
        boolean sembrado = ejecutar();
        System.out.println(sembrado
                ? "Datos de demostracion creados."
                : "La semilla demo ya estaba aplicada; no se hizo nada.");
    }
}
