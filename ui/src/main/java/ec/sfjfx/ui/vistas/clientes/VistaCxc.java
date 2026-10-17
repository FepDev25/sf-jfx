package ec.sfjfx.ui.vistas.clientes;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.CxcMovimiento;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.servicio.ServicioCxc;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaCxc extends VistaLista<Cliente> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioCxc servicio = new ServicioCxc();

    public VistaCxc() {
        super("Cuentas por cobrar");
        columna("Cliente", 300, Cliente::nombreParaMostrar);
        columna("Limite credito", 130, c -> c.limiteCredito() == null ? "" : FormatoReporte.moneda(c.limiteCredito()));
        columna("Saldo por cobrar", 150, c -> c.saldo() == null ? "" : FormatoReporte.moneda(c.saldo()));
        buscarPor(Cliente::nombreParaMostrar);
        proveedor(servicio::conSaldo);

        if (Perm.tiene("clientes.editar")) {
            accion("Abonar", true, v -> v.seleccion().ifPresent(this::abonar));
        }
        accion("Estado de cuenta", false, v -> v.seleccion().ifPresent(this::estadoCuenta));
        onDobleClic(this::estadoCuenta);
        refrescar();
    }

    private void abonar(Cliente cliente) {
        TextField monto = Fx.campo("0");
        ComboBox<MedioPagoRepositorio.MedioPago> medio = new ComboBox<>();
        medio.getItems().setAll(new MedioPagoRepositorio().listar());
        medio.getStyleClass().add("selector");
        medio.setMaxWidth(Double.MAX_VALUE);
        if (!medio.getItems().isEmpty()) {
            medio.setValue(medio.getItems().get(0));
        }
        TextField nota = Fx.campo("");
        new DialogoFormulario("Abono - " + cliente.nombreParaMostrar() + " (saldo $" + cliente.saldo() + ")", ventana())
                .agregar("Monto", monto)
                .agregar("Medio de pago", medio)
                .agregar("Nota", nota)
                .alGuardar(() -> {
                    servicio.abonar(cliente.id(), decimal(Fx.texto(monto)),
                            medio.getValue() == null ? null : medio.getValue().id(), Fx.nuloSiVacio(Fx.texto(nota)));
                    refrescar();
                })
                .mostrar();
    }

    private void estadoCuenta(Cliente cliente) {
        StringBuilder sb = new StringBuilder("Saldo actual: $ " + cliente.saldo() + "\n\n");
        for (CxcMovimiento m : servicio.listar(cliente.id())) {
            sb.append(String.format("%-17s %-6s %10s  saldo %10s%n",
                    m.fecha() == null ? "" : m.fecha().format(FORMATO), m.tipo(), valor(m.monto()),
                    valor(m.saldoResultante())));
        }
        VBox caja = new VBox(6, new Label(sb.toString()));
        caja.setPadding(new Insets(12));
        Alertas.info("Estado de cuenta - " + cliente.nombreParaMostrar(), sb.toString());
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
