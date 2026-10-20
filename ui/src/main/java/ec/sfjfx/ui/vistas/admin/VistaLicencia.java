package ec.sfjfx.ui.vistas.admin;

import java.io.File;

import ec.sfjfx.core.servicio.ServicioLicencia;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Licencia perpetua + mantenimiento anual, con carga offline. */
public final class VistaLicencia extends BorderPane {

    private final ServicioLicencia servicio = new ServicioLicencia();

    private final Label estado = valor();
    private final Label cliente = valor();
    private final Label edicion = valor();
    private final Label limites = valor();
    private final Label mantenimiento = valor();
    private final Label codigo = valor();
    private final Label error = new Label();
    private final Label archivo = new Label();

    public VistaLicencia() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));

        Label titulo = new Label("Licencia y mantenimiento");
        titulo.getStyleClass().add("titulo-seccion");
        Label subtitulo = new Label("Licencia perpetua con mantenimiento anual. "
                + "La validacion es local y no requiere internet.");
        subtitulo.getStyleClass().add("texto-suave");
        subtitulo.setWrapText(true);

        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("boton-secundario");
        actualizar.setOnAction(e -> cargar());
        Button copiar = new Button("Copiar codigo");
        copiar.getStyleClass().add("boton-secundario");
        copiar.setOnAction(e -> copiarCodigo());
        Button cargar = new Button("Cargar licencia");
        cargar.getStyleClass().add("boton-primario");
        cargar.setOnAction(e -> cargarLicencia());
        cargar.setDisable(!Perm.tiene("configuracion.editar"));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, espacio, copiar, actualizar, cargar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.getStyleClass().add("panel-tarjeta");
        int fila = 0;
        fila = agregar(grid, fila, "Estado", estado);
        fila = agregar(grid, fila, "Cliente", cliente);
        fila = agregar(grid, fila, "Edicion", edicion);
        fila = agregar(grid, fila, "Limites", limites);
        fila = agregar(grid, fila, "Mantenimiento hasta", mantenimiento);
        fila = agregar(grid, fila, "Codigo de instalacion", codigo);
        agregar(grid, fila, "Archivo", archivo);

        error.getStyleClass().add("wizard-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox contenido = new VBox(10, titulo, subtitulo, acciones, grid, error);
        setCenter(contenido);
        cargar();
    }

    private void cargar() {
        try {
            ServicioLicencia.EstadoLicencia licencia = servicio.estado();
            estado.setText(licencia.mensaje());
            cliente.setText(licencia.cliente() == null ? "-" : licencia.cliente());
            edicion.setText(licencia.edicion() == null ? "-" : licencia.edicion());
            limites.setText(licencia.valida()
                    ? licencia.maxSucursales() + " sucursal(es) · " + licencia.maxCajas() + " caja(s)"
                    : "-");
            mantenimiento.setText(licencia.mantenimientoHasta() == null
                    ? "-" : licencia.mantenimientoHasta().toString()
                    + (licencia.mantenimientoVigente() ? " (vigente)" : " (vencido)"));
            codigo.setText(servicio.codigoInstalacion().isBlank() ? "-" : servicio.codigoInstalacion());
            archivo.setText(servicio.archivo().toString());
            error.setVisible(false);
            error.setManaged(false);
        } catch (RuntimeException e) {
            error.setText("No se pudo consultar la licencia: " + e.getMessage());
            error.setVisible(true);
            error.setManaged(true);
        }
    }

    private void cargarLicencia() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar licencia");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Licencia sf-jfx", "*.properties", "*.lic", "*.*"));
        File archivo = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            ServicioLicencia.EstadoLicencia licencia = servicio.instalar(archivo.toPath());
            cargar();
            Alertas.info("Licencia instalada", licencia.mensaje());
        } catch (RuntimeException e) {
            Alertas.error("Licencia no valida", e.getMessage());
        }
    }

    private void copiarCodigo() {
        if (codigo.getText() == null || codigo.getText().isBlank() || "-".equals(codigo.getText())) {
            Alertas.advertencia("Codigo de instalacion", "No hay un codigo disponible.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(codigo.getText());
        Clipboard.getSystemClipboard().setContent(content);
        Alertas.info("Codigo copiado", "El codigo de instalacion fue copiado al portapapeles.");
    }

    private static Label valor() {
        Label label = new Label("-");
        label.getStyleClass().add("panel-titulo");
        label.setWrapText(true);
        return label;
    }

    private static int agregar(GridPane grid, int fila, String titulo, Label valor) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("etiqueta-form");
        grid.add(etiqueta, 0, fila);
        grid.add(valor, 1, fila);
        GridPane.setHgrow(valor, Priority.ALWAYS);
        return fila + 1;
    }
}
