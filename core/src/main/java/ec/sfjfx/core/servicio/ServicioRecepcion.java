package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.RecepcionCompra;
import ec.sfjfx.core.modelo.RecepcionDetalle;
import ec.sfjfx.core.repositorio.LoteRepositorio;
import ec.sfjfx.core.repositorio.OrdenCompraRepositorio;
import ec.sfjfx.core.repositorio.RecepcionCompraRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioRecepcion {

    private final RecepcionCompraRepositorio repo = new RecepcionCompraRepositorio();
    private final OrdenCompraRepositorio ordenes = new OrdenCompraRepositorio();
    private final LoteRepositorio lotes = new LoteRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<RecepcionCompra> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public RecepcionCompra porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Recepcion no encontrada"));
    }

    /**
     * Registra una recepcion: ingresa stock (con PPP), crea lotes si aplica y
     * actualiza las cantidades recibidas de la orden.
     */
    public long registrar(Long ordenCompraId, long bodegaId, String nota, List<RecepcionDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La recepcion no tiene items");
        }
        long id = Bd.transaccion(c -> {
            long rec = repo.insertar(c, new RecepcionCompra(null, empresa(), ordenCompraId, numero(), bodegaId, null,
                    null, "CONFIRMADA", nota, detalles), usuario());
            for (RecepcionDetalle d : detalles) {
                if (d.cantidadRecibida() == null || d.cantidadRecibida().signum() <= 0) {
                    throw new ValidacionExcepcion("Cantidad recibida invalida");
                }
                Long loteId = d.loteId();
                if (loteId == null && d.loteCodigo() != null && !d.loteCodigo().isBlank()) {
                    loteId = lotes.buscarOCrear(c, d.productoId(), d.loteCodigo().trim(), null, d.fechaVencimiento());
                }
                inventario.entradaEn(c, bodegaId, d.productoId(), d.cantidadRecibida(), d.costoUnitario(), "COMPRA",
                        "COMPRA", rec, loteId, null, nota);
                if (d.ordenCompraDetalleId() != null) {
                    ordenes.sumarRecibida(c, d.ordenCompraDetalleId(), d.cantidadRecibida());
                }
            }
            if (ordenCompraId != null) {
                ordenes.recalcularEstado(c, ordenCompraId);
            }
            return rec;
        });
        auditoria.registrar("COMPRAS", "recepcion", id, "CREAR", null, Map.of("items", detalles.size()));
        return id;
    }

    private String numero() {
        return "RC-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
