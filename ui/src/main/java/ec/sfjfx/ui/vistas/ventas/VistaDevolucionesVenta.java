package ec.sfjfx.ui.vistas.ventas;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.DevolucionVenta;
import ec.sfjfx.core.modelo.DevolucionVentaDetalle;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.servicio.ServicioDevolucionVenta;
import ec.sfjfx.core.servicio.ServicioVenta;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaDevolucionesVenta extends VistaLista<DevolucionVenta> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioDevolucionVenta servicio = new ServicioDevolucionVenta();
    private final ServicioVenta ventas = new ServicioVenta();

    public VistaDevolucionesVenta() {
        super("Devoluciones de venta");
        columna("Numero", 150, DevolucionVenta::numero);
        columna("Venta", 150, DevolucionVenta::ventaNumero);
        columna("Fecha", 150, d -> d.fecha() == null ? "" : d.fecha().format(FORMATO));
        columna("Motivo", 160, DevolucionVenta::motivo);
        columna("Total", 110, d -> d.total() == null ? "" : d.total().toPlainString());
        columna("Estado", 100, DevolucionVenta::estado);
        buscarPor(d -> d.numero() + " " + d.ventaNumero());
        proveedor(servicio::listar);

        if (Perm.tiene("pos.anular")) {
            accion("Registrar", true, v -> registrar());
        }
        refrescar();
    }

    private void registrar() {
        List<Venta> pagadas = ventas.listar(null).stream().filter(v -> v.pagada()).limit(200).toList();
        if (pagadas.isEmpty()) {
            ec.sfjfx.ui.util.Alertas.advertencia("Sin ventas", "No hay ventas pagadas para devolver.");
            return;
        }
        ComboBox<Venta> venta = new ComboBox<>();
        venta.getItems().setAll(pagadas);
        venta.setValue(pagadas.get(0));
        venta.getStyleClass().add("selector");
        venta.setMaxWidth(Double.MAX_VALUE);

        GridPane grilla = new GridPane();
        grilla.setHgap(8);
        grilla.setVgap(6);
        grilla.setPadding(new Insets(6));
        grilla.add(new Label("Producto"), 0, 0);
        grilla.add(new Label("Vendido"), 1, 0);
        grilla.add(new Label("Devolver"), 2, 0);
        grilla.add(new Label("Reingresa"), 3, 0);

        List<VentaDetalle> detalles = new ArrayList<>();
        List<TextField> camposCantidad = new ArrayList<>();
        List<CheckBox> checksReingresa = new ArrayList<>();
        int[] fila = {1};

        Runnable cargar = () -> {
            grilla.getChildren().removeIf(n -> javafx.scene.layout.GridPane.getRowIndex(n) != null
                    && javafx.scene.layout.GridPane.getRowIndex(n) >= 1);
            detalles.clear();
            camposCantidad.clear();
            checksReingresa.clear();
            fila[0] = 1;
            if (venta.getValue() == null) {
                return;
            }
            for (VentaDetalle d : ventas.porId(venta.getValue().id()).detalles()) {
                detalles.add(d);
                grilla.add(new Label(d.descripcion()), 0, fila[0]);
                grilla.add(new Label(d.cantidad().stripTrailingZeros().toPlainString()), 1, fila[0]);
                TextField cant = Fx.campo("0");
                cant.setPrefWidth(80);
                camposCantidad.add(cant);
                grilla.add(cant, 2, fila[0]);
                CheckBox reingresa = new CheckBox();
                reingresa.setSelected(true);
                checksReingresa.add(reingresa);
                grilla.add(reingresa, 3, fila[0]);
                fila[0]++;
            }
        };
        venta.valueProperty().addListener((o, a, b) -> cargar.run());
        cargar.run();

        ComboBox<Bodega> bodega = Combos.bodegas();
        ComboBox<String> motivo = Fx.combo("DEVOLUCION", "PRODUCTO_DANADO", "CLIENTE_DESISTE", "OTRO");
        VBox caja = new VBox(8, grilla);

        new DialogoFormulario("Registrar devolucion de venta", ventana())
                .agregar("Venta", venta)
                .agregar("Bodega", bodega)
                .agregar("Motivo", motivo)
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    List<DevolucionVentaDetalle> lineas = new ArrayList<>();
                    for (int i = 0; i < detalles.size(); i++) {
                        BigDecimal cant = decimal(Fx.texto(camposCantidad.get(i)));
                        if (cant.signum() > 0) {
                            VentaDetalle d = detalles.get(i);
                            lineas.add(new DevolucionVentaDetalle(null, 0, d.productoId(), d.descripcion(), cant,
                                    d.precioUnitario(), BigDecimal.ZERO, checksReingresa.get(i).isSelected()));
                        }
                    }
                    if (lineas.isEmpty()) {
                        throw new ec.sfjfx.core.util.ValidacionExcepcion("Indique al menos una cantidad a devolver");
                    }
                    servicio.registrar(venta.getValue().id(), bodega.getValue().id(), motivo.getValue(), lineas);
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

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
