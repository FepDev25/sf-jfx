package ec.sfjfx.ui.vistas.inventario;

import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.modelo.MovimientoVista;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.servicio.ServicioInventario;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public final class VistaKardex extends BorderPane {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioInventario servicio = new ServicioInventario();
    private final ComboBox<Producto> producto = Combos.productos();
    private final TableView<MovimientoVista> tabla = new TableView<>();

    public VistaKardex() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));

        Label titulo = new Label("Kardex");
        titulo.getStyleClass().add("titulo-seccion");
        producto.setPrefWidth(360);
        HBox barra = new HBox(10, titulo, new Label("Producto:"), producto);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(0, 0, 10, 0));
        producto.valueProperty().addListener((o, a, b) -> cargar());

        tabla.getColumns().addAll(
                col("Fecha", 150, m -> m.fecha() == null ? "" : m.fecha().format(FORMATO)),
                col("Bodega", 140, MovimientoVista::bodega),
                col("Tipo", 160, MovimientoVista::tipo),
                col("Cantidad", 100, m -> valor(m.cantidad())),
                col("Costo unit.", 110, m -> valor(m.costoUnitario())),
                col("Saldo", 110, m -> valor(m.saldoCantidad())),
                col("Referencia", 160, MovimientoVista::referencia),
                col("Usuario", 120, m -> m.usuario() == null ? "sistema" : m.usuario()),
                col("Nota", 200, MovimientoVista::nota));

        setTop(barra);
        setCenter(tabla);
        if (!producto.getItems().isEmpty()) {
            producto.setValue(producto.getItems().get(0));
        }
    }

    private void cargar() {
        if (producto.getValue() == null) {
            tabla.getItems().clear();
            return;
        }
        tabla.getItems().setAll(servicio.kardex(producto.getValue().id(), null, 300));
    }

    private TableColumn<MovimientoVista, String> col(String titulo, double ancho,
                                                     java.util.function.Function<MovimientoVista, String> valor) {
        TableColumn<MovimientoVista, String> c = new TableColumn<>(titulo);
        c.setPrefWidth(ancho);
        c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(valor.apply(v.getValue())));
        return c;
    }

    private String valor(java.math.BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
