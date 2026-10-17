package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.StockItem;
import ec.sfjfx.core.modelo.TomaDetalle;
import ec.sfjfx.core.modelo.TomaInventario;
import ec.sfjfx.core.repositorio.TomaInventarioRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

public final class ServicioToma {

    private final TomaInventarioRepositorio repo = new TomaInventarioRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<TomaInventario> listar(String filtro) {
        return repo.listar(SesionActual.requerida().empresa().id(), filtro);
    }

    public TomaInventario porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Toma no encontrada"));
    }

    /** Abre una toma cargando las existencias actuales de la bodega (agregadas por producto). */
    public long abrir(long bodegaId, boolean conteoCiego, String observacion) {
        List<StockItem> stock = inventario.stock(bodegaId, null);
        Map<Long, TomaDetalle> porProducto = new LinkedHashMap<>();
        for (StockItem s : stock) {
            TomaDetalle existente = porProducto.get(s.productoId());
            BigDecimal cantidad = existente == null ? s.cantidad() : existente.cantidadSistema().add(s.cantidad());
            porProducto.put(s.productoId(), new TomaDetalle(null, 0, s.productoId(), s.producto(), null,
                    cantidad, BigDecimal.ZERO, BigDecimal.ZERO, s.costoPromedio()));
        }
        if (porProducto.isEmpty()) {
            throw new ValidacionExcepcion("La bodega no tiene existencias para contar");
        }
        TomaInventario toma = new TomaInventario(null, empresa(), bodegaId, null, numero(), "ABIERTA",
                conteoCiego, null, null, observacion, new ArrayList<>(porProducto.values()));
        long id = Bd.transaccion(c -> repo.insertar(c, toma));
        auditoria.registrar("INVENTARIO", "toma", id, "CREAR", null, Map.of("items", porProducto.size()));
        return id;
    }

    public void guardarConteos(long tomaId, Map<Long, BigDecimal> conteosPorProducto) {
        Bd.transaccion(c -> {
            repo.guardarConteos(c, tomaId, conteosPorProducto);
            repo.cambiarEstado(c, tomaId, "EN_CONTEO", false);
            return null;
        });
    }

    /** Aplica las diferencias como movimientos de ajuste. */
    public void aplicar(long tomaId) {
        Bd.transaccion(c -> {
            TomaInventario toma = repo.porId(tomaId).orElseThrow(() -> new ValidacionExcepcion("No existe"));
            if (!"EN_CONTEO".equals(toma.estado()) && !"CERRADA".equals(toma.estado())) {
                throw new ValidacionExcepcion("La toma debe estar en conteo para aplicarse");
            }
            int ajustados = 0;
            for (TomaDetalle d : toma.detalles()) {
                BigDecimal dif = d.diferencia() == null ? BigDecimal.ZERO : d.diferencia();
                if (dif.signum() == 0) {
                    continue;
                }
                if (dif.signum() > 0) {
                    inventario.entradaEn(c, toma.bodegaId(), d.productoId(), dif, d.costoUnitario(), "TOMA",
                            "TOMA", tomaId, d.loteId(), null, "Toma " + toma.numero());
                } else {
                    inventario.salidaEn(c, toma.bodegaId(), d.productoId(), dif.negate(), "TOMA", "TOMA", tomaId,
                            d.loteId(), "Toma " + toma.numero());
                }
                ajustados++;
            }
            repo.cambiarEstado(c, tomaId, "APLICADA", true);
            return ajustados;
        });
        auditoria.registrar("INVENTARIO", "toma", tomaId, "AUTORIZAR", null, null);
    }

    private String numero() {
        return "TI-" + System.currentTimeMillis();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
