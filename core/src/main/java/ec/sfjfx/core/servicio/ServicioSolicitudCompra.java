package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.SolicitudCompra;
import ec.sfjfx.core.modelo.SolicitudDetalle;
import ec.sfjfx.core.repositorio.SolicitudCompraRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioSolicitudCompra {

    private final SolicitudCompraRepositorio repo = new SolicitudCompraRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<SolicitudCompra> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public SolicitudCompra porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Solicitud no encontrada"));
    }

    public long crear(long sucursalId, String nota, List<SolicitudDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La solicitud no tiene items");
        }
        SolicitudCompra s = new SolicitudCompra(null, empresa(), sucursalId, numero(), "BORRADOR", null, nota, detalles);
        long id = Bd.transaccion(c -> repo.insertar(c, s, usuario()));
        auditoria.registrar("COMPRAS", "solicitud_compra", id, "CREAR", null, Map.of("items", detalles.size()));
        return id;
    }

    public void aprobar(long id) {
        cambiarEstado(id, "APROBADA", "AUTORIZAR");
    }

    public void rechazar(long id) {
        cambiarEstado(id, "RECHAZADA", "ANULAR");
    }

    private void cambiarEstado(long id, String estado, String accion) {
        Bd.transaccion(c -> {
            repo.cambiarEstado(c, id, estado, usuario());
            return null;
        });
        auditoria.registrar("COMPRAS", "solicitud_compra", id, accion, null, Map.of("estado", estado));
    }

    private String numero() {
        return "SC-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
