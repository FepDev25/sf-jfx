package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.repositorio.FacturaProveedorRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioFacturaProveedor {

    private final FacturaProveedorRepositorio repo = new FacturaProveedorRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<FacturaProveedor> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public List<FacturaProveedor> conSaldo(Long proveedorId) {
        return repo.conSaldo(empresa(), proveedorId);
    }

    public FacturaProveedor porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Factura no encontrada"));
    }

    public long registrar(long proveedorId, String tipoDocumento, String numero, LocalDate fechaEmision,
                          LocalDate fechaVencimiento, BigDecimal subtotal, BigDecimal descuento, BigDecimal iva,
                          BigDecimal total, Long ordenCompraId, Long recepcionId) {
        Validaciones.requerido(numero, "Numero");
        if (fechaEmision == null) {
            throw new ValidacionExcepcion("La fecha de emision es obligatoria");
        }
        if (repo.numeroExiste(empresa(), proveedorId, numero)) {
            throw new ValidacionExcepcion("Ya existe una factura con ese numero para el proveedor");
        }
        FacturaProveedor f = new FacturaProveedor(null, empresa(), proveedorId, null, ordenCompraId, recepcionId,
                tipoDocumento, numero, fechaEmision, fechaVencimiento, subtotal, descuento, iva, total, total,
                "PENDIENTE");
        long id = Bd.transaccion(c -> repo.insertar(c, f, usuario()));
        auditoria.registrar("COMPRAS", "factura_proveedor", id, "CREAR", null,
                Map.of("numero", numero, "total", total.toPlainString()));
        return id;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
