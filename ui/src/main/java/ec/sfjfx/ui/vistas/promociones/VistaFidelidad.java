package ec.sfjfx.ui.vistas.promociones;

import java.math.BigDecimal;

import ec.sfjfx.core.modelo.ProgramaFidelidad;
import ec.sfjfx.core.modelo.PuntosMovimiento;
import ec.sfjfx.core.servicio.ServicioFidelidad;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Modulo de fidelizacion: programa de puntos y saldos/movimientos. */
public final class VistaFidelidad extends BorderPane {

    private final ServicioFidelidad servicio = new ServicioFidelidad();

    public VistaFidelidad() {
        getStyleClass().add("contenido");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(tab("Programa", programa()), tab("Puntos", puntos()));
        setCenter(tabs);
    }

    private Tab tab(String titulo, Node contenido) {
        Tab tab = new Tab(titulo, contenido);
        tab.setClosable(false);
        return tab;
    }

    private Node programa() {
        ProgramaFidelidad p = servicio.programa();
        TextField nombre = Fx.campo(p.nombre());
        TextField puntosPorMonto = Fx.campo(p.puntosPorMonto() == null ? "1" : p.puntosPorMonto().toPlainString());
        TextField valorPunto = Fx.campo(p.valorPunto() == null ? "0.01" : p.valorPunto().toPlainString());
        TextField vigencia = Fx.campo(p.vigenciaPuntosDias() == null ? "" : String.valueOf(p.vigenciaPuntosDias()));
        CheckBox activo = Fx.check("Programa activo", p.activo());

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        g.add(new Label("Nombre"), 0, 0);
        g.add(nombre, 1, 0);
        g.add(new Label("Puntos por unidad monetaria"), 0, 1);
        g.add(puntosPorMonto, 1, 1);
        g.add(new Label("Valor del punto (canje)"), 0, 2);
        g.add(valorPunto, 1, 2);
        g.add(new Label("Vigencia de puntos (dias, opcional)"), 0, 3);
        g.add(vigencia, 1, 3);
        g.add(activo, 1, 4);

        javafx.scene.control.Button guardar = new javafx.scene.control.Button("Guardar");
        guardar.getStyleClass().add("boton-primario");
        guardar.setDisable(!Perm.tiene("fidelizacion.editar") && !Perm.tiene("fidelizacion.crear"));
        guardar.setOnAction(e -> {
            try {
                servicio.guardarPrograma(new ProgramaFidelidad(p.id(), p.empresaId(), Fx.texto(nombre),
                        activo.isSelected(), decimal(Fx.texto(puntosPorMonto)), decimal(Fx.texto(valorPunto)),
                        enteroOFk(Fx.texto(vigencia))));
                Alertas.info("Fidelidad", "Programa guardado");
            } catch (RuntimeException ex) {
                Alertas.error("No se pudo guardar", ex.getMessage());
            }
        });
        g.add(guardar, 1, 5);
        g.setPadding(new Insets(24));
        return g;
    }

    private Node puntos() {
        VistaLista<FilaPuntos> lista = new VistaLista<>("Saldos de puntos por cliente");
        lista.columna("Cliente", 320, FilaPuntos::cliente);
        lista.columna("Puntos", 140, f -> f.saldo().toPlainString());
        lista.buscarPor(FilaPuntos::cliente);
        lista.proveedor(() -> servicio.clientesConPuntos().stream()
                .map(r -> new FilaPuntos((Long) r[0], (String) r[1], (BigDecimal) r[2])).toList());
        lista.accion("Ver movimientos", false, v -> v.seleccion().ifPresent(this::verMovimientos));
        lista.onDobleClic(this::verMovimientos);
        lista.refrescar();
        return lista;
    }

    private void verMovimientos(FilaPuntos f) {
        StringBuilder sb = new StringBuilder("Saldo actual: " + f.saldo().toPlainString() + " puntos\n\n");
        for (PuntosMovimiento m : servicio.movimientos(f.id())) {
            sb.append(String.format("%-17s %-8s %10s  saldo %10s%n",
                    m.fecha() == null ? "" : m.fecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    m.tipo(), valor(m.puntos()), valor(m.saldoResultante())));
        }
        Alertas.info("Movimientos de " + f.cliente(), sb.toString());
    }

    private record FilaPuntos(long id, String cliente, BigDecimal saldo) {
    }

    private int enteroOFk(String v) {
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

    @SuppressWarnings("unused")
    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
