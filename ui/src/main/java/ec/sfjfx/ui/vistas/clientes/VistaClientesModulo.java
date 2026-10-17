package ec.sfjfx.ui.vistas.clientes;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de clientes: clientes, cuentas por cobrar y pedidos. */
public final class VistaClientesModulo extends BorderPane {

    public VistaClientesModulo() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Clientes", new VistaClientes()),
                tab("Cuentas por cobrar", new VistaCxc()),
                tab("Pedidos", new VistaPedidos()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
