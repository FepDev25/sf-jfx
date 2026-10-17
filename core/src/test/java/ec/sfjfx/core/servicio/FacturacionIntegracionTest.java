package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.AjusteDetalle;
import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.TipoImpuesto;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class FacturacionIntegracionTest {

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
    void emiteFacturaDesdeVentaConXmlYRide() throws Exception {
        long marca = System.currentTimeMillis();
        ServicioProducto productos = new ServicioProducto();
        ServicioAjuste ajustes = new ServicioAjuste();
        ServicioVenta ventas = new ServicioVenta();
        ServicioSesionCaja sesiones = new ServicioSesionCaja();
        ServicioComprobante comprobantes = new ServicioComprobante();

        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "FAC" + marca, "Producto factura", null, null, null, null,
                        TipoProducto.SIMPLE, null, BigDecimal.ZERO, BigDecimal.ZERO, false, false, false,
                        null, null, null, null, null, true),
                List.of(), List.of(), Set.of(), List.of(), Map.of(), List.of(), List.of()));
        Bodega bodega = new ec.sfjfx.core.repositorio.BodegaRepositorio().porEmpresa(empresa()).get(0);
        ajustes.aplicar(bodega.id(), "ENTRADA", "INICIAL", "carga",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("5"), new BigDecimal("2.00"))));

        Caja caja = new CajaRepositorio().porEmpresa(empresa()).get(0);
        SesionCaja sesion = sesiones.sesionAbierta(caja.id());
        boolean abrimos = false;
        if (sesion == null) {
            sesion = sesiones.porId(sesiones.abrir(caja.id(), BigDecimal.ZERO));
            abrimos = true;
        }
        Long ivaId = new ServicioImpuesto().deVenta().stream()
                .filter(i -> i.tipo() == TipoImpuesto.IVA && i.porcentaje().signum() > 0)
                .map(Impuesto::id).findFirst().orElseThrow();
        Long efectivoId = new MedioPagoRepositorio().listar().stream()
                .filter(m -> "EFECTIVO".equals(m.tipo())).map(MedioPagoRepositorio.MedioPago::id).findFirst().orElseThrow();

        long ventaId = ventas.cobrar(sesion, bodega.id(), null, null, "factura", false,
                List.of(new VentaDetalle(null, 0, productoId, "Producto", new BigDecimal("2"), new BigDecimal("5.00"),
                        BigDecimal.ZERO, ivaId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                List.of(new VentaPago(null, 0, efectivoId, "Efectivo", new BigDecimal("11.50"), null, BigDecimal.ZERO)),
                BigDecimal.ZERO, BigDecimal.ZERO, null);

        long comprobanteId = comprobantes.emitirFacturaDesdeVenta(ventaId);
        Comprobante c = comprobantes.porId(comprobanteId);
        assertEquals(49, c.claveAcceso().length());
        assertEquals("FACTURA", c.tipo());
        assertTrue(c.pdfPath() != null && Files.exists(Path.of(c.pdfPath())), "Debe existir el RIDE");
        assertTrue(c.xmlPath() != null && Files.exists(Path.of(c.xmlPath())), "Debe existir el XML");
        assertTrue(Files.readString(Path.of(c.xmlPath())).contains("<factura"));

        comprobantes.anular(comprobanteId, "prueba");
        assertEquals("ANULADO", comprobantes.porId(comprobanteId).estado());

        // Limpieza
        Path xml = Path.of(c.xmlPath());
        Path pdf = Path.of(c.pdfPath());
        Bd.transaccion(conn -> {
            ejecutar(conn, "DELETE FROM comprobante_detalle WHERE comprobante_id = ?", comprobanteId);
            ejecutar(conn, "DELETE FROM comprobante_pago WHERE comprobante_id = ?", comprobanteId);
            ejecutar(conn, "DELETE FROM comprobante WHERE id = ?", comprobanteId);
            ejecutar(conn, "DELETE FROM venta_pago WHERE venta_id = ?", ventaId);
            ejecutar(conn, "DELETE FROM venta_detalle WHERE venta_id = ?", ventaId);
            ejecutar(conn, "DELETE FROM venta WHERE id = ?", ventaId);
            ejecutar(conn, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(conn, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(conn, "DELETE FROM historial_precio WHERE producto_id = ?", productoId);
            ejecutar(conn, "DELETE FROM ajuste_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(conn, "DELETE FROM producto WHERE id = ?", productoId);
            return null;
        });
        Files.deleteIfExists(xml);
        Files.deleteIfExists(pdf);
        if (abrimos) {
            sesiones.cerrar(sesion.id(), Map.of(), "fin de prueba");
        }
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
