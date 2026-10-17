package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Transferencia;
import ec.sfjfx.core.modelo.TransferenciaDetalle;
import ec.sfjfx.core.repositorio.TransferenciaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioTransferencia {

    private final TransferenciaRepositorio repo = new TransferenciaRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Transferencia> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Transferencia porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Transferencia no encontrada"));
    }

    public List<TransferenciaDetalle> detalles(long id) {
        return repo.detalles(id);
    }

    public long crear(long sucursalOrigenId, long bodegaOrigenId, long sucursalDestinoId, long bodegaDestinoId,
                      String nota, List<TransferenciaDetalle> detalles) {
        if (bodegaOrigenId == bodegaDestinoId) {
            throw new ValidacionExcepcion("La bodega destino debe ser distinta a la de origen");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("La transferencia no tiene items");
        }
        Transferencia cabecera = new Transferencia(null, empresa(), numero(), sucursalOrigenId, bodegaOrigenId, null,
                sucursalDestinoId, bodegaDestinoId, null, "BORRADOR", null, null, nota);
        long id = Bd.transaccion(c -> repo.insertar(c, cabecera, detalles));
        auditoria.registrar("INVENTARIO", "transferencia", id, "CREAR", null, Map.of("items", detalles.size()));
        return id;
    }

    /** Envia: da salida del origen y deja la transferencia en transito. */
    public void enviar(long id) {
        Bd.transaccion(c -> {
            Transferencia t = repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if (!"BORRADOR".equals(t.estado())) {
                throw new ValidacionExcepcion("Solo se pueden enviar transferencias en borrador");
            }
            List<TransferenciaDetalle> detalles = repo.detallesEn(c, id);
            if (detalles.isEmpty()) {
                throw new ValidacionExcepcion("La transferencia no tiene items");
            }
            for (TransferenciaDetalle d : detalles) {
                inventario.salidaEn(c, t.bodegaOrigenId(), d.productoId(), d.cantidadEnviada(),
                        "TRANSFERENCIA_SALIDA", "TRANSFERENCIA", id, d.loteId(), t.nota());
            }
            repo.marcarEnTransito(c, id, usuario());
            return null;
        });
        auditoria.registrar("INVENTARIO", "transferencia", id, "EDITAR", null, Map.of("accion", "ENVIAR"));
    }

    /** Recibe: ingresa al destino e iguala cantidades recibidas a las enviadas. */
    public void recibir(long id) {
        Bd.transaccion(c -> {
            Transferencia t = repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if (!"EN_TRANSITO".equals(t.estado())) {
                throw new ValidacionExcepcion("Solo se reciben transferencias en transito");
            }
            List<TransferenciaDetalle> detalles = repo.detallesEn(c, id);
            for (TransferenciaDetalle d : detalles) {
                inventario.entradaEn(c, t.bodegaDestinoId(), d.productoId(), d.cantidadEnviada(), null,
                        "TRANSFERENCIA_ENTRADA", "TRANSFERENCIA", id, d.loteId(), null, t.nota());
                repo.registrarCantidadRecibida(c, d.id(), d.cantidadEnviada());
            }
            repo.marcarRecibida(c, id, usuario());
            return null;
        });
        auditoria.registrar("INVENTARIO", "transferencia", id, "EDITAR", null, Map.of("accion", "RECIBIR"));
    }

    public void anular(long id) {
        Bd.transaccion(c -> {
            Transferencia t = repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if (!"BORRADOR".equals(t.estado())) {
                throw new ValidacionExcepcion("Solo se anulan transferencias en borrador");
            }
            repo.anular(c, id);
            return null;
        });
        auditoria.registrar("INVENTARIO", "transferencia", id, "ANULAR", null, null);
    }

    private String numero() {
        return "TR-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
