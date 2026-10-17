package ec.sfjfx.ui.vistas.inventario;

import java.math.BigDecimal;

import ec.sfjfx.core.modelo.StockItem;
import ec.sfjfx.core.servicio.ServicioInventario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public final class VistaStock extends VistaLista<StockItem> {

    private final ServicioInventario servicio = new ServicioInventario();

    public VistaStock() {
        super("Existencias (stock por bodega)");
        columna("Codigo", 120, StockItem::codigo);
        columna("Producto", 260, StockItem::producto);
        columna("Bodega", 150, StockItem::bodega);
        columna("Lote", 110, s -> s.lote() == null ? "-" : s.lote());
        columna("Cantidad", 100, s -> valor(s.cantidad()));
        columna("Reservada", 100, s -> valor(s.reservada()));
        columna("Disponible", 100, s -> valor(s.disponible()));
        columna("Costo prom.", 110, s -> valor(s.costoPromedio()));
        columna("Valor", 120, s -> valor(s.valor()));
        buscarPor(s -> s.producto() + " " + s.codigo() + " " + s.bodega());
        proveedor(() -> servicio.stock(null, null));

        accion("Valor inventario", false, v -> Alertas.info("Inventario",
                "Valor total valorizado (PPP): $ " + servicio.valorInventario().toPlainString()));
        accion("Refrescar", false, v -> refrescar());
        refrescar();
    }

    private String valor(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
