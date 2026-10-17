package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.servicio.ServicioFacturaProveedor;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaFacturas extends VistaLista<FacturaProveedor> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ServicioFacturaProveedor servicio = new ServicioFacturaProveedor();

    public VistaFacturas() {
        super("Facturas de proveedor");
        columna("Proveedor", 220, FacturaProveedor::proveedorNombre);
        columna("Numero", 130, FacturaProveedor::numero);
        columna("Tipo", 110, FacturaProveedor::tipoDocumento);
        columna("Emision", 110, f -> f.fechaEmision() == null ? "" : f.fechaEmision().format(FORMATO));
        columna("Vence", 110, f -> f.fechaVencimiento() == null ? "" : f.fechaVencimiento().format(FORMATO));
        columna("Total", 110, f -> valor(f.total()));
        columna("Saldo", 110, f -> valor(f.saldo()));
        columna("Estado", 100, FacturaProveedor::estadoPago);
        buscarPor(f -> f.numero() + " " + f.proveedorNombre());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Registrar", true, v -> registrar());
        }
        refrescar();
    }

    private void registrar() {
        ComboBox<Proveedor> proveedor = Combos.proveedores();
        ComboBox<String> tipo = Fx.combo("FACTURA", "NOTA_CREDITO", "LIQUIDACION");
        TextField numero = Fx.campo("");
        DatePicker emision = new DatePicker(LocalDate.now());
        DatePicker vencimiento = new DatePicker(LocalDate.now().plusDays(30));
        TextField subtotal = Fx.campo("0");
        TextField descuento = Fx.campo("0");
        TextField iva = Fx.campo("0");
        TextField total = Fx.campo("0");
        subtotal.textProperty().addListener((o, a, b) -> recalcular(subtotal, descuento, iva, total));
        descuento.textProperty().addListener((o, a, b) -> recalcular(subtotal, descuento, iva, total));
        iva.textProperty().addListener((o, a, b) -> recalcular(subtotal, descuento, iva, total));

        new DialogoFormulario("Registrar factura de proveedor", ventana())
                .agregar("Proveedor", proveedor)
                .agregar("Tipo", tipo)
                .agregar("Numero", numero)
                .agregar("Fecha emision", emision)
                .agregar("Fecha vencimiento", vencimiento)
                .agregar("Subtotal", subtotal)
                .agregar("Descuento", descuento)
                .agregar("IVA", iva)
                .agregar("Total", total)
                .alGuardar(() -> {
                    servicio.registrar(proveedor.getValue().id(), tipo.getValue(), Fx.texto(numero),
                            emision.getValue(), vencimiento.getValue(), decimal(Fx.texto(subtotal)),
                            decimal(Fx.texto(descuento)), decimal(Fx.texto(iva)), decimal(Fx.texto(total)), null, null);
                    refrescar();
                })
                .mostrar();
    }

    private void recalcular(TextField subtotal, TextField descuento, TextField iva, TextField total) {
        BigDecimal t = decimal(Fx.texto(subtotal)).subtract(decimal(Fx.texto(descuento))).add(decimal(Fx.texto(iva)));
        total.setText(t.toPlainString());
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
