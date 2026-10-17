package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.OrdenCompra;
import ec.sfjfx.core.modelo.OrdenCompraDetalle;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.servicio.ServicioOrdenCompra;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaOrdenes extends VistaLista<OrdenCompra> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ServicioOrdenCompra servicio = new ServicioOrdenCompra();

    public VistaOrdenes() {
        super("Ordenes de compra");
        columna("Numero", 150, OrdenCompra::numero);
        columna("Proveedor", 240, OrdenCompra::proveedorNombre);
        columna("Estado", 140, OrdenCompra::estado);
        columna("Entrega", 110, o -> o.fechaEntregaEsperada() == null ? "-" : o.fechaEntregaEsperada().format(FORMATO));
        columna("Total", 110, o -> o.total() == null ? "" : o.total().toPlainString());
        buscarPor(o -> o.numero() + " " + o.proveedorNombre());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Nueva", true, v -> nueva());
        }
        if (Perm.tiene("compras.aprobar")) {
            accion("Aprobar", false, v -> accion(v, "APROBAR"));
        }
        if (Perm.tiene("compras.editar")) {
            accion("Anular", false, v -> accion(v, "ANULAR"));
        }
        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        onDobleClic(this::ver);
        refrescar();
    }

    private void nueva() {
        ComboBox<Proveedor> proveedor = Combos.proveedores();
        ComboBox<Sucursal> sucursal = Combos.sucursales();
        DatePicker entrega = new DatePicker();
        TextField condiciones = Fx.campo("");
        TextField nota = Fx.campo("");

        ObservableList<OrdenCompraDetalle> items = FXCollections.observableArrayList();
        TableView<OrdenCompraDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(200);
        TableColumn<OrdenCompraDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(240);
        TableColumn<OrdenCompraDetalle, String> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().cantidad())));
        cCant.setPrefWidth(90);
        TableColumn<OrdenCompraDetalle, String> cPrecio = new TableColumn<>("Precio");
        cPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().precioUnitario())));
        cPrecio.setPrefWidth(90);
        tabla.getColumns().addAll(cP, cCant, cPrecio);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(220);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(70);
        TextField precio = Fx.campo("");
        precio.setPrefWidth(80);
        precio.setPromptText("precio");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p != null) {
                items.add(new OrdenCompraDetalle(null, 0, p.id(), p.nombre(), null, decimal(Fx.texto(cantidad)),
                        BigDecimal.ZERO, decimal(Fx.texto(precio)), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, producto, cantidad, precio, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barra);
        itemsBox.setPadding(new Insets(4));

        new DialogoFormulario("Nueva orden de compra", ventana())
                .agregar("Proveedor", proveedor)
                .agregar("Sucursal", sucursal)
                .agregar("Entrega esperada", entrega)
                .agregar("Condiciones de pago", condiciones)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    servicio.crear(sucursal.getValue().id(), proveedor.getValue().id(), entrega.getValue(),
                            Fx.nuloSiVacio(Fx.texto(condiciones)), Fx.nuloSiVacio(Fx.texto(nota)),
                            new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private void accion(VistaLista<OrdenCompra> vista, String accion) {
        OrdenCompra o = vista.seleccion().orElse(null);
        if (o == null) {
            return;
        }
        try {
            if ("APROBAR".equals(accion)) {
                servicio.aprobar(o.id());
            } else {
                servicio.anular(o.id());
            }
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo " + accion.toLowerCase(), e.getMessage());
        }
    }

    private void ver(OrdenCompra o) {
        OrdenCompra completa = servicio.porId(o.id());
        StringBuilder sb = new StringBuilder("Proveedor: " + completa.proveedorNombre() + "\nEstado: " + completa.estado()
                + "\nTotal: " + valor(completa.total()) + "\n\n");
        for (OrdenCompraDetalle d : completa.detalles()) {
            sb.append(String.format("%-36s pedido %8s  recibido %8s  x %8s%n",
                    d.productoNombre(), valor(d.cantidad()), valor(d.cantidadRecibida()), valor(d.precioUnitario())));
        }
        Alertas.info("Orden " + completa.numero(), sb.toString());
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
