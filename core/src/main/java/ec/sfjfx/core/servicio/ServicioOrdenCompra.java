package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.OrdenCompra;
import ec.sfjfx.core.modelo.OrdenCompraDetalle;
import ec.sfjfx.core.repositorio.OrdenCompraRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioOrdenCompra {

    private final OrdenCompraRepositorio repo = new OrdenCompraRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<OrdenCompra> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public OrdenCompra porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Orden no encontrada"));
    }

    public long crear(long sucursalId, long proveedorId, LocalDate fechaEntrega, String condicionesPago, String nota,
                      List<OrdenCompraDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La orden no tiene items");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        for (OrdenCompraDetalle d : detalles) {
            if (d.cantidad() == null || d.cantidad().signum() <= 0) {
                throw new ValidacionExcepcion("Cantidad invalida en la orden");
            }
        }
        // Los totales de linea los calcula el servicio
        java.util.List<OrdenCompraDetalle> normalizados = new java.util.ArrayList<>();
        for (OrdenCompraDetalle d : detalles) {
            BigDecimal sub = d.cantidad().multiply(nz(d.precioUnitario())).setScale(4, RoundingMode.HALF_UP);
            BigDecimal desc = nz(d.descuento());
            BigDecimal tot = sub.subtract(desc);
            subtotal = subtotal.add(sub);
            descuento = descuento.add(desc);
            normalizados.add(new OrdenCompraDetalle(null, 0, d.productoId(), d.productoNombre(), d.descripcion(),
                    d.cantidad(), BigDecimal.ZERO, nz(d.precioUnitario()), d.impuestoId(), desc, sub, tot));
        }
        BigDecimal total = subtotal.subtract(descuento);
        OrdenCompra orden = new OrdenCompra(null, empresa(), sucursalId, proveedorId, null, numero(), "BORRADOR",
                null, fechaEntrega, condicionesPago, subtotal, descuento, BigDecimal.ZERO, total, nota, normalizados);
        long id = Bd.transaccion(c -> repo.insertar(c, orden, usuario()));
        auditoria.registrar("COMPRAS", "orden_compra", id, "CREAR", null, Map.of("items", detalles.size()));
        return id;
    }

    public void aprobar(long id) {
        Bd.transaccion(c -> {
            OrdenCompra o = repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if (!"BORRADOR".equals(o.estado())) {
                throw new ValidacionExcepcion("Solo se aprueban ordenes en borrador");
            }
            repo.cambiarEstado(c, id, "APROBADA");
            return null;
        });
        auditoria.registrar("COMPRAS", "orden_compra", id, "AUTORIZAR", null, null);
    }

    public void anular(long id) {
        Bd.transaccion(c -> {
            OrdenCompra o = repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if ("RECIBIDA".equals(o.estado()) || "RECEPCION_PARCIAL".equals(o.estado())) {
                throw new ValidacionExcepcion("No se puede anular una orden con recepciones");
            }
            repo.cambiarEstado(c, id, "ANULADA");
            return null;
        });
        auditoria.registrar("COMPRAS", "orden_compra", id, "ANULAR", null, null);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String numero() {
        return "OC-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
