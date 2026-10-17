package ec.sfjfx.ui.vistas.inventario;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.AjusteDetalle;
import ec.sfjfx.core.modelo.AjusteInventario;
import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.servicio.ServicioAjuste;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaAjustes extends VistaLista<AjusteInventario> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioAjuste servicio = new ServicioAjuste();

    public VistaAjustes() {
        super("Ajustes de inventario");
        columna("Numero", 160, AjusteInventario::numero);
        columna("Bodega", 160, AjusteInventario::bodega);
        columna("Tipo", 100, AjusteInventario::tipo);
        columna("Motivo", 130, AjusteInventario::motivo);
        columna("Estado", 100, AjusteInventario::estado);
        columna("Fecha", 150, a -> a.fecha() == null ? "" : a.fecha().format(FORMATO));
        buscarPor(a -> a.numero() + " " + a.motivo() + " " + a.bodega());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("inventario.crear")) {
            accion("Nuevo", true, v -> nuevoAjuste());
        }
        accion("Ver detalle", false, v -> v.seleccion().ifPresent(this::verDetalle));
        onDobleClic(this::verDetalle);
        refrescar();
    }

    private void nuevoAjuste() {
        ComboBox<Bodega> bodega = Combos.bodegas();
        ComboBox<String> tipo = Fx.combo("ENTRADA", "SALIDA");
        ComboBox<String> motivo = Fx.combo("MERMA", "DANO", "ROBO", "VENCIMIENTO", "CORRECCION", "INICIAL", "OTRO");
        TextField nota = Fx.campo("");

        ObservableList<AjusteDetalle> items = FXCollections.observableArrayList();
        TableView<AjusteDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(200);
        TableColumn<AjusteDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(260);
        TableColumn<AjusteDetalle, String> cC = new TableColumn<>("Cantidad");
        cC.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().cantidad())));
        cC.setPrefWidth(100);
        TableColumn<AjusteDetalle, String> cCosto = new TableColumn<>("Costo unit.");
        cCosto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().costoUnitario())));
        cCosto.setPrefWidth(110);
        tabla.getColumns().addAll(cP, cC, cCosto);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(240);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(80);
        TextField costo = Fx.campo("");
        costo.setPrefWidth(90);
        costo.setPromptText("costo");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p == null) {
                return;
            }
            items.add(new AjusteDetalle(null, 0, p.id(), p.nombre(), null, decimal(Fx.texto(cantidad)),
                    decimal(Fx.texto(costo))));
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barraItems = new HBox(8, producto, cantidad, costo, agregar, quitar);
        barraItems.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barraItems);
        itemsBox.setPadding(new Insets(4, 0, 4, 0));

        new DialogoFormulario("Nuevo ajuste de inventario", ventana())
                .agregar("Bodega", bodega)
                .agregar("Tipo", tipo)
                .agregar("Motivo", motivo)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    servicio.aplicar(bodega.getValue().id(), tipo.getValue(), motivo.getValue(),
                            Fx.nuloSiVacio(Fx.texto(nota)), new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private void verDetalle(AjusteInventario a) {
        AjusteInventario completo = servicio.porId(a.id());
        StringBuilder sb = new StringBuilder("Bodega: " + completo.bodega() + "\nTipo: " + completo.tipo()
                + "\nMotivo: " + completo.motivo() + "\n\n");
        for (AjusteDetalle d : completo.detalles()) {
            sb.append(String.format("%-40s %10s%n", d.productoNombre(), valor(d.cantidad())));
        }
        Alertas.info("Ajuste " + completo.numero(), sb.toString());
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
