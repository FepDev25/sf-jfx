package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.DevolucionCompraDetalle;
import ec.sfjfx.core.modelo.OrdenCompraDetalle;
import ec.sfjfx.core.modelo.PagoProveedor;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.modelo.RecepcionDetalle;
import ec.sfjfx.core.modelo.RetencionDetalle;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.repositorio.FacturaProveedorRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class ComprasIntegracionTest {

    @BeforeAll
    static void preparar() throws Exception {
        Path config = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config), "Sin configuracion local");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");
        SesionActual.iniciar(new ServicioAutenticacion().iniciar("admin", "admin"));
    }

    @Test
    void cicloDeCompraCompleto() {
        long marca = System.currentTimeMillis();
        ServicioProveedor proveedores = new ServicioProveedor();
        ServicioProducto productos = new ServicioProducto();
        ServicioOrdenCompra ordenes = new ServicioOrdenCompra();
        ServicioRecepcion recepciones = new ServicioRecepcion();
        ServicioFacturaProveedor facturas = new ServicioFacturaProveedor();
        ServicioPagoProveedor pagos = new ServicioPagoProveedor();
        ServicioRetencion retenciones = new ServicioRetencion();
        ServicioDevolucionCompra devoluciones = new ServicioDevolucionCompra();
        ServicioInventario inventario = new ServicioInventario();

        long proveedorId = proveedores.guardar(new Proveedor(null, empresa(), "PASAPORTE", "P" + marca,
                "Proveedor " + marca, null, null, null, null, "SOCIEDAD", 30, true, true, true, null)).id();
        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "CMP" + marca, "Producto compra", null, null, null, null,
                        TipoProducto.SIMPLE, null, BigDecimal.ZERO, BigDecimal.ZERO, false, false, false,
                        null, null, null, null, null, true),
                List.of(), List.of(), Set.of(), List.of(), Map.of(), List.of(), List.of()));
        Bodega bodega = new BodegaRepositorio().porEmpresa(empresa()).get(0);

        // Orden + recepcion (5 @ 2.00)
        long ordenId = ordenes.crear(bodega.sucursalId(), proveedorId, LocalDate.now().plusDays(3), "CONTADO", "prueba",
                List.of(new OrdenCompraDetalle(null, 0, productoId, null, null, new BigDecimal("5"), BigDecimal.ZERO,
                        new BigDecimal("2.00"), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        ordenes.aprobar(ordenId);
        long detalleOrdenId = ordenes.porId(ordenId).detalles().get(0).id();

        long recepcionId = recepciones.registrar(ordenId, bodega.id(), "prueba",
                List.of(new RecepcionDetalle(null, 0, detalleOrdenId, productoId, null, null, null, null,
                        new BigDecimal("5"), new BigDecimal("2.00"))));
        assertEquals(0, cantidadEn(inventario, productoId, bodega.id()).compareTo(new BigDecimal("5")));
        assertEquals(0, productos.cargar(productoId).producto().costoPromedio().compareTo(new BigDecimal("2.00")));
        assertEquals("RECIBIDA", ordenes.porId(ordenId).estado());

        // Factura 11.50 y pago parcial 5 => saldo 6.50
        long facturaId = facturas.registrar(proveedorId, "FACTURA", "F" + marca, LocalDate.now(),
                LocalDate.now().plusDays(30), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("1.5"),
                new BigDecimal("11.5"), null, null);
        assertEquals(0, facturas.porId(facturaId).saldo().compareTo(new BigDecimal("11.5")));

        long pagoId = pagos.registrar(proveedorId, new BigDecimal("5"), null, "REF", null,
                List.of(new PagoProveedor.PagoFactura(facturaId, "F" + marca, new BigDecimal("5"))));
        assertEquals(0, facturas.porId(facturaId).saldo().compareTo(new BigDecimal("6.5")));

        // Retencion IVA 30% sobre base 10 => 3.00
        long retencionId = retenciones.emitir(proveedorId, facturaId, LocalDate.now(),
                List.of(new RetencionDetalle(null, 0, "IVA", "725", new BigDecimal("10"), new BigDecimal("30"),
                        new BigDecimal("3"))));
        assertEquals(0, retenciones.porId(retencionId).totalRetenido().compareTo(new BigDecimal("3")));

        // Devolucion 2 @ 2.00 => stock 3 y saldo 6.5 - 4 = 2.5
        long devolucionId = devoluciones.registrar(proveedorId, facturaId, bodega.id(), "DANO", "prueba",
                List.of(new DevolucionCompraDetalle(null, 0, productoId, null, null, new BigDecimal("2"),
                        new BigDecimal("2.00"), BigDecimal.ZERO)));
        assertEquals(0, cantidadEn(inventario, productoId, bodega.id()).compareTo(new BigDecimal("3")));
        assertEquals(0, facturas.porId(facturaId).saldo().compareTo(new BigDecimal("2.5")));

        // Limpieza
        Bd.transaccion(c -> {
            ejecutar(c, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM devolucion_compra_detalle WHERE devolucion_id = ?", devolucionId);
            ejecutar(c, "DELETE FROM devolucion_compra WHERE id = ?", devolucionId);
            ejecutar(c, "DELETE FROM recepcion_compra_detalle WHERE recepcion_id = ?", recepcionId);
            ejecutar(c, "DELETE FROM recepcion_compra WHERE id = ?", recepcionId);
            ejecutar(c, "DELETE FROM orden_compra_detalle WHERE orden_compra_id = ?", ordenId);
            ejecutar(c, "DELETE FROM orden_compra WHERE id = ?", ordenId);
            ejecutar(c, "DELETE FROM retencion_detalle WHERE retencion_id = ?", retencionId);
            ejecutar(c, "DELETE FROM retencion WHERE id = ?", retencionId);
            ejecutar(c, "DELETE FROM pago_proveedor_factura WHERE pago_id = ?", pagoId);
            ejecutar(c, "DELETE FROM pago_proveedor WHERE id = ?", pagoId);
            ejecutar(c, "DELETE FROM factura_proveedor WHERE id = ?", facturaId);
            ejecutar(c, "DELETE FROM producto WHERE id = ?", productoId);
            ejecutar(c, "DELETE FROM proveedor WHERE id = ?", proveedorId);
            return null;
        });
        assertTrue(new FacturaProveedorRepositorio().porId(facturaId).isEmpty());
    }

    private BigDecimal cantidadEn(ServicioInventario inventario, long productoId, long bodegaId) {
        return inventario.stockProducto(productoId).stream()
                .filter(s -> s.bodegaId() == bodegaId)
                .map(s -> s.cantidad())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ejecutar(Connection c, String sql, long id) throws SQLException {
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
