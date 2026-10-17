package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.SolicitudCompra;
import ec.sfjfx.core.modelo.SolicitudDetalle;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.servicio.ServicioSolicitudCompra;
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

public final class VistaSolicitudes extends VistaLista<SolicitudCompra> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioSolicitudCompra servicio = new ServicioSolicitudCompra();

    public VistaSolicitudes() {
        super("Solicitudes de compra");
        columna("Numero", 160, SolicitudCompra::numero);
        columna("Estado", 130, SolicitudCompra::estado);
        columna("Fecha", 160, s -> s.fecha() == null ? "" : s.fecha().format(FORMATO));
        buscarPor(SolicitudCompra::numero);
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Nueva", true, v -> nueva());
        }
        if (Perm.tiene("compras.aprobar")) {
            accion("Aprobar", false, v -> cambiar(v, true));
            accion("Rechazar", false, v -> cambiar(v, false));
        }
        refrescar();
    }

    private void nueva() {
        ComboBox<Sucursal> sucursal = Combos.sucursales();
        TextField nota = Fx.campo("");

        ObservableList<SolicitudDetalle> items = FXCollections.observableArrayList();
        TableView<SolicitudDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(180);
        TableColumn<SolicitudDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(240);
        TableColumn<SolicitudDetalle, String> cC = new TableColumn<>("Cantidad");
        cC.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad().toPlainString()));
        cC.setPrefWidth(100);
        tabla.getColumns().addAll(cP, cC);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(220);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(80);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p != null) {
                items.add(new SolicitudDetalle(null, 0, p.id(), p.nombre(), decimal(Fx.texto(cantidad)), null));
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, producto, cantidad, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barra);
        itemsBox.setPadding(new Insets(4));

        new DialogoFormulario("Nueva solicitud de compra", ventana())
                .agregar("Sucursal", sucursal)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    servicio.crear(sucursal.getValue().id(), Fx.nuloSiVacio(Fx.texto(nota)), new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private void cambiar(VistaLista<SolicitudCompra> vista, boolean aprobar) {
        SolicitudCompra s = vista.seleccion().orElse(null);
        if (s == null) {
            return;
        }
        try {
            if (aprobar) {
                servicio.aprobar(s.id());
            } else {
                servicio.rechazar(s.id());
            }
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo actualizar", e.getMessage());
        }
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
