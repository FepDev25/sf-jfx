package ec.sfjfx.ui.componentes;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Lista generica: titulo, acciones, buscador y tabla. */
public class VistaLista<T> extends BorderPane {

    private final TableView<T> tabla = new TableView<>();
    private final ObservableList<T> datos = FXCollections.observableArrayList();
    private final FilteredList<T> filtrados = new FilteredList<>(datos, p -> true);
    private final HBox acciones = new HBox(8);
    private final TextField busqueda = new TextField();
    private final VBox cabecera = new VBox(10);

    private Supplier<List<T>> proveedor = List::of;
    private Function<T, String> textoBusqueda = Object::toString;
    private Runnable alCambiarSeleccion = () -> { };
    private Consumer<T> alDobleClic = t -> { };

    public VistaLista(String titulo) {
        getStyleClass().add("contenido");

        Label etiquetaTitulo = new Label(titulo);
        etiquetaTitulo.getStyleClass().add("titulo-seccion");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox lineaTitulo = new HBox(etiquetaTitulo, espacio, acciones);
        lineaTitulo.setAlignment(Pos.CENTER_LEFT);

        busqueda.setPromptText("Buscar...");
        busqueda.getStyleClass().add("campo");
        busqueda.setMaxWidth(320);
        busqueda.textProperty().addListener((o, a, b) -> aplicarFiltro());

        cabecera.getChildren().addAll(lineaTitulo, busqueda);
        cabecera.setPadding(new Insets(4, 0, 8, 0));

        tabla.setItems(filtrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin registros"));
        tabla.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> alCambiarSeleccion.run());
        tabla.setRowFactory(tv -> {
            var fila = new javafx.scene.control.TableRow<T>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && fila.getItem() != null) {
                    alDobleClic.accept(fila.getItem());
                }
            });
            return fila;
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        setTop(cabecera);
        setCenter(tabla);
        setPadding(new Insets(20));
    }

    public VistaLista<T> columna(String titulo, double ancho, Function<T, Object> valor) {
        TableColumn<T, Object> col = new TableColumn<>(titulo);
        col.setPrefWidth(ancho);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        tabla.getColumns().add(col);
        return this;
    }

    public VistaLista<T> accion(String texto, boolean primario, Consumer<VistaLista<T>> accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(primario ? "boton-primario" : "boton-secundario");
        boton.setOnAction(e -> accion.accept(this));
        acciones.getChildren().add(boton);
        return this;
    }

    public VistaLista<T> proveedor(Supplier<List<T>> proveedor) {
        this.proveedor = proveedor;
        return this;
    }

    public VistaLista<T> buscarPor(Function<T, String> textoBusqueda) {
        this.textoBusqueda = textoBusqueda;
        return this;
    }

    public VistaLista<T> onSeleccion(Runnable accion) {
        this.alCambiarSeleccion = accion;
        return this;
    }

    public VistaLista<T> onDobleClic(Consumer<T> accion) {
        this.alDobleClic = accion;
        return this;
    }

    public void refrescar() {
        T seleccionado = seleccion().orElse(null);
        datos.setAll(proveedor.get());
        aplicarFiltro();
        if (seleccionado != null) {
            datos.stream().filter(x -> x.equals(seleccionado)).findFirst()
                    .ifPresent(x -> tabla.getSelectionModel().select(x));
        }
    }

    public Optional<T> seleccion() {
        return Optional.ofNullable(tabla.getSelectionModel().getSelectedItem());
    }

    public TableView<T> tabla() {
        return tabla;
    }

    private void aplicarFiltro() {
        String texto = busqueda.getText();
        filtrados.setPredicate(t -> texto == null || texto.isBlank()
                || textoBusqueda.apply(t).toLowerCase().contains(texto.toLowerCase()));
    }
}
