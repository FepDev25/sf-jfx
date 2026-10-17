package ec.sfjfx.ui.vistas.facturacion;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;
import ec.sfjfx.core.modelo.DevolucionVenta;
import ec.sfjfx.core.modelo.Retencion;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.servicio.ServicioComprobante;
import ec.sfjfx.core.servicio.ServicioDevolucionVenta;
import ec.sfjfx.core.servicio.ServicioRetencion;
import ec.sfjfx.core.servicio.ServicioVenta;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaComprobantes extends VistaLista<Comprobante> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioComprobante servicio = new ServicioComprobante();

    public VistaComprobantes() {
        super("Comprobantes electronicos (simulado: sin envio al SRI)");
        columna("Numero", 150, Comprobante::numeroCompleto);
        columna("Tipo", 150, c -> c.tipo());
        columna("Fecha", 110, c -> c.fechaEmision() == null ? "" : c.fechaEmision().toString());
        columna("Receptor", 240, c -> c.receptorRazonSocial());
        columna("Total", 110, c -> c.total() == null ? "" : FormatoReporte.moneda(c.total()));
        columna("Estado", 100, Comprobante::estado);
        buscarPor(c -> c.numeroCompleto() + " " + c.receptorRazonSocial() + " " + c.claveAcceso());
        proveedorPaginado(servicio::pagina);

        if (Perm.tiene("facturacion.crear")) {
            accion("Factura de venta", true, v -> emitirFactura());
            accion("Nota de credito", false, v -> emitirNotaCredito());
            accion("Retencion", false, v -> emitirRetencion());
        }
        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        accion("Reimprimir RIDE", false, v -> v.seleccion().ifPresent(this::reimprimir));
        if (Perm.tiene("facturacion.anular")) {
            accion("Anular", false, v -> v.seleccion().ifPresent(this::anular));
        }
        onDobleClic(this::ver);
        refrescar();
    }

    private void emitirFactura() {
        List<Venta> pagadas = new ServicioVenta().listar(null).stream().filter(Venta::pagada).toList();
        if (pagadas.isEmpty()) {
            Alertas.advertencia("Sin ventas", "No hay ventas pagadas para facturar.");
            return;
        }
        ComboBox<Venta> venta = new ComboBox<>();
        venta.getItems().setAll(pagadas);
        venta.setValue(pagadas.get(0));
        venta.getStyleClass().add("selector");
        venta.setMaxWidth(Double.MAX_VALUE);
        new DialogoFormulario("Emitir factura desde venta", ventana())
                .agregar("Venta", venta)
                .alGuardar(() -> {
                    long id = servicio.emitirFacturaDesdeVenta(venta.getValue().id());
                    Alertas.info("Factura emitida", "Comprobante " + servicio.porId(id).numeroCompleto());
                    refrescar();
                })
                .mostrar();
    }

    private void emitirNotaCredito() {
        List<DevolucionVenta> devs = new ServicioDevolucionVenta().listar();
        if (devs.isEmpty()) {
            Alertas.advertencia("Sin devoluciones", "No hay devoluciones de venta.");
            return;
        }
        ComboBox<DevolucionVenta> devolucion = new ComboBox<>();
        devolucion.getItems().setAll(devs);
        devolucion.setValue(devs.get(0));
        devolucion.getStyleClass().add("selector");
        devolucion.setMaxWidth(Double.MAX_VALUE);
        new DialogoFormulario("Emitir nota de credito", ventana())
                .agregar("Devolucion", devolucion)
                .alGuardar(() -> {
                    long id = servicio.emitirNotaCreditoDevolucion(devolucion.getValue().id());
                    Alertas.info("Nota de credito emitida", "Comprobante " + servicio.porId(id).numeroCompleto());
                    refrescar();
                })
                .mostrar();
    }

    private void emitirRetencion() {
        List<Retencion> rets = new ServicioRetencion().listar(null);
        if (rets.isEmpty()) {
            Alertas.advertencia("Sin retenciones", "No hay comprobantes de retencion registrados.");
            return;
        }
        ComboBox<Retencion> retencion = new ComboBox<>();
        retencion.getItems().setAll(rets);
        retencion.setValue(rets.get(0));
        retencion.getStyleClass().add("selector");
        retencion.setMaxWidth(Double.MAX_VALUE);
        new DialogoFormulario("Emitir comprobante de retencion", ventana())
                .agregar("Retencion", retencion)
                .alGuardar(() -> {
                    long id = servicio.emitirRetencion(retencion.getValue().id());
                    Alertas.info("Retencion emitida", "Comprobante " + servicio.porId(id).numeroCompleto());
                    refrescar();
                })
                .mostrar();
    }

    private void ver(Comprobante c) {
        Comprobante completo = servicio.porId(c.id());
        StringBuilder sb = new StringBuilder();
        sb.append("Tipo: ").append(completo.tipo()).append("\n");
        sb.append("Numero: ").append(completo.numeroCompleto()).append("\n");
        sb.append("Clave de acceso: ").append(completo.claveAcceso()).append("\n");
        sb.append("Receptor: ").append(completo.receptorRazonSocial()).append(" (")
                .append(completo.receptorNumeroId()).append(")\n");
        sb.append("Total: ").append(completo.total()).append("\n");
        sb.append("Estado: ").append(completo.estado()).append("\n\n");
        for (ComprobanteDetalle d : completo.detalles()) {
            sb.append(String.format("%-34s %8s x %8s = %8s%n", d.descripcion(), valor(d.cantidad()),
                    valor(d.precioUnitario()), valor(d.total())));
        }
        sb.append("\nRIDE: ").append(completo.pdfPath() == null ? "(no generado)" : completo.pdfPath());
        sb.append("\nXML: ").append(completo.xmlPath() == null ? "(no generado)" : completo.xmlPath());
        sb.append("\nXML firmado: ").append(completo.xmlFirmadoPath() == null ? "(sin firma)" : completo.xmlFirmadoPath());
        Alertas.info("Comprobante " + completo.numeroCompleto(), sb.toString());
    }

    private void reimprimir(Comprobante c) {
        try {
            String ruta = servicio.reimprimirRide(c.id());
            Alertas.info("RIDE regenerado", ruta);
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo reimprimir", e.getMessage());
        }
    }

    private void anular(Comprobante c) {
        TextField motivo = Fx.campo("");
        new DialogoFormulario("Anular comprobante " + c.numeroCompleto(), ventana())
                .agregar("Motivo", motivo)
                .alGuardar(() -> {
                    servicio.anular(c.id(), Fx.texto(motivo));
                    refrescar();
                })
                .mostrar();
    }

    private String valor(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
