package ec.sfjfx.ui.vistas.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.modulos.PerfilInstalacion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.GestorModulos;
import ec.sfjfx.core.servicio.ServicioPerfiles;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class VistaModulos extends ScrollPane {

    private final long empresaId = SesionActual.requerida().empresa().id();
    private final GestorModulos gestor = new GestorModulos();
    private final ServicioPerfiles perfiles = new ServicioPerfiles();
    private final Map<ModuloSistema, CheckBox> checks = new LinkedHashMap<>();
    private final ComboBox<PerfilInstalacion> perfil = new ComboBox<>();

    public VistaModulos() {
        getStyleClass().add("contenido");
        setFitToWidth(true);

        VBox raiz = new VBox(10);
        raiz.setPadding(new Insets(20));

        Label titulo = new Label("Modulos y perfiles del sistema");
        titulo.getStyleClass().add("titulo-seccion");
        Label ayuda = new Label("Active o desactive modulos para esta empresa, o aplique un perfil de "
                + "instalacion completo. El menu se adapta al iniciar una sesion nueva.");
        ayuda.getStyleClass().add("texto-suave");
        ayuda.setWrapText(true);

        boolean editable = Perm.tiene("configuracion.editar") || Perm.tiene("configuracion.crear");
        perfil.getItems().setAll(PerfilInstalacion.values());
        perfil.setValue(perfiles.perfilActual());
        perfil.getStyleClass().add("selector");
        perfil.setPrefWidth(200);
        perfil.setDisable(!editable);

        Button aplicar = new Button("Aplicar perfil");
        aplicar.getStyleClass().add("boton-secundario");
        aplicar.setDisable(!editable);
        aplicar.setOnAction(e -> aplicarPerfil());

        HBox filaPerfil = new HBox(10, perfil, aplicar);
        filaPerfil.setAlignment(Pos.CENTER_LEFT);

        Label etiquetaPerfil = new Label("Perfil de instalacion");
        etiquetaPerfil.getStyleClass().add("etiqueta-form");
        VBox bloquePerfil = new VBox(4, etiquetaPerfil, filaPerfil);
        bloquePerfil.setPadding(new Insets(6, 0, 8, 0));

        raiz.getChildren().addAll(titulo, ayuda, bloquePerfil);

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
            check.setDisable(!editable);
            check.selectedProperty().addListener((o, anterior, nuevo) -> {
                gestor.ajustar(empresaId, m, nuevo);
            });
            checks.put(m, check);
            grupo.getChildren().add(check);
        }

        raiz.getChildren().add(contenedorGrupos);
        setContent(raiz);
        refrescarChecks();
    }

    private void aplicarPerfil() {
        PerfilInstalacion seleccionado = perfil.getValue();
        if (seleccionado == null) {
            Alertas.advertencia("Perfil", "Seleccione un perfil de instalacion.");
            return;
        }
        if (!Alertas.confirmar("Aplicar perfil",
                "Se activaran y desactivaran los modulos segun el perfil \"" + seleccionado.etiqueta()
                + "\". ¿Desea continuar?")) {
            return;
        }
        try {
            perfiles.aplicar(seleccionado);
            refrescarChecks();
            Alertas.info("Perfil aplicado",
                    "El perfil " + seleccionado.etiqueta() + " fue aplicado. Inicie una sesion nueva "
                    + "para ver el menu actualizado.");
        } catch (RuntimeException e) {
            Alertas.error("No se pudo aplicar el perfil", e.getMessage());
        }
    }

    private void refrescarChecks() {
        Map<String, Boolean> estado = gestor.estado(empresaId);
        for (Map.Entry<ModuloSistema, CheckBox> entrada : checks.entrySet()) {
            boolean activo = estado.getOrDefault(entrada.getKey().codigo(), false);
            if (entrada.getValue().isSelected() != activo) {
                entrada.getValue().setSelected(activo);
            }
        }
    }
}
