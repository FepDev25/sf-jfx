package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.FacturaProveedor;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.modelo.Retencion;
import ec.sfjfx.core.modelo.RetencionDetalle;
import ec.sfjfx.core.servicio.ServicioFacturaProveedor;
import ec.sfjfx.core.servicio.ServicioRetencion;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaRetenciones extends VistaLista<Retencion> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ServicioRetencion servicio = new ServicioRetencion();
    private final ServicioFacturaProveedor facturas = new ServicioFacturaProveedor();

    public VistaRetenciones() {
        super("Comprobantes de retencion");
        columna("Proveedor", 220, Retencion::proveedorNombre);
        columna("Secuencial", 130, Retencion::numero);
        columna("Fecha", 110, r -> r.fechaEmision() == null ? "" : r.fechaEmision().format(FORMATO));
        columna("Base IVA", 100, r -> valor(r.baseIva()));
        columna("Ret. IVA", 100, r -> valor(r.valorRetencionIva()));
        columna("Ret. Renta", 100, r -> valor(r.valorRetencionRenta()));
        columna("Total", 100, r -> valor(r.totalRetenido()));
        buscarPor(r -> r.numero() + " " + r.proveedorNombre());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Emitir", true, v -> emitir());
        }
        refrescar();
    }

    private void emitir() {
        ComboBox<Proveedor> proveedor = Combos.proveedores();
        ComboBox<FacturaProveedor> factura = new ComboBox<>();
        factura.getStyleClass().add("selector");
        factura.setMaxWidth(Double.MAX_VALUE);
        factura.setPromptText("(opcional)");

        Runnable cargarFacturas = () -> {
            factura.getItems().clear();
            if (proveedor.getValue() != null) {
                factura.getItems().setAll(facturas.listar(null).stream()
                        .filter(f -> f.proveedorId() == proveedor.getValue().id()).toList());
            }
        };
        proveedor.valueProperty().addListener((o, a, b) -> cargarFacturas.run());
        cargarFacturas.run();

        DatePicker fecha = new DatePicker(LocalDate.now());
        TextField baseIva = Fx.campo("0");
        TextField pctIva = Fx.campo("0");
        TextField baseRenta = Fx.campo("0");
        TextField pctRenta = Fx.campo("0");

        new DialogoFormulario("Emitir retencion", ventana())
                .agregar("Proveedor", proveedor)
                .agregar("Factura", factura)
                .agregar("Fecha", fecha)
                .agregar("Base IVA", baseIva)
                .agregar("% Retencion IVA", pctIva)
                .agregar("Base Renta", baseRenta)
                .agregar("% Retencion Renta", pctRenta)
                .alGuardar(() -> {
                    List<RetencionDetalle> detalles = new ArrayList<>();
                    agregarLinea(detalles, "IVA", "725", decimal(Fx.texto(baseIva)), decimal(Fx.texto(pctIva)));
                    agregarLinea(detalles, "RENTA", "312", decimal(Fx.texto(baseRenta)), decimal(Fx.texto(pctRenta)));
                    if (detalles.isEmpty()) {
                        throw new ec.sfjfx.core.util.ValidacionExcepcion("Indique al menos una retencion con % mayor a cero");
                    }
                    Long facturaId = factura.getValue() == null ? null : factura.getValue().id();
                    servicio.emitir(proveedor.getValue().id(), facturaId, fecha.getValue(), detalles);
                    refrescar();
                })
                .mostrar();
    }

    private void agregarLinea(List<RetencionDetalle> detalles, String tipo, String codigo,
                              BigDecimal base, BigDecimal porcentaje) {
        if (porcentaje.signum() <= 0 || base.signum() <= 0) {
            return;
        }
        BigDecimal valor = base.multiply(porcentaje).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        detalles.add(new RetencionDetalle(null, 0, tipo, codigo, base, porcentaje, valor));
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
