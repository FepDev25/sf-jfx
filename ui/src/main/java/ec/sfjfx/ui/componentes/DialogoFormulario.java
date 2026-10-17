package ec.sfjfx.ui.componentes;

import ec.sfjfx.ui.util.Alertas;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Dialogo modal para formularios. Ejecuta la validacion antes de cerrar. */
public final class DialogoFormulario {

    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final GridPane grilla = new GridPane();
    private final VBox raiz = new VBox(12, grilla);
    private Runnable alGuardar = () -> { };
    private int fila = 0;

    public DialogoFormulario(String titulo, Window propietario) {
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, cancelar);

        grilla.setHgap(12);
        grilla.setVgap(10);
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(140);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        grilla.getColumnConstraints().addAll(etiquetas, campos);

        raiz.setPadding(new Insets(18, 18, 6, 18));
        ScrollPane scroll = new ScrollPane(raiz);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        scroll.setStyle("-fx-background-color: transparent;");
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().setPrefWidth(560);

        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                alGuardar.run();
            } catch (RuntimeException e) {
                Alertas.error("No se pudo guardar", e.getMessage());
                evento.consume();
            }
        });
    }

    public DialogoFormulario agregar(String etiqueta, Node control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("etiqueta-form");
        grilla.add(label, 0, fila);
        grilla.add(control, 1, fila);
        fila++;
        return this;
    }

    public DialogoFormulario agregarAnchoCompleto(Node control) {
        grilla.add(control, 0, fila, 2, 1);
        fila++;
        return this;
    }

    public DialogoFormulario alGuardar(Runnable accion) {
        this.alGuardar = accion;
        return this;
    }

    public void mostrar() {
        dialogo.showAndWait();
    }
}
