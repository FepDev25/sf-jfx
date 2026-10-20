package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.DevolucionVenta;
import ec.sfjfx.core.modelo.DevolucionVentaDetalle;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.repositorio.DevolucionVentaRepositorio;
import ec.sfjfx.core.repositorio.VentaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioDevolucionVenta {

    private final ServicioLicencia licencia = new ServicioLicencia();
    private final DevolucionVentaRepositorio repo = new DevolucionVentaRepositorio();
    private final VentaRepositorio ventas = new VentaRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<DevolucionVenta> listar() {
        return repo.listar(empresa(), 200);
    }

    public DevolucionVenta porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Devolucion no encontrada"));
    }

    /** Registra una devolucion total o parcial y repone stock de los items marcados. */
    public long registrar(long ventaId, long bodegaId, String motivo, List<DevolucionVentaDetalle> detalles) {
        licencia.exigirOperacion("registrar una devolucion de venta");
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La devolucion no tiene items");
        }
        Venta venta = ventas.porId(ventaId).orElseThrow(() -> new ValidacionExcepcion("Venta no existe"));
        List<DevolucionVentaDetalle> norm = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DevolucionVentaDetalle d : detalles) {
            if (d.cantidad() == null || d.cantidad().signum() <= 0) {
                throw new ValidacionExcepcion("Cantidad invalida en la devolucion");
            }
            BigDecimal linea = d.cantidad().multiply(nz(d.precioUnitario())).setScale(4, RoundingMode.HALF_UP);
            total = total.add(linea);
            norm.add(new DevolucionVentaDetalle(null, 0, d.productoId(), d.productoNombre(), d.cantidad(),
                    nz(d.precioUnitario()), linea, d.reingresaStock()));
        }
        BigDecimal totalFinal = total;
        DevolucionVenta dev = new DevolucionVenta(null, empresa(), ventaId, venta.numero(), venta.clienteId(),
                numero(), null, motivo, totalFinal, bodegaId, null, "APLICADA", norm);
        long id = Bd.transaccion(c -> {
            long devId = repo.insertar(c, dev, usuario());
            for (DevolucionVentaDetalle d : norm) {
                if (d.reingresaStock() && d.productoId() > 0) {
                    inventario.entradaEn(c, bodegaId, d.productoId(), d.cantidad(), null, "DEVOLUCION_VENTA",
                            "DEVOLUCION_VENTA", devId, null, null, "Devolucion venta " + venta.numero());
                }
            }
            return devId;
        });
        auditoria.registrar("POS", "devolucion_venta", id, "CREAR", null,
                Map.of("venta", venta.numero(), "total", totalFinal.toPlainString()));
        return id;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String numero() {
        return "DV-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
