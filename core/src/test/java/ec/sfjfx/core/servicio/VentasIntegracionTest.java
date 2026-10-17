package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import ec.sfjfx.core.modelo.DevolucionVentaDetalle;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.TipoImpuesto;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class VentasIntegracionTest {

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
    void ventaDescuentaStockYCobraConImpuesto() {
        long marca = System.currentTimeMillis();
        ServicioProducto productos = new ServicioProducto();
        ServicioAjuste ajustes = new ServicioAjuste();
        ServicioVenta ventas = new ServicioVenta();
        ServicioSesionCaja sesiones = new ServicioSesionCaja();
        ServicioDevolucionVenta devoluciones = new ServicioDevolucionVenta();
        ServicioInventario inventario = new ServicioInventario();

        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "VEN" + marca, "Producto venta", null, null, null, null,
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
// @formatter:off
            sesion = sesiones.porId(sesiones.abrir(caja.id(), BigDecimal.ZERO));
// @formatter:on
            abrimos = true;
        }

        Long ivaId = new ServicioImpuesto().deVenta().stream()
                .filter(i -> i.tipo() == TipoImpuesto.IVA && i.porcentaje().signum() > 0)
                .map(Impuesto::id).findFirst().orElseThrow();
        Long efectivoId = new MedioPagoRepositorio().listar().stream()
                .filter(m -> "EFECTIVO".equals(m.tipo())).map(MedioPagoRepositorio.MedioPago::id).findFirst().orElseThrow();

        // 2 x 10.00 + IVA 15% = 23.00 (precio sin IVA)
        long ventaId = ventas.cobrar(sesion, bodega.id(), null, null, "prueba", false,
                List.of(new VentaDetalle(null, 0, productoId, "Producto venta", new BigDecimal("2"),
                        new BigDecimal("10.00"), BigDecimal.ZERO, ivaId, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO)),
                List.of(new VentaPago(null, 0, efectivoId, "Efectivo", new BigDecimal("23.00"), null, BigDecimal.ZERO)),
                BigDecimal.ZERO, BigDecimal.ZERO, null);

        Venta venta = ventas.porId(ventaId);
        assertNotNull(venta);
        assertEquals(0, venta.total().compareTo(new BigDecimal("23.0000")), "Total con IVA 15% debe ser 23.00");
        assertEquals(0, cantidadEn(inventario, productoId, bodega.id()).compareTo(new BigDecimal("3")),
                "Stock debe bajar de 5 a 3");

        long devolucionId = devoluciones.registrar(ventaId, bodega.id(), "DEVOLUCION",
                List.of(new DevolucionVentaDetalle(null, 0, productoId, "x", new BigDecimal("1"),
                        new BigDecimal("10.00"), BigDecimal.ZERO, true)));
        assertEquals(0, cantidadEn(inventario, productoId, bodega.id()).compareTo(new BigDecimal("4")),
                "Devolucion debe reponer stock a 4");

        Bd.transaccion(c -> {
            ejecutar(c, "DELETE FROM devolucion_venta_detalle WHERE devolucion_id = ?", devolucionId);
            ejecutar(c, "DELETE FROM devolucion_venta WHERE id = ?", devolucionId);
            ejecutar(c, "DELETE FROM venta_pago WHERE venta_id = ?", ventaId);
            ejecutar(c, "DELETE FROM venta_detalle WHERE venta_id = ?", ventaId);
            ejecutar(c, "DELETE FROM venta WHERE id = ?", ventaId);
            ejecutar(c, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM historial_precio WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM ajuste_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM producto WHERE id = ?", productoId);
            return null;
        });
        if (abrimos) {
            sesiones.cerrar(sesion.id(), Map.of(), "fin de prueba");
        }
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
