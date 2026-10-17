package ec.sfjfx.ui.vistas.admin;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Configuracion: organizacion, parametros y modulos. */
public final class VistaConfiguracion extends BorderPane {

    public VistaConfiguracion() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Empresa", new VistaEmpresa()),
                tab("Sucursales", new VistaSucursales()),
                tab("Bodegas", new VistaBodegas()),
                tab("Cajas", new VistaCajas()),
                tab("Dispositivos", new VistaDispositivos()),
                tab("Parametros", new VistaParametros()),
                tab("Modulos", new VistaModulos()),
                tab("Importaciones", new VistaImportaciones()),
                tab("Respaldos", new VistaRespaldos()),
                tab("Migraciones", new VistaMigraciones()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, javafx.scene.Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }
}
