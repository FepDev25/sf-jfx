package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoProveedor;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioProveedor;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaProveedores extends VistaLista<Proveedor> {

    private final ServicioProveedor servicio = new ServicioProveedor();

    public VistaProveedores() {
        super("Proveedores");
        columna("Razon social", 260, Proveedor::razonSocial);
        columna("Identificacion", 140, Proveedor::numeroIdentificacion);
        columna("Contacto", 180, Proveedor::telefono);
        columna("Credito (dias)", 110, p -> String.valueOf(p.diasCredito()));
        columna("Ret. IVA", 90, p -> Fx.siNo(p.aplicaRetencionIva()));
        columna("Ret. Renta", 90, p -> Fx.siNo(p.aplicaRetencionRenta()));
        columna("Activo", 70, p -> Fx.siNo(p.activo()));
        buscarPor(p -> p.razonSocial() + " " + p.numeroIdentificacion());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("proveedores.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("proveedores.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
            accion("Productos", false, v -> v.seleccion().ifPresent(this::productos));
        }
        onDobleClic(p -> {
            if (Perm.tiene("proveedores.editar")) {
                abrirFormulario(p);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Proveedor original) {
        boolean nuevo = original == null;
        long empresa = SesionActual.requerida().empresa().id();
        Proveedor base = nuevo
                ? new Proveedor(null, empresa, "RUC", "", "", null, null, null, null, null, 0, false, false, true, null)
                : original;

        ComboBox<String> tipoId = Fx.combo("RUC", "CEDULA", "PASAPORTE");
        tipoId.setValue(base.tipoIdentificacion());
        TextField identificacion = Fx.campo(base.numeroIdentificacion());
        TextField razon = Fx.campo(base.razonSocial());
        TextField comercial = Fx.campo(base.nombreComercial());
        TextField direccion = Fx.campo(base.direccion());
        TextField telefono = Fx.campo(base.telefono());
        TextField email = Fx.campo(base.email());
        TextField contribuyente = Fx.campo(base.tipoContribuyente());
        TextField dias = Fx.campo(String.valueOf(base.diasCredito()));
        CheckBox retIva = Fx.check("Aplica retencion de IVA", base.aplicaRetencionIva());
        CheckBox retRenta = Fx.check("Aplica retencion de Renta", base.aplicaRetencionRenta());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nuevo proveedor" : "Editar proveedor", ventana())
                .agregar("Tipo identificacion", tipoId)
                .agregar("Identificacion", identificacion)
                .agregar("Razon social", razon)
                .agregar("Nombre comercial", comercial)
                .agregar("Direccion", direccion)
                .agregar("Telefono", telefono)
                .agregar("Email", email)
                .agregar("Tipo contribuyente", contribuyente)
                .agregar("Dias credito", dias)
                .agregarAnchoCompleto(retIva)
                .agregarAnchoCompleto(retRenta)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Proveedor(base.id(), empresa, tipoId.getValue(), Fx.texto(identificacion),
                            Fx.texto(razon), Fx.nuloSiVacio(Fx.texto(comercial)), Fx.nuloSiVacio(Fx.texto(direccion)),
                            Fx.nuloSiVacio(Fx.texto(telefono)), Fx.nuloSiVacio(Fx.texto(email)),
                            Fx.nuloSiVacio(Fx.texto(contribuyente)), entero(Fx.texto(dias)), retIva.isSelected(),
                            retRenta.isSelected(), activo.isSelected(), base.nota()));
                    refrescar();
                })
                .mostrar();
    }

    private void productos(Proveedor p) {
        ObservableList<ProductoProveedor> items = FXCollections.observableArrayList(servicio.productosDe(p.id()));
        TableView<ProductoProveedor> tabla = new TableView<>(items);
        tabla.setPrefHeight(220);
        TableColumn<ProductoProveedor, String> cProd = new TableColumn<>("Producto");
        cProd.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productoNombre()));
        cProd.setPrefWidth(240);
        TableColumn<ProductoProveedor, String> cCosto = new TableColumn<>("Costo ultimo");
        cCosto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().costoUltimo() == null ? "" : c.getValue().costoUltimo().toPlainString()));
        cCosto.setPrefWidth(110);
        TableColumn<ProductoProveedor, String> cPref = new TableColumn<>("Preferido");
        cPref.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esPreferido())));
        cPref.setPrefWidth(90);
        tabla.getColumns().addAll(cProd, cCosto, cPref);

        ComboBox<Producto> producto = Combos.productos();
        producto.setPrefWidth(240);
        TextField costo = Fx.campo("");
        costo.setPrefWidth(90);
        costo.setPromptText("costo");
        CheckBox preferido = new CheckBox("Preferido");
        Button agregar = new Button("Agregar / actualizar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto sel = producto.getValue();
            if (sel == null) {
                return;
            }
            servicio.guardarProducto(new ProductoProveedor(null, sel.id(), sel.nombre(), p.id(), p.razonSocial(),
                    null, decimal(Fx.texto(costo)), null, preferido.isSelected()));
            items.setAll(servicio.productosDe(p.id()));
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> {
            ProductoProveedor sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                servicio.eliminarProducto(sel.id());
                items.setAll(servicio.productosDe(p.id()));
            }
        });

        HBox barra = new HBox(8, producto, costo, preferido, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(8, new javafx.scene.control.Label("Productos del proveedor: " + p.razonSocial()),
                tabla, barra);
        caja.setPadding(new Insets(12));

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialogo = new javafx.scene.control.Dialog<>();
        dialogo.setTitle("Productos del proveedor");
        dialogo.getDialogPane().getButtonTypes().add(new javafx.scene.control.ButtonType("Cerrar",
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().setContent(caja);
        dialogo.getDialogPane().setPrefWidth(720);
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
            return v == null || v.isBlank() ? null : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
