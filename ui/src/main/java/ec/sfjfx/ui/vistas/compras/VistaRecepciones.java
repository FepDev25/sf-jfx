package ec.sfjfx.ui.vistas.compras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.OrdenCompra;
import ec.sfjfx.core.modelo.OrdenCompraDetalle;
import ec.sfjfx.core.modelo.RecepcionCompra;
import ec.sfjfx.core.modelo.RecepcionDetalle;
import ec.sfjfx.core.servicio.ServicioOrdenCompra;
import ec.sfjfx.core.servicio.ServicioRecepcion;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaRecepciones extends VistaLista<RecepcionCompra> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioRecepcion servicio = new ServicioRecepcion();
    private final ServicioOrdenCompra ordenes = new ServicioOrdenCompra();

    public VistaRecepciones() {
        super("Recepciones de compra");
        columna("Numero", 160, RecepcionCompra::numero);
        columna("Bodega", 180, RecepcionCompra::bodega);
        columna("Estado", 120, RecepcionCompra::estado);
        columna("Fecha", 160, r -> r.fecha() == null ? "" : r.fecha().format(FORMATO));
        buscarPor(r -> r.numero() + " " + r.bodega());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("compras.crear")) {
            accion("Nueva", true, v -> nueva());
        }
        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        onDobleClic(this::ver);
        refrescar();
    }

    private void nueva() {
        List<OrdenCompra> pendientes = ordenes.listar(null).stream()
                .filter(o -> "APROBADA".equals(o.estado()) || "RECEPCION_PARCIAL".equals(o.estado())).toList();
        if (pendientes.isEmpty()) {
            ec.sfjfx.ui.util.Alertas.advertencia("Sin ordenes", "No hay ordenes aprobadas por recibir.");
            return;
        }
        ComboBox<OrdenCompra> orden = new ComboBox<>();
        orden.getItems().setAll(pendientes);
        orden.setValue(pendientes.get(0));
        orden.getStyleClass().add("selector");
        orden.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Bodega> bodega = Combos.bodegas();
        TextField nota = Fx.campo("");

        GridPane grilla = new GridPane();
        grilla.setHgap(8);
        grilla.setVgap(6);
        grilla.setPadding(new Insets(6));
        grilla.add(new Label("Producto"), 0, 0);
        grilla.add(new Label("Pendiente"), 1, 0);
        grilla.add(new Label("Recibir"), 2, 0);
        grilla.add(new Label("Lote"), 3, 0);
        grilla.add(new Label("Vence"), 4, 0);

        List<OrdenCompraDetalle> detalles = new ArrayList<>();
        List<TextField> camposCantidad = new ArrayList<>();
        List<TextField> camposLote = new ArrayList<>();
        List<DatePicker> camposVence = new ArrayList<>();
        int[] fila = {1};

        Runnable cargar = () -> {
            grilla.getChildren().removeIf(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) >= 1);
            detalles.clear();
            camposCantidad.clear();
            camposLote.clear();
            camposVence.clear();
            fila[0] = 1;
            if (orden.getValue() == null) {
                return;
            }
            OrdenCompra completa = ordenes.porId(orden.getValue().id());
            for (OrdenCompraDetalle d : completa.detalles()) {
                if (d.pendiente().signum() <= 0) {
                    continue;
                }
                detalles.add(d);
                grilla.add(new Label(d.productoNombre()), 0, fila[0]);
                grilla.add(new Label(valor(d.pendiente())), 1, fila[0]);
                TextField cant = Fx.campo(valor(d.pendiente()));
                cant.setPrefWidth(80);
                camposCantidad.add(cant);
                grilla.add(cant, 2, fila[0]);
                TextField lote = Fx.campo("");
                lote.setPrefWidth(110);
                camposLote.add(lote);
                grilla.add(lote, 3, fila[0]);
                DatePicker vence = new DatePicker();
                camposVence.add(vence);
                grilla.add(vence, 4, fila[0]);
                fila[0]++;
            }
        };
        orden.valueProperty().addListener((o, a, b) -> cargar.run());
        cargar.run();

        VBox caja = new VBox(8, grilla);
        caja.setPadding(new Insets(4, 0, 4, 0));

        new DialogoFormulario("Recepcion de compra", ventana())
                .agregar("Orden", orden)
                .agregar("Bodega", bodega)
                .agregar("Nota", nota)
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    List<RecepcionDetalle> lineas = new ArrayList<>();
                    for (int i = 0; i < detalles.size(); i++) {
                        OrdenCompraDetalle d = detalles.get(i);
                        BigDecimal cant = decimal(Fx.texto(camposCantidad.get(i)));
                        if (cant.signum() <= 0) {
                            continue;
                        }
                        if (cant.compareTo(d.pendiente()) > 0) {
                            throw new ec.sfjfx.core.util.ValidacionExcepcion(
                                    "La cantidad recibida supera lo pendiente de " + d.productoNombre());
                        }
                        lineas.add(new RecepcionDetalle(null, 0, d.id(), d.productoId(), d.productoNombre(), null,
                                Fx.nuloSiVacio(Fx.texto(camposLote.get(i))), camposVence.get(i).getValue(), cant,
                                d.precioUnitario()));
                    }
                    if (lineas.isEmpty()) {
                        throw new ec.sfjfx.core.util.ValidacionExcepcion("No hay cantidades por recibir");
                    }
                    servicio.registrar(orden.getValue().id(), bodega.getValue().id(), Fx.nuloSiVacio(Fx.texto(nota)), lineas);
                    refrescar();
                })
                .mostrar();
    }

    private void ver(RecepcionCompra r) {
        RecepcionCompra completa = servicio.porId(r.id());
        StringBuilder sb = new StringBuilder("Bodega: " + completa.bodega() + "\n\n");
        for (RecepcionDetalle d : completa.detalles()) {
            sb.append(String.format("%-36s %10s x %8s%n", d.productoNombre(), valor(d.cantidadRecibida()),
                    valor(d.costoUnitario())));
        }
        ec.sfjfx.ui.util.Alertas.info("Recepcion " + completa.numero(), sb.toString());
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String valor(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }

    @SuppressWarnings("unused")
    private LocalDate hoy() {
        return LocalDate.now();
    }
}
