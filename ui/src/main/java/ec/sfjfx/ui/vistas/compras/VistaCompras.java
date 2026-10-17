package ec.sfjfx.ui.vistas.compras;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de compras: ordenes, recepciones, facturas, CxP, retenciones y devoluciones. */
public final class VistaCompras extends BorderPane {

    public VistaCompras() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Solicitudes", new VistaSolicitudes()),
                tab("Ordenes", new VistaOrdenes()),
                tab("Recepciones", new VistaRecepciones()),
                tab("Facturas", new VistaFacturas()),
                tab("Cuentas por pagar", new VistaCuentasPorPagar()),
                tab("Retenciones", new VistaRetenciones()),
                tab("Devoluciones", new VistaDevoluciones()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
