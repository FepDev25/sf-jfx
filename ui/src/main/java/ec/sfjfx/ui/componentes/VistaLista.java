package ec.sfjfx.ui.componentes;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ec.sfjfx.core.repositorio.Pagina;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    private final HBox paginador = new HBox(8);
    private final Button anterior = new Button("Anterior");
    private final Button siguiente = new Button("Siguiente");
    private final Label paginaInfo = new Label();
    private final Label totalInfo = new Label();
    private final ComboBox<Integer> tamano = new ComboBox<>();

    private Supplier<List<T>> proveedor = List::of;
    private ProveedorPaginado<T> proveedorPaginado;
    private Function<T, String> textoBusqueda = Object::toString;
    private Runnable alCambiarSeleccion = () -> { };
    private Consumer<T> alDobleClic = t -> { };
    private int paginaActual = 1;
    private int tamanoPagina = 50;

    /** Proveedor de datos paginados en el servidor (filtro, pagina, tamano). */
    @FunctionalInterface
    public interface ProveedorPaginado<T> {
        Pagina<T> cargar(String filtro, int pagina, int tamano);
    }

    public VistaLista(String titulo) {
        getStyleClass().add("contenido");

        Label etiquetaTitulo = new Label(titulo);
        etiquetaTitulo.getStyleClass().add("titulo-seccion");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox lineaTitulo = new HBox(etiquetaTitulo, espacio, acciones);
        lineaTitulo.setAlignment(Pos.CENTER_LEFT);

        busqueda.setPromptText("Buscar... (Ctrl+F)");
        busqueda.getStyleClass().add("campo");
        busqueda.setMaxWidth(320);
        busqueda.setAccessibleText("Buscar en la lista");
        busqueda.setTooltip(new Tooltip("Ctrl+F para enfocar, F5 para actualizar"));
        busqueda.textProperty().addListener((o, a, b) -> {
            if (proveedorPaginado != null) {
                paginaActual = 1;
                refrescar();
            } else {
                aplicarFiltro();
            }
        });

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

        construirPaginador();
        setTop(cabecera);
        setCenter(tabla);
        setBottom(paginador);
        setPadding(new Insets(20));

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F5) {
                refrescar();
                e.consume();
            } else if (e.getCode() == KeyCode.F && e.isControlDown()) {
                busqueda.requestFocus();
                busqueda.selectAll();
                e.consume();
            }
        });
    }

    private void construirPaginador() {
        totalInfo.getStyleClass().add("texto-suave");
        paginaInfo.getStyleClass().add("texto-suave");
        anterior.getStyleClass().add("boton-secundario");
        siguiente.getStyleClass().add("boton-secundario");
        anterior.setAccessibleText("Pagina anterior");
        siguiente.setAccessibleText("Pagina siguiente");
        anterior.setOnAction(e -> {
            if (paginaActual > 1) {
                paginaActual--;
                refrescar();
            }
        });
        siguiente.setOnAction(e -> {
            paginaActual++;
            refrescar();
        });
        tamano.getItems().addAll(25, 50, 100, 200);
        tamano.setValue(tamanoPagina);
        tamano.getStyleClass().add("selector");
        tamano.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                tamanoPagina = b;
                paginaActual = 1;
                refrescar();
            }
        });
        Label porPagina = new Label("Por pagina:");
        porPagina.getStyleClass().add("texto-suave");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        paginador.setAlignment(Pos.CENTER_LEFT);
        paginador.setPadding(new Insets(10, 0, 0, 0));
        paginador.getChildren().addAll(totalInfo, espacio, anterior, paginaInfo, siguiente, porPagina, tamano);
        paginador.setVisible(false);
        paginador.setManaged(false);
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

    /** Activa la paginacion en el servidor para listados grandes. */
    public VistaLista<T> proveedorPaginado(ProveedorPaginado<T> proveedor) {
        this.proveedorPaginado = proveedor;
        tabla.setItems(datos);
        paginaActual = 1;
        paginador.setVisible(true);
        paginador.setManaged(true);
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
        if (proveedorPaginado != null) {
            Pagina<T> pagina = proveedorPaginado.cargar(busqueda.getText(), paginaActual, tamanoPagina);
            datos.setAll(pagina.items());
            paginaActual = pagina.pagina();
            actualizarPaginador(pagina);
        } else {
            datos.setAll(proveedor.get());
            aplicarFiltro();
        }
        if (seleccionado != null) {
            datos.stream().filter(x -> x.equals(seleccionado)).findFirst()
                    .ifPresent(x -> tabla.getSelectionModel().select(x));
        }
    }

    private void actualizarPaginador(Pagina<T> pagina) {
        paginaInfo.setText("Pagina " + pagina.pagina() + " de " + pagina.totalPaginas());
        totalInfo.setText(pagina.total() == 0
                ? "Sin registros"
                : String.format("Mostrando %,d - %,d de %,d", pagina.desde(), pagina.hasta(), pagina.total()));
        anterior.setDisable(!pagina.hayAnterior());
        siguiente.setDisable(!pagina.haySiguiente());
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
