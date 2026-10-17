package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.DevolucionCompra;
import ec.sfjfx.core.modelo.DevolucionCompraDetalle;
import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.servicio.ServicioDevolucionCompra;
import ec.sfjfx.core.servicio.ServicioFacturaProveedor;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaDevoluciones extends VistaLista<DevolucionCompra> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioDevolucionCompra servicio = new ServicioDevolucionCompra();
    private final ServicioFacturaProveedor facturas = new ServicioFacturaProveedor();

    public VistaDevoluciones() {
        super("Devoluciones a proveedor");
        columna("Numero", 150, DevolucionCompra::numero);
        columna("Proveedor", 220, DevolucionCompra::proveedorNombre);
        columna("Bodega", 150, DevolucionCompra::bodega);
        columna("Motivo", 120, DevolucionCompra::motivo);
        columna("Total", 110, d -> valor(d.total()));
        columna("Estado", 100, DevolucionCompra::estado);
        buscarPor(d -> d.numero() + " " + d.proveedorNombre());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Registrar", true, v -> registrar());
        }
        refrescar();
    }

    private void registrar() {
        ComboBox<Proveedor> proveedor = Combos.proveedores();
        ComboBox<FacturaProveedor> factura = new ComboBox<>();
        factura.getStyleClass().add("selector");
        factura.setMaxWidth(Double.MAX_VALUE);
        factura.setPromptText("(opcional)");
        Runnable cargarFacturas = () -> {
            factura.getItems().clear();
            if (proveedor.getValue() != null) {
                factura.getItems().setAll(facturas.listar(null).stream()
                        .filter(f -> f.proveedorId() == proveedor.getValue().id()).toList());
            }
        };
        proveedor.valueProperty().addListener((o, a, b) -> cargarFacturas.run());
        cargarFacturas.run();

        ComboBox<Bodega> bodega = Combos.bodegas();
        ComboBox<String> motivo = Fx.combo("DANO", "VENCIMIENTO", "ERROR", "OTRO");
        TextField nota = Fx.campo("");

        ObservableList<DevolucionCompraDetalle> items = FXCollections.observableArrayList();
        TableView<DevolucionCompraDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(180);
        TableColumn<DevolucionCompraDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(240);
        TableColumn<DevolucionCompraDetalle, String> cC = new TableColumn<>("Cantidad");
        cC.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().cantidad())));
        cC.setPrefWidth(100);
        tabla.getColumns().addAll(cP, cC);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(220);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(80);
        TextField costo = Fx.campo("");
        costo.setPrefWidth(90);
        costo.setPromptText("costo");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p != null) {
                items.add(new DevolucionCompraDetalle(null, 0, p.id(), p.nombre(), null, decimal(Fx.texto(cantidad)),
                        decimal(Fx.texto(costo)), BigDecimal.ZERO));
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, producto, cantidad, costo, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barra);
        itemsBox.setPadding(new Insets(4));

        new DialogoFormulario("Devolucion a proveedor", ventana())
                .agregar("Proveedor", proveedor)
                .agregar("Factura", factura)
                .agregar("Bodega", bodega)
                .agregar("Motivo", motivo)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    Long facturaId = factura.getValue() == null ? null : factura.getValue().id();
                    servicio.registrar(proveedor.getValue().id(), facturaId, bodega.getValue().id(), motivo.getValue(),
                            Fx.nuloSiVacio(Fx.texto(nota)), new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String valor(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
