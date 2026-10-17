package ec.sfjfx.ui.vistas.clientes;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.Pedido;
import ec.sfjfx.core.modelo.PedidoDetalle;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCliente;
import ec.sfjfx.core.servicio.ServicioPedido;
import ec.sfjfx.core.servicio.ServicioProducto;
import ec.sfjfx.core.servicio.ServicioSesionCaja;
import ec.sfjfx.core.servicio.ServicioVenta;
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

public final class VistaPedidos extends VistaLista<Pedido> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioPedido servicio = new ServicioPedido();
    private final ServicioCliente clientes = new ServicioCliente();

    public VistaPedidos() {
        super("Pedidos (pickup / delivery)");
        columna("Numero", 150, Pedido::numero);
        columna("Cliente", 220, Pedido::clienteNombre);
        columna("Entrega", 110, Pedido::tipoEntrega);
        columna("Estado", 140, Pedido::estado);
        columna("Total", 110, p -> p.total() == null ? "" : p.total().toPlainString());
        columna("Venta", 140, p -> p.ventaNumero() == null ? "-" : p.ventaNumero());
        buscarPor(p -> p.numero() + " " + (p.clienteNombre() == null ? "" : p.clienteNombre()));
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("clientes.crear")) {
            accion("Nuevo", true, v -> nuevo());
        }
        if (Perm.tiene("clientes.editar")) {
            accion("Cambiar estado", false, v -> v.seleccion().ifPresent(this::cambiarEstado));
            accion("Convertir a venta", false, v -> v.seleccion().ifPresent(this::convertir));
        }
        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        onDobleClic(this::ver);
        refrescar();
    }

    private void nuevo() {
        ComboBox<Sucursal> sucursal = new ComboBox<>();
        sucursal.getItems().setAll(new SucursalRepositorio().porEmpresa(empresa()));
        sucursal.getStyleClass().add("selector");
        sucursal.setMaxWidth(Double.MAX_VALUE);
        if (!sucursal.getItems().isEmpty()) {
            sucursal.setValue(sucursal.getItems().get(0));
        }
        ComboBox<Cliente> cliente = new ComboBox<>();
        cliente.getItems().setAll(clientes.listar(null));
        cliente.getStyleClass().add("selector");
        cliente.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> tipoEntrega = Fx.combo("PICKUP", "DELIVERY");
        TextField nota = Fx.campo("");

        ObservableList<PedidoDetalle> items = FXCollections.observableArrayList();
        TableView<PedidoDetalle> tabla = new TableView<>(items);
        tabla.setPrefHeight(180);
        TableColumn<PedidoDetalle, String> cP = new TableColumn<>("Producto");
        cP.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cP.setPrefWidth(240);
        TableColumn<PedidoDetalle, String> cC = new TableColumn<>("Cantidad");
        cC.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().cantidad())));
        cC.setPrefWidth(90);
        TableColumn<PedidoDetalle, String> cPrinc = new TableColumn<>("Precio");
        cPrinc.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().precioUnitario())));
        cPrinc.setPrefWidth(90);
        tabla.getColumns().addAll(cP, cC, cPrinc);

        ComboBox<Producto> producto = new ComboBox<>();
        producto.getItems().setAll(new ServicioProducto().listar(null));
        producto.setPrefWidth(220);
        TextField cantidad = Fx.campo("1");
        cantidad.setPrefWidth(70);
        TextField precio = Fx.campo("");
        precio.setPrefWidth(80);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto p = producto.getValue();
            if (p != null) {
                items.add(new PedidoDetalle(null, 0, p.id(), p.nombre(), decimal(Fx.texto(cantidad)),
                        decimal(Fx.texto(precio)), BigDecimal.ZERO));
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> items.remove(tabla.getSelectionModel().getSelectedItem()));
        HBox barra = new HBox(8, producto, cantidad, precio, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox itemsBox = new VBox(8, new javafx.scene.control.Label("Items"), tabla, barra);
        itemsBox.setPadding(new Insets(4));

        new DialogoFormulario("Nuevo pedido", ventana())
                .agregar("Sucursal", sucursal)
                .agregar("Cliente", cliente)
                .agregar("Tipo entrega", tipoEntrega)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(itemsBox)
                .alGuardar(() -> {
                    servicio.crear(sucursal.getValue().id(), cliente.getValue() == null ? null : cliente.getValue().id(),
                            tipoEntrega.getValue(), null, Fx.nuloSiVacio(Fx.texto(nota)), new ArrayList<>(items));
                    refrescar();
                })
                .mostrar();
    }

    private void cambiarEstado(Pedido pedido) {
        java.util.List<String> estados = new ArrayList<>(ServicioPedido.estados());
        estados.add("CANCELADO");
        ComboBox<String> estado = Fx.combo(estados.toArray(new String[0]));
        estado.setValue(pedido.estado());
        new DialogoFormulario("Estado del pedido " + pedido.numero(), ventana())
                .agregar("Estado", estado)
                .alGuardar(() -> {
                    servicio.cambiarEstado(pedido.id(), estado.getValue());
                    refrescar();
                })
                .mostrar();
    }

    private void convertir(Pedido pedido) {
        if (pedido.convertido()) {
            Alertas.advertencia("Pedido", "El pedido ya fue convertido a venta.");
            return;
        }
        var caja = SesionActual.requerida().caja();
        if (caja == null) {
            Alertas.advertencia("Sin caja", "Seleccione una caja en la barra superior.");
            return;
        }
        SesionCaja sesion = new ServicioSesionCaja().sesionAbierta(caja.id());
        if (sesion == null) {
            Alertas.advertencia("Caja cerrada", "Abra la caja para convertir el pedido en venta.");
            return;
        }
        Bodega bodega = new BodegaRepositorio().bodegaVentaDeSucursal(pedido.sucursalId())
                .orElseGet(() -> new BodegaRepositorio().porEmpresa(empresa()).stream().findFirst().orElse(null));
        if (bodega == null) {
            Alertas.error("Convertir", "No hay bodega disponible");
            return;
        }
        ComboBox<MedioPagoRepositorio.MedioPago> medio = new ComboBox<>();
        medio.getItems().setAll(new MedioPagoRepositorio().listar());
        medio.getStyleClass().add("selector");
        medio.setMaxWidth(Double.MAX_VALUE);
        if (!medio.getItems().isEmpty()) {
            medio.setValue(medio.getItems().get(0));
        }
        boolean incluye = new ServicioVenta().listaPrecioDefault().map(l -> l.incluyeIva()).orElse(false);
        new DialogoFormulario("Convertir " + pedido.numero() + " en venta (total $" + pedido.total() + ")", ventana())
                .agregar("Medio de pago", medio)
                .alGuardar(() -> {
                    servicio.convertirAVenta(pedido.id(), sesion, bodega.id(), medio.getValue().id(), incluye);
                    refrescar();
                })
                .mostrar();
    }

    private void ver(Pedido pedido) {
        Pedido completo = servicio.porId(pedido.id());
        StringBuilder sb = new StringBuilder("Cliente: " + (completo.clienteNombre() == null ? "Consumidor final" : completo.clienteNombre())
                + "\nEstado: " + completo.estado() + "\nTipo: " + completo.tipoEntrega() + "\n\n");
        for (PedidoDetalle d : completo.detalles()) {
            sb.append(String.format("%-36s %8s x %8s%n", d.productoNombre(), valor(d.cantidad()), valor(d.precioUnitario())));
        }
        sb.append("\nTotal: ").append(valor(completo.total()));
        Alertas.info("Pedido " + completo.numero(), sb.toString());
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

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
