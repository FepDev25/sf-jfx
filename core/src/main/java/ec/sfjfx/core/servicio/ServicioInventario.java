package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Lote;
import ec.sfjfx.core.modelo.MovimientoSolicitud;
import ec.sfjfx.core.modelo.MovimientoVista;
import ec.sfjfx.core.modelo.Serie;
import ec.sfjfx.core.modelo.StockItem;
import ec.sfjfx.core.repositorio.HistorialPrecioRepositorio;
import ec.sfjfx.core.repositorio.InventarioRepositorio;
import ec.sfjfx.core.repositorio.LoteRepositorio;
import ec.sfjfx.core.repositorio.SerieRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/**
 * Nucleo de inventario: aplica movimientos actualizando stock y kardex.
 * El costeo es Promedio Ponderado (PPP) al ingresar mercaderia.
 */
public final class ServicioInventario {

    private final InventarioRepositorio repo = new InventarioRepositorio();
    private final LoteRepositorio lotes = new LoteRepositorio();
    private final SerieRepositorio series = new SerieRepositorio();
    private final HistorialPrecioRepositorio historial = new HistorialPrecioRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    // ------------------------------------------------------------------ consultas

    public List<StockItem> stock(Long bodegaId, String filtro) {
        return repo.stock(empresa(), bodegaId, filtro);
    }

    public List<StockItem> stockProducto(long productoId) {
        return repo.stockPorProducto(productoId);
    }

    public BigDecimal valorInventario() {
        return repo.valorInventario(empresa());
    }

    public List<MovimientoVista> kardex(long productoId, Long bodegaId, int limite) {
        return repo.kardex(productoId, bodegaId, limite);
    }

    public List<Lote> lotes(String filtro) {
        return lotes.listar(empresa(), filtro);
    }

    public List<Serie> series(String filtro) {
        return series.listar(empresa(), filtro);
    }

    // ------------------------------------------------------------------ movimientos (API)

    public void entrada(long bodegaId, long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
                        String tipoMovimiento, String referenciaTipo, Long referenciaId, Long loteId, String nota) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ValidacionExcepcion("La cantidad de entrada debe ser mayor a cero");
        }
        Bd.transaccion(c -> {
            entradaEn(c, bodegaId, productoId, cantidad, costoUnitario, tipoMovimiento, referenciaTipo,
                    referenciaId, loteId, null, nota);
            return null;
        });
        auditar(productoId, cantidad, tipoMovimiento);
    }

    public void salida(long bodegaId, long productoId, BigDecimal cantidad, String tipoMovimiento,
                       String referenciaTipo, Long referenciaId, Long loteId, String nota) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ValidacionExcepcion("La cantidad de salida debe ser mayor a cero");
        }
        Bd.transaccion(c -> {
            salidaEn(c, bodegaId, productoId, cantidad, tipoMovimiento, referenciaTipo, referenciaId, loteId, nota);
            return null;
        });
        auditar(productoId, cantidad.negate(), tipoMovimiento);
    }

    // ------------------------------------------------------------------ movimientos (en transaccion)

    public void entradaEn(Connection c, long bodegaId, long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
                          String tipoMovimiento, String referenciaTipo, Long referenciaId, Long loteId,
                          Long serieId, String nota) throws SQLException {
        BigDecimal total = repo.stockTotal(c, productoId);
        BigDecimal promedio = repo.costoPromedio(c, productoId);
        BigDecimal costo = costoUnitario != null ? costoUnitario : promedio;
        BigDecimal nuevoPromedio = promedioPonderado(total, promedio, cantidad, costo);

        if (costoUnitario != null && nuevoPromedio.compareTo(promedio) != 0) {
            historial.registrar(c, empresa(), productoId, null, "COSTO", promedio, nuevoPromedio, null, usuario());
        }
        repo.actualizarCosto(c, productoId, nuevoPromedio, costo);

        MovimientoSolicitud m = new MovimientoSolicitud(empresa(), repo.sucursalDeBodega(c, bodegaId), bodegaId,
                productoId, loteId, serieId, tipoMovimiento, cantidad, costo, referenciaTipo, referenciaId,
                usuario(), nota);
        repo.aplicar(c, m, nuevoPromedio);
    }

    public void salidaEn(Connection c, long bodegaId, long productoId, BigDecimal cantidad, String tipoMovimiento,
                         String referenciaTipo, Long referenciaId, Long loteId, String nota) throws SQLException {
        BigDecimal promedio = repo.costoPromedio(c, productoId);
        MovimientoSolicitud m = new MovimientoSolicitud(empresa(), repo.sucursalDeBodega(c, bodegaId), bodegaId,
                productoId, loteId, null, tipoMovimiento, cantidad.negate(), promedio, referenciaTipo, referenciaId,
                usuario(), nota);
        repo.aplicar(c, m, promedio);
    }

    // ------------------------------------------------------------------ helpers

    private BigDecimal promedioPonderado(BigDecimal totalAnterior, BigDecimal promedio, BigDecimal entrada,
                                         BigDecimal costoEntrada) {
        BigDecimal totalNuevo = totalAnterior.add(entrada);
        if (totalNuevo.signum() <= 0) {
            return costoEntrada;
        }
        BigDecimal valorAnterior = totalAnterior.multiply(promedio);
        BigDecimal valorNuevo = entrada.multiply(costoEntrada);
        return valorAnterior.add(valorNuevo).divide(totalNuevo, 4, RoundingMode.HALF_UP);
    }

    private void auditar(long productoId, BigDecimal cantidad, String tipo) {
        auditoria.registrar("INVENTARIO", "movimiento", productoId, "CREAR", null,
                Map.of("producto", productoId, "cantidad", cantidad.toPlainString(), "tipo", tipo));
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
