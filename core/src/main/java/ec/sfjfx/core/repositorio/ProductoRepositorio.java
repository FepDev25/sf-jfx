package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.CodigoBarra;
import ec.sfjfx.core.modelo.ComponenteKit;
import ec.sfjfx.core.modelo.EmpaqueProducto;
import ec.sfjfx.core.modelo.ImagenProducto;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoCodigoBarra;
import ec.sfjfx.core.modelo.TipoProducto;

public final class ProductoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, codigo, nombre, descripcion, categoria_id, marca_id, unidad_medida_id, tipo, "
            + "producto_padre_id, costo, costo_promedio, controla_lote, controla_serie, es_granel, codigo_plu, "
            + "peso_neto, volumen, ubicacion, imagen_principal_id, activo";

    public List<Producto> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM producto WHERE empresa_id = ? "
                    + "AND (? IS NULL OR nombre ILIKE ? OR codigo ILIKE ?) ORDER BY nombre LIMIT 500";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = (filtro == null || filtro.isBlank()) ? null : "%" + filtro.trim() + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Producto> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Producto> porId(long id) {
        return Bd.consulta(c -> porId(c, id));
    }

    public boolean codigoExiste(long empresaId, String codigo, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM producto WHERE empresa_id = ? AND lower(codigo) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, codigo);
                if (excluirId == null) {
                    ps.setNull(3, Types.BIGINT);
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(3, excluirId);
                    ps.setLong(4, excluirId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public boolean codigoBarraExiste(String codigo, Long excluirProductoId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM producto_codigo_barra WHERE codigo = ? AND (? IS NULL OR producto_id <> ?)")) {
                ps.setString(1, codigo);
                if (excluirProductoId == null) {
                    ps.setNull(2, Types.BIGINT);
                    ps.setNull(3, Types.BIGINT);
                } else {
                    ps.setLong(2, excluirProductoId);
                    ps.setLong(3, excluirProductoId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Mapa producto -> impuesto IVA (para el POS). */
    public java.util.Map<Long, Long> impuestoIvaPorProducto(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT pi.producto_id, pi.impuesto_id FROM producto_impuesto pi "
                    + "JOIN impuesto i ON i.id = pi.impuesto_id JOIN producto p ON p.id = pi.producto_id "
                    + "WHERE p.empresa_id = ? AND i.tipo = 'IVA' AND i.activo = TRUE "
                    + "ORDER BY i.porcentaje DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    java.util.Map<Long, Long> mapa = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        mapa.putIfAbsent(rs.getLong("producto_id"), rs.getLong("impuesto_id"));
                    }
                    return mapa;
                }
            }
        });
    }

    // ------------------------------------------------------------------ lectura agregada

    public ProductoCompleto cargarCompleto(long id) {
        return Bd.consulta(c -> cargarCompleto(c, id));
    }

    private ProductoCompleto cargarCompleto(Connection c, long id) throws SQLException {
        Producto p = porId(c, id).orElseThrow();
        List<CodigoBarra> codigos = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, producto_id, codigo, tipo, es_principal, empaque_id "
                + "FROM producto_codigo_barra WHERE producto_id = ? ORDER BY es_principal DESC, codigo")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codigos.add(new CodigoBarra(rs.getLong("id"), rs.getLong("producto_id"),
                            rs.getString("codigo"), TipoCodigoBarra.valueOf(rs.getString("tipo")),
                            rs.getBoolean("es_principal"), (Long) rs.getObject("empaque_id")));
                }
            }
        }
        List<EmpaqueProducto> empaques = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, producto_id, nombre, unidad_medida_id, factor, es_compra, es_venta, "
                + "codigo_barra_id, activo FROM producto_empaque WHERE producto_id = ? ORDER BY nombre")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    empaques.add(new EmpaqueProducto(rs.getLong("id"), rs.getLong("producto_id"),
                            rs.getString("nombre"), (Long) rs.getObject("unidad_medida_id"),
                            rs.getBigDecimal("factor"), rs.getBoolean("es_compra"), rs.getBoolean("es_venta"),
                            (Long) rs.getObject("codigo_barra_id"), rs.getBoolean("activo")));
                }
            }
        }
        Set<Long> impuestos = new LinkedHashSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT impuesto_id FROM producto_impuesto WHERE producto_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    impuestos.add(rs.getLong(1));
                }
            }
        }
        List<ImagenProducto> imagenes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, producto_id, ruta, nombre_archivo, orden, es_principal "
                + "FROM producto_imagen WHERE producto_id = ? ORDER BY orden, id")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    imagenes.add(new ImagenProducto(rs.getLong("id"), rs.getLong("producto_id"),
                            rs.getString("ruta"), rs.getString("nombre_archivo"), rs.getInt("orden"),
                            rs.getBoolean("es_principal")));
                }
            }
        }
        Map<Long, String> atributos = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT atributo_definicion_id, valor FROM producto_atributo WHERE producto_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    atributos.put(rs.getLong(1), rs.getString(2));
                }
            }
        }
        List<PrecioProducto> precios = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT pp.id, pp.lista_precio_id, lp.nombre AS lista, pp.producto_id, pp.precio, "
                + "pp.sucursal_id, pp.vigente_desde, pp.vigente_hasta "
                + "FROM precio_producto pp JOIN lista_precio lp ON lp.id = pp.lista_precio_id "
                + "WHERE pp.producto_id = ? ORDER BY lp.nombre")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    precios.add(new PrecioProducto(rs.getLong("id"), rs.getLong("lista_precio_id"),
                            rs.getString("lista"), rs.getLong("producto_id"), rs.getBigDecimal("precio"),
                            (Long) rs.getObject("sucursal_id"),
                            rs.getObject("vigente_desde", java.time.LocalDate.class),
                            rs.getObject("vigente_hasta", java.time.LocalDate.class)));
                }
            }
        }
        List<ComponenteKit> componentes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT pc.id, pc.producto_kit_id, pc.producto_componente_id, p.nombre AS nombre, pc.cantidad "
                + "FROM producto_componente pc JOIN producto p ON p.id = pc.producto_componente_id "
                + "WHERE pc.producto_kit_id = ? ORDER BY p.nombre")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    componentes.add(new ComponenteKit(rs.getLong("id"), rs.getLong("producto_kit_id"),
                            rs.getLong("producto_componente_id"), rs.getString("nombre"),
                            rs.getBigDecimal("cantidad")));
                }
            }
        }
        return new ProductoCompleto(p, codigos, empaques, impuestos, imagenes, atributos, precios, componentes);
    }

    // ------------------------------------------------------------------ escritura

    public long insertar(Connection c, Producto p) throws SQLException {
        String sql = "INSERT INTO producto (empresa_id, codigo, nombre, descripcion, categoria_id, marca_id, "
                + "unidad_medida_id, tipo, producto_padre_id, costo, costo_promedio, controla_lote, controla_serie, "
                + "es_granel, codigo_plu, peso_neto, volumen, ubicacion, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignarProducto(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, Producto p) throws SQLException {
        String sql = "UPDATE producto SET codigo = ?, nombre = ?, descripcion = ?, categoria_id = ?, marca_id = ?, "
                + "unidad_medida_id = ?, tipo = ?, producto_padre_id = ?, costo = ?, costo_promedio = ?, "
                + "controla_lote = ?, controla_serie = ?, es_granel = ?, codigo_plu = ?, peso_neto = ?, "
                + "volumen = ?, ubicacion = ?, activo = ?, actualizado_en = now() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.nombre());
            ps.setString(3, p.descripcion());
            ps.setObject(4, p.categoriaId());
            ps.setObject(5, p.marcaId());
            ps.setObject(6, p.unidadMedidaId());
            ps.setString(7, p.tipo().name());
            ps.setObject(8, p.productoPadreId());
            ps.setBigDecimal(9, nz(p.costo()));
            ps.setBigDecimal(10, nz(p.costoPromedio()));
            ps.setBoolean(11, p.controlaLote());
            ps.setBoolean(12, p.controlaSerie());
            ps.setBoolean(13, p.esGranel());
            ps.setString(14, p.codigoPlu());
            ps.setBigDecimal(15, p.pesoNeto());
            ps.setBigDecimal(16, p.volumen());
            ps.setString(17, p.ubicacion());
            ps.setBoolean(18, p.activo());
            ps.setLong(19, p.id());
            ps.executeUpdate();
        }
    }

    private void asignarProducto(PreparedStatement ps, Producto p) throws SQLException {
        ps.setLong(1, p.empresaId());
        ps.setString(2, p.codigo());
        ps.setString(3, p.nombre());
        ps.setString(4, p.descripcion());
        ps.setObject(5, p.categoriaId());
        ps.setObject(6, p.marcaId());
        ps.setObject(7, p.unidadMedidaId());
        ps.setString(8, p.tipo().name());
        ps.setObject(9, p.productoPadreId());
        ps.setBigDecimal(10, nz(p.costo()));
        ps.setBigDecimal(11, nz(p.costoPromedio()));
        ps.setBoolean(12, p.controlaLote());
        ps.setBoolean(13, p.controlaSerie());
        ps.setBoolean(14, p.esGranel());
        ps.setString(15, p.codigoPlu());
        ps.setBigDecimal(16, p.pesoNeto());
        ps.setBigDecimal(17, p.volumen());
        ps.setString(18, p.ubicacion());
        ps.setBoolean(19, p.activo());
    }

    /** Reemplaza los hijos del producto (codigos, empaques, impuestos, imagenes, atributos, precios, componentes). */
    public void guardarHijos(Connection c, long productoId, ProductoCompleto completo) throws SQLException {
        // limpieza (rompe el ciclo codigo<->empaque y la FK de imagen principal)
        ejecutar(c, "UPDATE producto SET imagen_principal_id = NULL WHERE id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_impuesto WHERE producto_id = ?", productoId);
        ejecutar(c, "DELETE FROM precio_producto WHERE producto_id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_atributo WHERE producto_id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_componente WHERE producto_kit_id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_imagen WHERE producto_id = ?", productoId);
        ejecutar(c, "UPDATE producto_codigo_barra SET empaque_id = NULL WHERE producto_id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_empaque WHERE producto_id = ?", productoId);
        ejecutar(c, "DELETE FROM producto_codigo_barra WHERE producto_id = ?", productoId);

        // codigos de barra
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_codigo_barra (producto_id, codigo, tipo, es_principal) VALUES (?, ?, ?, ?)")) {
            for (CodigoBarra cb : completo.codigos()) {
                if (cb.codigo() == null || cb.codigo().isBlank()) {
                    continue;
                }
                ps.setLong(1, productoId);
                ps.setString(2, cb.codigo().trim());
                ps.setString(3, cb.tipo().name());
                ps.setBoolean(4, cb.esPrincipal());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // empaques
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_empaque (producto_id, nombre, unidad_medida_id, factor, es_compra, es_venta, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (EmpaqueProducto e : completo.empaques()) {
                ps.setLong(1, productoId);
                ps.setString(2, e.nombre());
                ps.setObject(3, e.unidadMedidaId());
                ps.setBigDecimal(4, nz(e.factor()));
                ps.setBoolean(5, e.esCompra());
                ps.setBoolean(6, e.esVenta());
                ps.setBoolean(7, e.activo());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // impuestos
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_impuesto (producto_id, impuesto_id) VALUES (?, ?)")) {
            for (Long impId : completo.impuestoIds()) {
                ps.setLong(1, productoId);
                ps.setLong(2, impId);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // imagenes + principal
        Long principalId = null;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_imagen (producto_id, ruta, nombre_archivo, orden, es_principal) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            int orden = 0;
            for (ImagenProducto img : completo.imagenes()) {
                ps.setLong(1, productoId);
                ps.setString(2, img.ruta());
                ps.setString(3, img.nombreArchivo());
                ps.setInt(4, orden++);
                ps.setBoolean(5, img.esPrincipal());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (img.esPrincipal() && principalId == null) {
                        principalId = rs.getLong(1);
                    }
                }
            }
        }
        if (principalId != null) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE producto SET imagen_principal_id = ? WHERE id = ?")) {
                ps.setLong(1, principalId);
                ps.setLong(2, productoId);
                ps.executeUpdate();
            }
        }

        // atributos
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_atributo (producto_id, atributo_definicion_id, valor) VALUES (?, ?, ?)")) {
            for (Map.Entry<Long, String> e : completo.atributos().entrySet()) {
                if (e.getValue() == null || e.getValue().isBlank()) {
                    continue;
                }
                ps.setLong(1, productoId);
                ps.setLong(2, e.getKey());
                ps.setString(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // precios (solo los > 0)
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO precio_producto (lista_precio_id, producto_id, precio, sucursal_id, vigente_desde) "
                + "VALUES (?, ?, ?, ?, CURRENT_DATE)")) {
            for (PrecioProducto pp : completo.precios()) {
                if (pp.precio() == null || pp.precio().signum() <= 0) {
                    continue;
                }
                ps.setLong(1, pp.listaPrecioId());
                ps.setLong(2, productoId);
                ps.setBigDecimal(3, pp.precio());
                ps.setObject(4, pp.sucursalId());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // componentes de kit
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO producto_componente (producto_kit_id, producto_componente_id, cantidad) VALUES (?, ?, ?)")) {
            for (ComponenteKit comp : completo.componentes()) {
                ps.setLong(1, productoId);
                ps.setLong(2, comp.productoComponenteId());
                ps.setBigDecimal(3, nz(comp.cantidad()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------ helpers

    private Optional<Producto> porId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM producto WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private void ejecutar(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getLong("id"),
                rs.getLong("empresa_id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                (Long) rs.getObject("categoria_id"),
                (Long) rs.getObject("marca_id"),
                (Long) rs.getObject("unidad_medida_id"),
                TipoProducto.valueOf(rs.getString("tipo")),
                (Long) rs.getObject("producto_padre_id"),
                rs.getBigDecimal("costo"),
                rs.getBigDecimal("costo_promedio"),
                rs.getBoolean("controla_lote"),
                rs.getBoolean("controla_serie"),
                rs.getBoolean("es_granel"),
                rs.getString("codigo_plu"),
                rs.getBigDecimal("peso_neto"),
                rs.getBigDecimal("volumen"),
                rs.getString("ubicacion"),
                (Long) rs.getObject("imagen_principal_id"),
                rs.getBoolean("activo"));
    }
}
