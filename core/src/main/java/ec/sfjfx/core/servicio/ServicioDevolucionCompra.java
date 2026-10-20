package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.DevolucionCompra;
import ec.sfjfx.core.modelo.DevolucionCompraDetalle;
import ec.sfjfx.core.repositorio.DevolucionCompraRepositorio;
import ec.sfjfx.core.repositorio.FacturaProveedorRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioDevolucionCompra {

    private final ServicioLicencia licencia = new ServicioLicencia();
    private final DevolucionCompraRepositorio repo = new DevolucionCompraRepositorio();
    private final FacturaProveedorRepositorio facturas = new FacturaProveedorRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<DevolucionCompra> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public DevolucionCompra porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Devolucion no encontrada"));
    }

    /** Registra la devolucion: saca stock y reduce el saldo de la factura de proveedor. */
    public long registrar(long proveedorId, Long facturaProveedorId, long bodegaId, String motivo, String nota,
                          List<DevolucionCompraDetalle> detalles) {
        licencia.exigirOperacion("registrar una devolucion a proveedor");
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La devolucion no tiene items");
        }
        List<DevolucionCompraDetalle> normalizados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DevolucionCompraDetalle d : detalles) {
            if (d.cantidad() == null || d.cantidad().signum() <= 0) {
                throw new ValidacionExcepcion("Cantidad invalida en la devolucion");
            }
            BigDecimal linea = d.cantidad().multiply(nz(d.costoUnitario())).setScale(4, RoundingMode.HALF_UP);
            total = total.add(linea);
            normalizados.add(new DevolucionCompraDetalle(null, 0, d.productoId(), d.productoNombre(), d.loteId(),
                    d.cantidad(), nz(d.costoUnitario()), linea));
        }
        BigDecimal totalFinal = total;
        DevolucionCompra dev = new DevolucionCompra(null, empresa(), proveedorId, null, facturaProveedorId, null,
                bodegaId, null, numero(), null, motivo, totalFinal, "APLICADA", nota, normalizados);
        long id = Bd.transaccion(c -> {
            long devId = repo.insertar(c, dev, usuario());
            for (DevolucionCompraDetalle d : normalizados) {
                inventario.salidaEn(c, bodegaId, d.productoId(), d.cantidad(), "DEVOLUCION_COMPRA", "DEVOLUCION_COMPRA",
                        devId, d.loteId(), nota);
            }
            if (facturaProveedorId != null) {
                facturas.aplicarPago(c, facturaProveedorId, totalFinal);
            }
            return devId;
        });
        auditoria.registrar("COMPRAS", "devolucion_compra", id, "CREAR", null,
                Map.of("items", detalles.size(), "total", totalFinal.toPlainString()));
        return id;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String numero() {
        return "DC-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
