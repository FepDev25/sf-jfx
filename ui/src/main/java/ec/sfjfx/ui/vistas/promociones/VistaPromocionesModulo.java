package ec.sfjfx.ui.vistas.promociones;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de promociones y cupones. */
public final class VistaPromocionesModulo extends BorderPane {

    public VistaPromocionesModulo() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(tab("Promociones", new VistaPromociones()), tab("Cupones", new VistaCupones()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
