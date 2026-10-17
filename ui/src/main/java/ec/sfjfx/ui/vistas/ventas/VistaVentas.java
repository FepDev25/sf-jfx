package ec.sfjfx.ui.vistas.ventas;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Modulo de ventas: punto de venta, consulta, sesiones de caja y devoluciones. */
public final class VistaVentas extends BorderPane {

    public VistaVentas() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Punto de venta", new VistaPos()),
                tab("Ventas", new VistaVentasConsulta()),
                tab("Sesiones de caja", new VistaSesionesCaja()),
                tab("Devoluciones", new VistaDevolucionesVenta()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
