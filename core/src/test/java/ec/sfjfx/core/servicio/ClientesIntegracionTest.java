package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.PedidoDetalle;
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

class ClientesIntegracionTest {

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
    void creditoYCobranzaYPedidoConvertido() {
        long marca = System.currentTimeMillis();
        ServicioCliente servicioCliente = new ServicioCliente();
        ServicioProducto productos = new ServicioProducto();
        ServicioAjuste ajustes = new ServicioAjuste();
        ServicioVenta ventas = new ServicioVenta();
        ServicioSesionCaja sesiones = new ServicioSesionCaja();
        ServicioCxc cxc = new ServicioCxc();
        ServicioPedido pedidos = new ServicioPedido();
        ServicioInventario inventario = new ServicioInventario();

        long clienteId = servicioCliente.guardar(new Cliente(null, empresa(), "PERSONA_NATURAL", "CEDULA",
                "1710034081", "Cliente", "Prueba", null, null, null, null, true, new BigDecimal("1000"), 30,
                BigDecimal.ZERO, true)).id();
        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "CLI" + marca, "Producto cliente", null, null, null, null,
                        TipoProducto.SIMPLE, null, BigDecimal.ZERO, BigDecimal.ZERO, false, false, false,
                        null, null, null, null, null, true),
                List.of(), List.of(), Set.of(), List.of(), Map.of(), List.of(), List.of()));
        Bodega bodega = new ec.sfjfx.core.repositorio.BodegaRepositorio().porEmpresa(empresa()).get(0);
        ajustes.aplicar(bodega.id(), "ENTRADA", "INICIAL", "carga",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("10"), new BigDecimal("2.00"))));

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
        Long creditoId = new MedioPagoRepositorio().listar().stream()
                .filter(m -> "CREDITO".equals(m.tipo())).map(MedioPagoRepositorio.MedioPago::id).findFirst().orElseThrow();

        // Venta a credito 1 x 10.00 + IVA = 11.50
        long ventaCredito = ventas.cobrar(sesion, bodega.id(), null, clienteId, "credito", false,
                List.of(new VentaDetalle(null, 0, productoId, "Producto", BigDecimal.ONE, new BigDecimal("10.00"),
                        BigDecimal.ZERO, ivaId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                List.of(new VentaPago(null, 0, creditoId, "Credito", new BigDecimal("11.50"), null, BigDecimal.ZERO)),
                BigDecimal.ZERO, BigDecimal.ZERO, null);
        assertEquals(0, servicioCliente.porId(clienteId).saldo().compareTo(new BigDecimal("11.5000")),
                "El saldo del cliente debe ser 11.50");
        assertTrue(cxc.listar(clienteId).stream().anyMatch(m -> "CARGO".equals(m.tipo())));

        // Abono 5 => saldo 6.50
        Long efectivoId = new MedioPagoRepositorio().listar().stream()
                .filter(m -> "EFECTIVO".equals(m.tipo())).map(MedioPagoRepositorio.MedioPago::id).findFirst().orElseThrow();
        cxc.abonar(clienteId, new BigDecimal("5"), efectivoId, "abono");
        assertEquals(0, servicioCliente.porId(clienteId).saldo().compareTo(new BigDecimal("6.5000")));

        // Pedido y conversion a venta
        long pedidoId = pedidos.crear(bodega.sucursalId(), clienteId, "PICKUP", null, "prueba",
                List.of(new PedidoDetalle(null, 0, productoId, "Producto", BigDecimal.ONE, new BigDecimal("5.00"), BigDecimal.ZERO)));
        long ventaPedido = pedidos.convertirAVenta(pedidoId, sesion, bodega.id(), efectivoId, true);
        assertNotNull(ventas.porId(ventaPedido));
        assertTrue(pedidos.porId(pedidoId).convertido());

        // Limpieza
        Bd.transaccion(c -> {
            ejecutar(c, "DELETE FROM cxc_movimiento WHERE cliente_id = ?", clienteId);
            ejecutar(c, "DELETE FROM pedido_detalle WHERE pedido_id = ?", pedidoId);
            ejecutar(c, "DELETE FROM pedido WHERE id = ?", pedidoId);
            ejecutar(c, "DELETE FROM venta_pago WHERE venta_id = ?", ventaCredito);
            ejecutar(c, "DELETE FROM venta_detalle WHERE venta_id = ?", ventaCredito);
            ejecutar(c, "DELETE FROM venta WHERE id = ?", ventaCredito);
            ejecutar(c, "DELETE FROM venta_pago WHERE venta_id = ?", ventaPedido);
            ejecutar(c, "DELETE FROM venta_detalle WHERE venta_id = ?", ventaPedido);
            ejecutar(c, "DELETE FROM venta WHERE id = ?", ventaPedido);
            ejecutar(c, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM historial_precio WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM ajuste_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM producto WHERE id = ?", productoId);
            ejecutar(c, "DELETE FROM cliente WHERE id = ?", clienteId);
            return null;
        });
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
