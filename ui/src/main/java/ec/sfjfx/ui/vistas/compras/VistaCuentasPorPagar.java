package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.PagoProveedor;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.servicio.ServicioFacturaProveedor;
import ec.sfjfx.core.servicio.ServicioPagoProveedor;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaCuentasPorPagar extends VistaLista<FacturaProveedor> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ServicioFacturaProveedor facturas = new ServicioFacturaProveedor();
    private final ServicioPagoProveedor pagos = new ServicioPagoProveedor();

    public VistaCuentasPorPagar() {
        super("Cuentas por pagar (facturas con saldo)");
        columna("Proveedor", 220, FacturaProveedor::proveedorNombre);
        columna("Numero", 130, FacturaProveedor::numero);
        columna("Vence", 110, f -> f.fechaVencimiento() == null ? "" : f.fechaVencimiento().format(FORMATO));
        columna("Total", 110, f -> valor(f.total()));
        columna("Saldo", 110, f -> valor(f.saldo()));
        columna("Estado", 100, FacturaProveedor::estadoPago);
        buscarPor(f -> f.numero() + " " + f.proveedorNombre());
        proveedor(() -> facturas.conSaldo(null));

        if (Perm.tiene("compras.crear")) {
            accion("Registrar pago", true, v -> registrarPago());
        }
        accion("Refrescar", false, v -> refrescar());
        refrescar();
    }

    private void registrarPago() {
        ComboBox<Proveedor> proveedor = Combos.proveedores();
        ComboBox<MedioPagoRepositorio.MedioPago> medio = Combos.mediosPago();
        TextField referencia = Fx.campo("");
        TextField nota = Fx.campo("");

        GridPane grilla = new GridPane();
        grilla.setHgap(8);
        grilla.setVgap(6);
        grilla.setPadding(new Insets(6));
        grilla.add(new Label("Factura"), 0, 0);
        grilla.add(new Label("Saldo"), 1, 0);
        grilla.add(new Label("Aplicar"), 2, 0);

        List<FacturaProveedor> listaFacturas = new ArrayList<>();
        List<TextField> campos = new ArrayList<>();
        int[] fila = {1};

        Runnable cargar = () -> {
            grilla.getChildren().removeIf(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) >= 1);
            listaFacturas.clear();
            campos.clear();
            fila[0] = 1;
            if (proveedor.getValue() == null) {
                return;
            }
            for (FacturaProveedor f : facturas.conSaldo(proveedor.getValue().id())) {
                listaFacturas.add(f);
                grilla.add(new Label(f.numero()), 0, fila[0]);
                grilla.add(new Label(valor(f.saldo())), 1, fila[0]);
                TextField monto = Fx.campo(valor(f.saldo()));
                monto.setPrefWidth(100);
                campos.add(monto);
                grilla.add(monto, 2, fila[0]);
                fila[0]++;
            }
        };
        proveedor.valueProperty().addListener((o, a, b) -> cargar.run());
        cargar.run();

        VBox caja = new VBox(8, grilla);
        caja.setPadding(new Insets(4, 0, 4, 0));

        new DialogoFormulario("Registrar pago a proveedor", ventana())
                .agregar("Proveedor", proveedor)
                .agregar("Medio de pago", medio)
                .agregar("Referencia", referencia)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    List<PagoProveedor.PagoFactura> aplicaciones = new ArrayList<>();
                    BigDecimal total = BigDecimal.ZERO;
                    for (int i = 0; i < listaFacturas.size(); i++) {
                        BigDecimal monto = decimal(Fx.texto(campos.get(i)));
                        if (monto.signum() > 0) {
                            FacturaProveedor f = listaFacturas.get(i);
                            aplicaciones.add(new PagoProveedor.PagoFactura(f.id(), f.numero(), monto));
                            total = total.add(monto);
                        }
                    }
                    if (aplicaciones.isEmpty()) {
                        throw new ec.sfjfx.core.util.ValidacionExcepcion("No hay montos por aplicar");
                    }
                    pagos.registrar(proveedor.getValue().id(), total, medio.getValue() == null ? null : medio.getValue().id(),
                            Fx.nuloSiVacio(Fx.texto(referencia)), Fx.nuloSiVacio(Fx.texto(nota)), aplicaciones);
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
