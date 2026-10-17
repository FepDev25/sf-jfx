package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Pedido;
import ec.sfjfx.core.modelo.PedidoDetalle;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.PedidoRepositorio;
import ec.sfjfx.core.repositorio.ProductoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Pedidos de venta (pickup/delivery) con flujo de estados y conversion a venta. */
public final class ServicioPedido {

    private static final List<String> FLUJO = List.of("RECIBIDO", "EN_PREPARACION", "LISTO", "EN_REPARTO", "ENTREGADO");

    private final PedidoRepositorio repo = new PedidoRepositorio();
    private final ProductoRepositorio productos = new ProductoRepositorio();
    private final ServicioVenta ventas = new ServicioVenta();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Pedido> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Pedido porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Pedido no encontrado"));
    }

    public long crear(long sucursalId, Long clienteId, String tipoEntrega, Long direccionId, String nota,
                      List<PedidoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("El pedido no tiene items");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        List<PedidoDetalle> norm = new ArrayList<>();
        for (PedidoDetalle d : detalles) {
            BigDecimal total = nz(d.cantidad()).multiply(nz(d.precioUnitario())).setScale(4, RoundingMode.HALF_UP);
            subtotal = subtotal.add(total);
            norm.add(new PedidoDetalle(null, 0, d.productoId(), d.productoNombre(), nz(d.cantidad()),
                    nz(d.precioUnitario()), total));
        }
        Pedido pedido = new Pedido(null, empresa(), sucursalId, clienteId, null, numero(), tipoEntrega, "RECIBIDO",
                direccionId, null, null, LocalDateTime.now().plusHours(2), subtotal, BigDecimal.ZERO, subtotal, null,
                null, nota, norm);
        long id = Bd.transaccion(c -> repo.insertar(c, pedido, usuario()));
        auditoria.registrar("CLIENTES", "pedido", id, "CREAR", null, Map.of("items", detalles.size()));
        return id;
    }

    public void cambiarEstado(long id, String estado) {
        Pedido p = porId(id);
        if (p.convertido()) {
            throw new ValidacionExcepcion("El pedido ya fue convertido a venta");
        }
        repo.accionEstado(id, estado);
        auditoria.registrar("CLIENTES", "pedido", id, "EDITAR", null, Map.of("estado", estado));
    }

    /** Convierte el pedido en venta pagada y enlaza el documento. */
    public long convertirAVenta(long pedidoId, SesionCaja sesion, long bodegaId, long medioPagoId, boolean incluyeIva) {
        Pedido p = porId(pedidoId);
        if (p.convertido()) {
            throw new ValidacionExcepcion("El pedido ya fue convertido a venta");
        }
        Map<Long, Long> impuestos = productos.impuestoIvaPorProducto(empresa());
        List<VentaDetalle> lineas = new ArrayList<>();
        for (PedidoDetalle d : p.detalles()) {
            lineas.add(new VentaDetalle(null, 0, d.productoId(), d.productoNombre(), d.cantidad(), d.precioUnitario(),
                    BigDecimal.ZERO, impuestos.get(d.productoId()), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO));
        }
        List<VentaPago> pagos = List.of(new VentaPago(null, 0, medioPagoId, null, nz(p.total()), null, BigDecimal.ZERO));
        long ventaId = ventas.cobrar(sesion, bodegaId, null, p.clienteId(), "Pedido " + p.numero(), incluyeIva, lineas,
                pagos, BigDecimal.ZERO, BigDecimal.ZERO, null);
        Bd.transaccion(c -> {
            repo.vincularVenta(c, pedidoId, ventaId, "ENTREGADO");
            return null;
        });
        auditoria.registrar("CLIENTES", "pedido", pedidoId, "EDITAR", null, Map.of("venta", ventaId));
        return ventaId;
    }

    public static Set<String> estados() {
        return new java.util.LinkedHashSet<>(FLUJO);
    }

    private String numero() {
        return "PED-" + System.currentTimeMillis();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
