package ec.sfjfx.ui.vistas.inventario;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Transferencia;
import ec.sfjfx.core.modelo.TransferenciaDetalle;
import ec.sfjfx.core.servicio.ServicioTransferencia;
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

public final class VistaTransferencias extends VistaLista<Transferencia> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioTransferencia servicio = new ServicioTransferencia();

    public VistaTransferencias() {
        super("Transferencias entre bodegas");
        columna("Numero", 150, Transferencia::numero);
        columna("Origen", 160, Transferencia::bodegaOrigen);
        columna("Destino", 160, Transferencia::bodegaDestino);
        columna("Estado", 110, Transferencia::estado);
        columna("Envio", 150, t -> t.fechaEnvio() == null ? "" : t.fechaEnvio().format(FORMATO));
        columna("Recepcion", 150, t -> t.fechaRecepcion() == null ? "" : t.fechaRecepcion().format(FORMATO));
        buscarPor(t -> t.numero() + " " + t.bodegaOrigen() + " " + t.bodegaDestino());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("inventario.crear")) {
            accion("Nueva", true, v -> nueva());
        }
        accion("Enviar", false, v -> accionEstado(v, "ENVIAR"));
        accion("Recibir", false, v -> accionEstado(v, "RECIBIR"));
        accion("Anular", false, v -> accionEstado(v, "ANULAR"));
        accion("Ver detalle", false, v -> v.seleccion().ifPresent(this::verDetalle));
        onDobleClic(this::verDetalle);
        refrescar();
    }

    private void nueva() {
        ComboBox<Bodega> origen = Combos.bodegas();
        ComboBox<Bodega> destino = Combos.bodegas();
        TextField nota = Fx.campo("");

        ObservableList<TransferenciaDetalle> items = FXCollections.observableArrayList();
        TableView<TransferenciaDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(200);
        TableColumn<TransferenciaDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(280);
        TableColumn<TransferenciaDetalle, String> cC = new TableColumn<>("Cantidad");
        cC.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().cantidadEnviada())));
        cC.setPrefWidth(110);
        tabla.getColumns().addAll(cP, cC);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(260);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(90);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p != null) {
                items.add(new TransferenciaDetalle(null, 0, p.id(), p.nombre(), null, decimal(Fx.texto(cantidad)), BigDecimal.ZERO));
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barraItems = new HBox(8, producto, cantidad, agregar, quitar);
        barraItems.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barraItems);
        itemsBox.setPadding(new Insets(4, 0, 4, 0));

        new DialogoFormulario("Nueva transferencia", ventana())
                .agregar("Bodega origen", origen)
                .agregar("Bodega destino", destino)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    Bodega bo = origen.getValue();
                    Bodega bd = destino.getValue();
                    servicio.crear(bo.sucursalId(), bo.id(), bd.sucursalId(), bd.id(),
                            Fx.nuloSiVacio(Fx.texto(nota)), new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private void accionEstado(VistaLista<Transferencia> vista, String accion) {
        Transferencia t = vista.seleccion().orElse(null);
        if (t == null) {
            return;
        }
        try {
            switch (accion) {
                case "ENVIAR" -> servicio.enviar(t.id());
                case "RECIBIR" -> servicio.recibir(t.id());
                case "ANULAR" -> servicio.anular(t.id());
                default -> { }
            }
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo " + accion.toLowerCase(), e.getMessage());
        }
    }

    private void verDetalle(Transferencia t) {
        StringBuilder sb = new StringBuilder(t.bodegaOrigen() + " -> " + t.bodegaDestino()
                + "\nEstado: " + t.estado() + "\n\n");
        for (TransferenciaDetalle d : servicio.detalles(t.id())) {
            sb.append(String.format("%-40s enviado %10s  recibido %10s%n",
                    d.productoNombre(), valor(d.cantidadEnviada()), valor(d.cantidadRecibida())));
        }
        Alertas.info("Transferencia " + t.numero(), sb.toString());
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
