package ec.sfjfx.ui.vistas.inventario;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de inventario y bodegas. */
public final class VistaInventario extends BorderPane {

    public VistaInventario() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Stock", new VistaStock()),
                tab("Kardex", new VistaKardex()),
                tab("Ajustes", new VistaAjustes()),
                tab("Transferencias", new VistaTransferencias()),
                tab("Tomas", new VistaTomas()),
                tab("Lotes", new VistaLotes()),
                tab("Series", new VistaSeries()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
