package ec.sfjfx.ui.vistas.clientes;

import java.math.BigDecimal;

import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.ClienteDireccion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCliente;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaClientes extends VistaLista<Cliente> {

    private final ServicioCliente servicio = new ServicioCliente();

    public VistaClientes() {
        super("Clientes");
        columna("Nombre / Razon social", 260, Cliente::nombreParaMostrar);
        columna("Identificacion", 140, Cliente::numeroIdentificacion);
        columna("Telefono", 130, Cliente::telefono);
        columna("Credito", 90, c -> Fx.siNo(c.esCredito()));
        columna("Limite", 100, c -> c.limiteCredito() == null ? "" : FormatoReporte.moneda(c.limiteCredito()));
        columna("Saldo", 100, c -> c.saldo() == null ? "" : FormatoReporte.moneda(c.saldo()));
        columna("Activo", 70, c -> Fx.siNo(c.activo()));
        buscarPor(c -> c.nombreParaMostrar() + " " + c.numeroIdentificacion());
        proveedorPaginado(servicio::pagina);

        if (Perm.tiene("clientes.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("clientes.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
            accion("Direcciones", false, v -> v.seleccion().ifPresent(this::direcciones));
        }
        accion("Ficha", false, v -> v.seleccion().ifPresent(this::ficha));
        onDobleClic(c -> {
            if (Perm.tiene("clientes.editar")) {
                abrirFormulario(c);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Cliente original) {
        boolean nuevo = original == null;
        long empresa = SesionActual.requerida().empresa().id();
        Cliente base = nuevo
                ? new Cliente(null, empresa, "CONSUMIDOR_FINAL", "CONSUMIDOR_FINAL", "9999999999", null, null,
                        null, null, null, null, false, BigDecimal.ZERO, 0, BigDecimal.ZERO, true)
                : original;

        ComboBox<String> tipo = Fx.combo("CONSUMIDOR_FINAL", "PERSONA_NATURAL", "EMPRESA");
        tipo.setValue(base.tipo());
        ComboBox<String> tipoId = Fx.combo("CONSUMIDOR_FINAL", "CEDULA", "RUC", "PASAPORTE");
        tipoId.setValue(base.tipoIdentificacion());
        TextField identificacion = Fx.campo(base.numeroIdentificacion());
        TextField nombres = Fx.campo(base.nombres());
        TextField apellidos = Fx.campo(base.apellidos());
        TextField razon = Fx.campo(base.razonSocial());
        TextField email = Fx.campo(base.email());
        TextField telefono = Fx.campo(base.telefono());
        TextField direccion = Fx.campo(base.direccion());
        CheckBox credito = Fx.check("Habilitado para credito", base.esCredito());
        TextField limite = Fx.campo(base.limiteCredito() == null ? "0" : base.limiteCredito().toPlainString());
        TextField dias = Fx.campo(String.valueOf(base.diasCredito()));
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nuevo cliente" : "Editar cliente", ventana())
                .agregar("Tipo", tipo)
                .agregar("Tipo identificacion", tipoId)
                .agregar("Identificacion", identificacion)
                .agregar("Nombres", nombres)
                .agregar("Apellidos", apellidos)
                .agregar("Razon social", razon)
                .agregar("Email", email)
                .agregar("Telefono", telefono)
                .agregar("Direccion", direccion)
                .agregar("Limite credito", limite)
                .agregar("Dias credito", dias)
                .agregarAnchoCompleto(credito)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Cliente(base.id(), empresa, tipo.getValue(), tipoId.getValue(),
                            Fx.texto(identificacion), Fx.nuloSiVacio(Fx.texto(nombres)),
                            Fx.nuloSiVacio(Fx.texto(apellidos)), Fx.nuloSiVacio(Fx.texto(razon)),
                            Fx.nuloSiVacio(Fx.texto(email)), Fx.nuloSiVacio(Fx.texto(telefono)),
                            Fx.nuloSiVacio(Fx.texto(direccion)), credito.isSelected(), decimal(Fx.texto(limite)),
                            entero(Fx.texto(dias)), base.saldo(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private void ficha(Cliente cliente) {
        Cliente cl = servicio.porId(cliente.id());
        var ventas = new ec.sfjfx.core.servicio.ServicioVenta().listar(null).stream()
                .filter(v -> v.pagada() && cl.id().equals(v.clienteId())).toList();
        BigDecimal total = ventas.stream().map(v -> v.total() == null ? BigDecimal.ZERO : v.total())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = ventas.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(ventas.size()), 2, java.math.RoundingMode.HALF_UP);
        StringBuilder sb = new StringBuilder();
        sb.append("Identificacion: ").append(cl.numeroIdentificacion()).append("\n");
        sb.append("Telefono: ").append(cl.telefono() == null ? "-" : cl.telefono()).append("\n");
        sb.append("Credito: ").append(cl.esCredito() ? "Si (limite $" + cl.limiteCredito() + ", " + cl.diasCredito() + " dias)" : "No").append("\n");
        sb.append("Saldo por cobrar: $ ").append(cl.saldo() == null ? "0" : cl.saldo().toPlainString()).append("\n\n");
        sb.append("Compras: ").append(ventas.size()).append("\n");
        sb.append("Total comprado: $ ").append(total.toPlainString()).append("\n");
        sb.append("Ticket promedio: $ ").append(promedio.toPlainString());
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alerta.setTitle("Ficha de cliente");
        alerta.setHeaderText(cl.nombreParaMostrar());
        alerta.setContentText(sb.toString());
        alerta.showAndWait();
    }

    private void direcciones(Cliente cliente) {
        ObservableList<ClienteDireccion> items = FXCollections.observableArrayList(servicio.direcciones(cliente.id()));
        TableView<ClienteDireccion> tabla = new TableView<>(items);
        tabla.setPrefHeight(180);
        TableColumn<ClienteDireccion, String> cAlias = new TableColumn<>("Alias");
        cAlias.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().alias()));
        cAlias.setPrefWidth(140);
        TableColumn<ClienteDireccion, String> cDir = new TableColumn<>("Direccion");
        cDir.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().direccion()));
        cDir.setPrefWidth(280);
        TableColumn<ClienteDireccion, String> cPrin = new TableColumn<>("Principal");
        cPrin.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esPrincipal())));
        cPrin.setPrefWidth(80);
        tabla.getColumns().addAll(cAlias, cDir, cPrin);

        TextField alias = Fx.campo("");
        TextField dir = Fx.campo("");
        dir.setPrefWidth(240);
        TextField ref = Fx.campo("");
        CheckBox principal = new CheckBox("Principal");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            if (Fx.texto(dir).isBlank()) {
                return;
            }
            servicio.agregarDireccion(new ClienteDireccion(null, cliente.id(), Fx.nuloSiVacio(Fx.texto(alias)),
                    Fx.texto(dir), Fx.nuloSiVacio(Fx.texto(ref)), null, null, principal.isSelected()));
            items.setAll(servicio.direcciones(cliente.id()));
            dir.clear();
            alias.clear();
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> {
            ClienteDireccion sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                servicio.eliminarDireccion(sel.id());
                items.setAll(servicio.direcciones(cliente.id()));
            }
        });
        HBox barra = new HBox(8, alias, dir, ref, principal, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(8, new Label("Direcciones de " + cliente.nombreParaMostrar()), tabla, barra);
        caja.setPadding(new Insets(12));

        Dialog<javafx.scene.control.ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Direcciones");
        dialogo.getDialogPane().getButtonTypes().add(new javafx.scene.control.ButtonType("Cerrar",
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().setContent(caja);
        dialogo.getDialogPane().setPrefWidth(760);
        if (ventana() != null) {
            dialogo.initOwner(ventana());
        }
        dialogo.showAndWait();
    }

    private int entero(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
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
