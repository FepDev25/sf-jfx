package ec.sfjfx.ui.vistas.admin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import ec.sfjfx.core.servicio.ServicioImportacion;
import ec.sfjfx.core.servicio.TipoImportacion;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Importacion masiva de clientes y proveedores con vista previa y validacion. */
public final class VistaImportaciones extends BorderPane {

    private final ServicioImportacion servicio = new ServicioImportacion();
    private final ComboBox<TipoImportacion> tipo = new ComboBox<>();
    private final TextField archivo = new TextField();
    private final TableView<ObservableList<String>> tabla = new TableView<>();
    private final ListView<String> errores = new ListView<>();
    private final Label resumen = new Label();
    private final Button importar = new Button("Importar");

    private Path seleccionado;
    private ServicioImportacion.Analisis analisis;

    public VistaImportaciones() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));
        setTop(construirFormulario());
        setCenter(construirVistaPrevia());
    }

    private VBox construirFormulario() {
        Label titulo = new Label("Importaciones masivas");
        titulo.getStyleClass().add("titulo-seccion");
        Label ayuda = new Label("Cargue un archivo CSV o Excel. El catalogo de productos tiene su propio "
                + "importador en el modulo Productos.");
        ayuda.getStyleClass().add("texto-suave");
        ayuda.setWrapText(true);

        tipo.getItems().setAll(TipoImportacion.values());
        tipo.setValue(TipoImportacion.CLIENTES);
        tipo.getStyleClass().add("selector");
        tipo.valueProperty().addListener((o, a, b) -> limpiar());

        archivo.setEditable(false);
        archivo.getStyleClass().add("campo");
        HBox.setHgrow(archivo, Priority.ALWAYS);

        Button examinar = new Button("Examinar...");
        examinar.getStyleClass().add("boton-secundario");
        examinar.setOnAction(e -> examinar());

        Button plantilla = new Button("Descargar plantilla");
        plantilla.getStyleClass().add("boton-secundario");
        plantilla.setOnAction(e -> descargarPlantilla());

        Button previsualizar = new Button("Previsualizar");
        previsualizar.getStyleClass().add("boton-secundario");
        previsualizar.setOnAction(e -> previsualizar());

        importar.getStyleClass().add("boton-primario");
        importar.setDisable(true);
        importar.setOnAction(e -> importar());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox seleccion = new HBox(8, campo("Tipo", tipo), archivo, examinar, plantilla);
        seleccion.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(archivo, Priority.ALWAYS);
        HBox acciones = new HBox(8, previsualizar, importar, espacio);
        acciones.setAlignment(Pos.CENTER_LEFT);

        boolean puedeImportar = Perm.tiene("configuracion.crear") || Perm.tiene("configuracion.editar");
        seleccion.setDisable(!puedeImportar);
        acciones.setDisable(!puedeImportar);

        resumen.getStyleClass().add("texto-suave");
        errores.setPrefHeight(90);
        errores.setPlaceholder(new Label("Sin errores de validacion"));

        VBox caja = new VBox(8, titulo, ayuda, seleccion, acciones, resumen);
        caja.setPadding(new Insets(0, 0, 12, 0));
        return caja;
    }

    private VBox construirVistaPrevia() {
        HBox.setHgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(errores, Priority.NEVER);

        Label tituloErrores = new Label("Errores");
        tituloErrores.getStyleClass().add("etiqueta-form");
        VBox caja = new VBox(6, tabla, tituloErrores, errores);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return caja;
    }

    private void examinar() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar archivo de importacion");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV o Excel", "*.csv", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File f = selector.showOpenDialog(ventana());
        if (f != null) {
            seleccionado = f.toPath();
            archivo.setText(seleccionado.toString());
            limpiarAnalisis();
        }
    }

    private void previsualizar() {
        if (seleccionado == null) {
            Alertas.advertencia("Previsualizar", "Seleccione un archivo primero.");
            return;
        }
        try {
            analisis = servicio.analizar(tipo.getValue(), seleccionado);
            mostrar(analisis);
            importar.setDisable(analisis.filas().isEmpty());
        } catch (Exception e) {
            limpiar();
            Alertas.error("No se pudo leer el archivo", e.getMessage());
        }
    }

    private void mostrar(ServicioImportacion.Analisis datos) {
        tabla.getColumns().clear();
        String[] cabecera = datos.cabecera();
        for (int i = 0; i < cabecera.length; i++) {
            final int indice = i;
            TableColumn<ObservableList<String>, String> columna =
                    new TableColumn<>(cabecera[i] == null ? "Columna " + (i + 1) : cabecera[i]);
            columna.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                    indice < d.getValue().size() ? d.getValue().get(indice) : ""));
            columna.setPrefWidth(140);
            tabla.getColumns().add(columna);
        }
        ObservableList<ObservableList<String>> filas = FXCollections.observableArrayList();
        for (String[] fila : datos.filas()) {
            filas.add(FXCollections.observableArrayList(fila));
        }
        tabla.setItems(filas);
        tabla.setPlaceholder(new Label("Sin filas para mostrar"));
        errores.setItems(FXCollections.observableArrayList(datos.errores()));
        resumen.setText(filas.size() + " fila(s) listas para importar. Se muestran todas en la vista previa.");
    }

    private void importar() {
        if (analisis == null || seleccionado == null) {
            return;
        }
        if (!Alertas.confirmar("Importar",
                "¿Importar " + analisis.filas().size() + " fila(s) de " + tipo.getValue().etiqueta() + "?")) {
            return;
        }
        try {
            ServicioImportacion.Resultado resultado = servicio.importar(tipo.getValue(), seleccionado);
            refrescar();
            String mensaje = "Creados: " + resultado.creados() + "\nOmitidos: " + resultado.omitidos()
                    + "\nErrores: " + resultado.errores().size();
            if (!resultado.errores().isEmpty()) {
                errores.setItems(FXCollections.observableArrayList(resultado.errores()));
            }
            Alertas.info("Importacion completada", mensaje);
        } catch (Exception e) {
            Alertas.error("No se pudo importar", e.getMessage());
        }
    }

    private void refrescar() {
        if (analisis != null) {
            previsualizar();
        }
    }

    private void descargarPlantilla() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar plantilla");
        selector.setInitialFileName("plantilla-" + tipo.getValue().name().toLowerCase() + ".csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = selector.showSaveDialog(ventana());
        if (destino == null) {
            return;
        }
        try {
            servicio.plantilla(tipo.getValue(), destino.toPath());
            Alertas.info("Plantilla", "Plantilla guardada en " + destino.getAbsolutePath());
        } catch (Exception e) {
            Alertas.error("No se pudo guardar la plantilla", e.getMessage());
        }
    }

    private void limpiar() {
        seleccionado = null;
        archivo.clear();
        limpiarAnalisis();
    }

    private void limpiarAnalisis() {
        analisis = null;
        tabla.getColumns().clear();
        tabla.getItems().clear();
        errores.getItems().clear();
        resumen.setText("");
        importar.setDisable(true);
    }

    private static VBox campo(String etiqueta, javafx.scene.control.Control control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("etiqueta-form");
        control.setMaxWidth(200);
        return new VBox(3, label, control);
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
