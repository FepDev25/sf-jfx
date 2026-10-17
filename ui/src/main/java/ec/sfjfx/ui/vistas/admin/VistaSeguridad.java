package ec.sfjfx.ui.vistas.admin;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Seguridad: usuarios y roles/permisos. */
public final class VistaSeguridad extends BorderPane {

    public VistaSeguridad() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Usuarios", new VistaUsuarios()),
                tab("Roles y permisos", new VistaRoles()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
