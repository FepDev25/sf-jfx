package ec.sfjfx.ui.vistas.catalogo;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de catalogo: productos y catalogos auxiliares. */
public final class VistaCatalogo extends BorderPane {

    public VistaCatalogo() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Productos", new VistaProductos()),
                tab("Categorias", new VistaCategorias()),
                tab("Marcas", new VistaMarcas()),
                tab("Unidades", new VistaUnidades()),
                tab("Impuestos", new VistaImpuestos()),
                tab("Atributos", new VistaAtributos()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
