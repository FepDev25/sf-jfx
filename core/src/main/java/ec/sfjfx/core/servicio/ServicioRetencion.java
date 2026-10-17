package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.Retencion;
import ec.sfjfx.core.modelo.RetencionDetalle;
import ec.sfjfx.core.repositorio.FacturaProveedorRepositorio;
import ec.sfjfx.core.repositorio.RetencionRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioRetencion {

    private final RetencionRepositorio repo = new RetencionRepositorio();
    private final FacturaProveedorRepositorio facturas = new FacturaProveedorRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Retencion> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Retencion porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Retencion no encontrada"));
    }

    /** Emite un comprobante de retencion (IVA y/o Renta) para una factura de proveedor. */
    public long emitir(long proveedorId, Long facturaProveedorId, LocalDate fechaEmision,
                       List<RetencionDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La retencion no tiene lineas");
        }
        BigDecimal baseIva = BigDecimal.ZERO;
        BigDecimal retIva = BigDecimal.ZERO;
        BigDecimal baseRenta = BigDecimal.ZERO;
        BigDecimal retRenta = BigDecimal.ZERO;
        for (RetencionDetalle d : detalles) {
            if ("IVA".equals(d.tipoImpuesto())) {
                baseIva = baseIva.add(nz(d.baseImponible()));
                retIva = retIva.add(nz(d.valorRetenido()));
            } else {
                baseRenta = baseRenta.add(nz(d.baseImponible()));
                retRenta = retRenta.add(nz(d.valorRetenido()));
            }
        }
        BigDecimal valorIva = BigDecimal.ZERO;
        if (facturaProveedorId != null) {
            FacturaProveedor f = facturas.porId(facturaProveedorId).orElse(null);
            if (f != null) {
                valorIva = nz(f.iva());
            }
        }
        Retencion r = new Retencion(null, empresa(), proveedorId, null, facturaProveedorId, secuencial(),
                fechaEmision == null ? LocalDate.now() : fechaEmision, baseIva, valorIva, retIva, baseRenta, retRenta,
                retIva.add(retRenta), "EMITIDO", detalles);
        long id = Bd.transaccion(c -> repo.insertar(c, r, usuario()));
        auditoria.registrar("COMPRAS", "retencion", id, "CREAR", null,
                Map.of("proveedor", proveedorId, "total", r.totalRetenido().toPlainString()));
        return id;
    }

    private String secuencial() {
        return String.format("%09d", System.currentTimeMillis() % 1_000_000_000L);
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
