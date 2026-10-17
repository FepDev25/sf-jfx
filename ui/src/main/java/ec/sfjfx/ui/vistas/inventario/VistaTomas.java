package ec.sfjfx.ui.vistas.inventario;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.TomaDetalle;
import ec.sfjfx.core.modelo.TomaInventario;
import ec.sfjfx.core.servicio.ServicioToma;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
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

public final class VistaTomas extends VistaLista<TomaInventario> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioToma servicio = new ServicioToma();

    public VistaTomas() {
        super("Tomas de inventario");
        columna("Numero", 150, TomaInventario::numero);
        columna("Bodega", 170, TomaInventario::bodega);
        columna("Estado", 120, TomaInventario::estado);
        columna("Conteo ciego", 110, t -> Fx.siNo(t.conteoCiego()));
        columna("Inicio", 150, t -> t.fechaInicio() == null ? "" : t.fechaInicio().format(FORMATO));
        buscarPor(t -> t.numero() + " " + t.bodega() + " " + t.estado());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("inventario.crear")) {
            accion("Abrir toma", true, v -> abrir());
            accion("Contar", false, v -> v.seleccion().ifPresent(this::contar));
        }
        if (Perm.tiene("inventario.aprobar")) {
            accion("Aplicar", false, v -> v.seleccion().ifPresent(this::aplicar));
        }
        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        onDobleClic(this::ver);
        refrescar();
    }

    private void abrir() {
        ComboBox<Bodega> bodega = Combos.bodegas();
        CheckBox ciego = Fx.check("Conteo ciego (no muestra la cantidad del sistema)", true);
        TextField observacion = Fx.campo("");
        new DialogoFormulario("Abrir toma de inventario", ventana())
                .agregar("Bodega", bodega)
                .agregar("Observacion", observacion)
                .agregarAnchoCompleto(ciego)
                .alGuardar(() -> {
                    try {
                        servicio.abrir(bodega.getValue().id(), ciego.isSelected(), Fx.nuloSiVacio(Fx.texto(observacion)));
                        refrescar();
                    } catch (RuntimeException e) {
                        Alertas.error("No se pudo abrir la toma", e.getMessage());
                    }
                })
                .mostrar();
    }

    private void contar(TomaInventario t) {
        TomaInventario toma = servicio.porId(t.id());
        if ("APLICADA".equals(toma.estado()) || "ANULADA".equals(toma.estado())) {
            Alertas.advertencia("Toma", "La toma ya no se puede contar");
            return;
        }
        GridPane grilla = new GridPane();
        grilla.setHgap(12);
        grilla.setVgap(6);
        grilla.setPadding(new Insets(8));
        if (!toma.conteoCiego()) {
            grilla.add(new Label("Producto"), 0, 0);
            grilla.add(new Label("Sistema"), 1, 0);
            grilla.add(new Label("Conteo"), 2, 0);
        } else {
            grilla.add(new Label("Producto"), 0, 0);
            grilla.add(new Label("Conteo"), 2, 0);
        }
        Map<Long, TextField> campos = new HashMap<>();
        int fila = 1;
        for (TomaDetalle d : toma.detalles()) {
            grilla.add(new Label(d.productoNombre()), 0, fila);
            if (!toma.conteoCiego()) {
                grilla.add(new Label(valor(d.cantidadSistema())), 1, fila);
            }
            TextField campo = Fx.campo(valor(d.cantidadContada()));
            campo.setPrefWidth(100);
            campos.put(d.productoId(), campo);
            grilla.add(campo, 2, fila);
            fila++;
        }

        VBox caja = new VBox(8, grilla);
        caja.setPadding(new Insets(4));

        new DialogoFormulario("Conteo - " + toma.numero() + " (" + toma.bodega() + ")", ventana())
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    Map<Long, BigDecimal> conteos = new HashMap<>();
                    campos.forEach((productoId, campo) -> conteos.put(productoId, decimal(Fx.texto(campo))));
                    servicio.guardarConteos(toma.id(), conteos);
                    refrescar();
                })
                .mostrar();
    }

    private void aplicar(TomaInventario t) {
        try {
            servicio.aplicar(t.id());
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo aplicar", e.getMessage());
        }
    }

    private void ver(TomaInventario t) {
        TomaInventario toma = servicio.porId(t.id());
        StringBuilder sb = new StringBuilder("Bodega: " + toma.bodega() + "\nEstado: " + toma.estado()
                + "\nConteo ciego: " + Fx.siNo(toma.conteoCiego()) + "\n\n");
        for (TomaDetalle d : toma.detalles()) {
            sb.append(String.format("%-38s sistema %10s  contado %10s  dif %10s%n",
                    d.productoNombre(), valor(d.cantidadSistema()), valor(d.cantidadContada()), valor(d.diferencia())));
        }
        Alertas.info("Toma " + toma.numero(), sb.toString());
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
}
