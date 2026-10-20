package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.AjusteDetalle;
import ec.sfjfx.core.modelo.AjusteInventario;
import ec.sfjfx.core.repositorio.AjusteInventarioRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioAjuste {

    private final ServicioLicencia licencia = new ServicioLicencia();
    private final AjusteInventarioRepositorio repo = new AjusteInventarioRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<AjusteInventario> listar(String filtro) {
        return repo.listar(SesionActual.requerida().empresa().id(), filtro);
    }

    public AjusteInventario porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Ajuste no encontrado"));
    }

    /** Registra y aplica un ajuste (entrada o salida) en una sola transaccion. */
    public long aplicar(long bodegaId, String tipo, String motivo, String nota, List<AjusteDetalle> detalles) {
        licencia.exigirOperacion("aplicar un ajuste de inventario");
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionExcepcion("El ajuste no tiene items");
        }
        boolean entrada = "ENTRADA".equals(tipo);
        String tipoMov = entrada ? "AJUSTE_ENTRADA" : "AJUSTE_SALIDA";
        long id = Bd.transaccion(c -> {
            long aj = repo.insertar(c, new AjusteInventario(null, empresa(), bodegaId, null, numero(), tipo,
                    motivo, "BORRADOR", null, nota, detalles));
            for (AjusteDetalle d : detalles) {
                if (d.cantidad() == null || d.cantidad().signum() <= 0) {
                    throw new ValidacionExcepcion("Cantidad invalida en el ajuste");
                }
                if (entrada) {
                    inventario.entradaEn(c, bodegaId, d.productoId(), d.cantidad(), d.costoUnitario(), tipoMov,
                            "AJUSTE", aj, d.loteId(), null, nota);
                } else {
                    inventario.salidaEn(c, bodegaId, d.productoId(), d.cantidad(), tipoMov, "AJUSTE", aj,
                            d.loteId(), nota);
                }
            }
            repo.cambiarEstado(c, aj, "APLICADO");
            return aj;
        });
        auditoria.registrar("INVENTARIO", "ajuste", id, "CREAR", null,
                Map.of("tipo", tipo, "items", detalles.size()));
        return id;
    }

    private String numero() {
        return "AJ-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
