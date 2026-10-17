package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.PagoProveedor;
import ec.sfjfx.core.repositorio.FacturaProveedorRepositorio;
import ec.sfjfx.core.repositorio.PagoProveedorRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioPagoProveedor {

    private final PagoProveedorRepositorio repo = new PagoProveedorRepositorio();
    private final FacturaProveedorRepositorio facturas = new FacturaProveedorRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<PagoProveedor> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    /** Registra un pago aplicandolo a una o varias facturas (pagos parciales permitidos). */
    public long registrar(long proveedorId, BigDecimal total, Long medioPagoId, String referencia, String nota,
                          List<PagoProveedor.PagoFactura> aplicaciones) {
        if (aplicaciones == null || aplicaciones.isEmpty()) {
            throw new ValidacionExcepcion("El pago debe aplicarse al menos a una factura");
        }
        BigDecimal suma = aplicaciones.stream().map(PagoProveedor.PagoFactura::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(total) > 0) {
            throw new ValidacionExcepcion("La suma aplicada no puede superar el total del pago");
        }
        for (PagoProveedor.PagoFactura a : aplicaciones) {
            FacturaProveedor f = facturas.porId(a.facturaProveedorId())
                    .orElseThrow(() -> new ValidacionExcepcion("Factura no encontrada"));
            if (a.monto().compareTo(f.saldo()) > 0) {
                throw new ValidacionExcepcion("El monto aplicado supera el saldo de la factura " + f.numero());
            }
        }
        PagoProveedor pago = new PagoProveedor(null, empresa(), proveedorId, null, numero(), null, total, medioPagoId,
                referencia, nota, aplicaciones);
        long id = Bd.transaccion(c -> repo.insertar(c, pago, usuario()));
        auditoria.registrar("COMPRAS", "pago_proveedor", id, "CREAR", null,
                Map.of("total", total.toPlainString(), "facturas", aplicaciones.size()));
        return id;
    }

    private String numero() {
        return "PAG-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
