package ec.sfjfx.ui.vistas.ventas;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioSesionCaja;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaSesionesCaja extends VistaLista<SesionCaja> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BigDecimal[] DENOMINACIONES = {
            new BigDecimal("0.01"), new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("0.25"),
            new BigDecimal("0.50"), new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("5"),
            new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("100")};

    private final ServicioSesionCaja servicio = new ServicioSesionCaja();

    public VistaSesionesCaja() {
        super("Sesiones de caja (turnos)");
        columna("Caja", 150, SesionCaja::caja);
        columna("Apertura", 150, s -> s.fechaApertura() == null ? "" : s.fechaApertura().format(FORMATO));
        columna("Cierre", 150, s -> s.fechaCierre() == null ? "" : s.fechaCierre().format(FORMATO));
        columna("Inicial", 100, s -> valor(s.montoInicial()));
        columna("Esperado", 100, s -> valor(s.montoEsperado()));
        columna("Contado", 100, s -> valor(s.montoContado()));
        columna("Diferencia", 100, s -> valor(s.diferencia()));
        columna("Estado", 100, SesionCaja::estado);
        buscarPor(s -> s.caja() + " " + s.estado());
        proveedor(servicio::listar);

        if (Perm.tiene("pos.crear")) {
            accion("Abrir", true, v -> abrir());
        }
        if (Perm.tiene("pos.editar")) {
            accion("Cerrar / arqueo", false, v -> v.seleccion().ifPresent(this::cerrar));
        }
        accion("Refrescar", false, v -> refrescar());
        refrescar();
    }

    private void abrir() {
        ComboBox<Caja> caja = new ComboBox<>();
        caja.getItems().setAll(new CajaRepositorio().porEmpresa(SesionActual.requerida().empresa().id()));
        caja.getStyleClass().add("selector");
        caja.setMaxWidth(Double.MAX_VALUE);
        if (!caja.getItems().isEmpty()) {
            caja.setValue(caja.getItems().get(0));
        }
        TextField monto = Fx.campo("0");
        new DialogoFormulario("Abrir sesion de caja", ventana())
                .agregar("Caja", caja)
                .agregar("Monto inicial", monto)
                .alGuardar(() -> {
                    servicio.abrir(caja.getValue().id(), decimal(Fx.texto(monto)));
                    refrescar();
                })
                .mostrar();
    }

    private void cerrar(SesionCaja sesion) {
        if (!sesion.abierta()) {
            Alertas.advertencia("Sesion cerrada", "Esta sesion ya esta cerrada.");
            return;
        }
        GridPane grilla = new GridPane();
        grilla.setHgap(10);
        grilla.setVgap(6);
        grilla.setPadding(new Insets(8));
        Map<BigDecimal, TextField> campos = new LinkedHashMap<>();
        Label total = new Label();
        int fila = 0;
        for (BigDecimal d : DENOMINACIONES) {
            grilla.add(new Label("$ " + d.toPlainString()), 0, fila);
            TextField cantidad = Fx.campo("0");
            cantidad.setPrefWidth(80);
            campos.put(d, cantidad);
            cantidad.textProperty().addListener((o, a, b) -> actualizarTotal(campos, total));
            grilla.add(cantidad, 1, fila);
            fila++;
        }
        BigDecimal esperado = servicio.esperadoEfectivo(sesion.id());
        actualizarTotal(campos, total);
        TextField observacion = Fx.campo("");
        VBox caja = new VBox(8, new Label("Esperado en efectivo: $ " + valor(esperado) + " (inicial + ventas efectivo)"),
                grilla, total, new Label("Observacion"), observacion);
        caja.setPadding(new Insets(4));

        new DialogoFormulario("Cierre / arqueo - Caja " + sesion.caja(), ventana())
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    Map<BigDecimal, Integer> arqueo = new LinkedHashMap<>();
                    campos.forEach((d, campo) -> {
                        int c = entero(Fx.texto(campo));
                        if (c > 0) {
                            arqueo.put(d, c);
                        }
                    });
                    servicio.cerrar(sesion.id(), arqueo, Fx.nuloSiVacio(Fx.texto(observacion)));
                    refrescar();
                })
                .mostrar();
    }

    private void actualizarTotal(Map<BigDecimal, TextField> campos, Label total) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, TextField> e : campos.entrySet()) {
            suma = suma.add(e.getKey().multiply(BigDecimal.valueOf(entero(Fx.texto(e.getValue())))));
        }
        total.setText("Contado: $ " + valor(suma));
    }

    private int entero(String v) {
        try {
            return v == null || v.isBlank() ? 0 : Integer.parseInt(v.trim());
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

    private String valor(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
