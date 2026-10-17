package ec.sfjfx.ui.vistas.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.GestorModulos;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public final class VistaModulos extends ScrollPane {

    public VistaModulos() {
        getStyleClass().add("contenido");
        setFitToWidth(true);

        long empresaId = SesionActual.requerida().empresa().id();
        Map<String, Boolean> estado = new GestorModulos().estado(empresaId);
        boolean editable = Perm.tiene("configuracion.editar") || Perm.tiene("configuracion.crear");

        VBox raiz = new VBox(10);
        raiz.setPadding(new Insets(20));

        Label titulo = new Label("Modulos del sistema");
        titulo.getStyleClass().add("titulo-seccion");
        Label ayuda = new Label("Active o desactive modulos para esta empresa. El menu se adapta al guardar sesion nueva.");
        ayuda.getStyleClass().add("texto-suave");
        raiz.getChildren().addAll(titulo, ayuda);

        Map<String, FlowPane> porGrupo = new LinkedHashMap<>();
        VBox contenedorGrupos = new VBox(14);

        for (ModuloSistema m : ModuloSistema.values()) {
            FlowPane grupo = porGrupo.computeIfAbsent(m.grupo(), g -> {
                Label etiqueta = new Label(g);
                etiqueta.getStyleClass().add("etiqueta-form");
                FlowPane pane = new FlowPane(18, 8);
                VBox bloque = new VBox(4, etiqueta, pane);
                contenedorGrupos.getChildren().add(bloque);
                return pane;
            });

            CheckBox check = new CheckBox(m.etiqueta());
            check.setSelected(estado.getOrDefault(m.codigo(), false));
            check.setDisable(!editable);
            check.selectedProperty().addListener((o, anterior, nuevo) -> {
                new GestorModulos().ajustar(empresaId, m, nuevo);
            });
            grupo.getChildren().add(check);
        }

        raiz.getChildren().add(contenedorGrupos);
        setContent(raiz);
    }
}
