package ec.sfjfx.ui.vistas.ventas;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.servicio.ServicioVenta;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

import javafx.stage.Window;

public final class VistaVentasConsulta extends VistaLista<Venta> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServicioVenta servicio = new ServicioVenta();

    public VistaVentasConsulta() {
        super("Ventas");
        columna("Numero", 160, Venta::numero);
        columna("Fecha", 150, v -> v.fecha() == null ? "" : v.fecha().format(FORMATO));
        columna("Cliente", 180, v -> v.clienteNombre() == null ? "Consumidor final" : v.clienteNombre());
        columna("Total", 110, v -> v.total() == null ? "" : v.total().toPlainString());
        columna("Estado", 110, Venta::estado);
        buscarPor(v -> v.numero() + " " + (v.clienteNombre() == null ? "" : v.clienteNombre()));
        proveedor(() -> servicio.listar(null));

        accion("Ver", false, v -> v.seleccion().ifPresent(this::ver));
        if (Perm.tiene("pos.anular")) {
            accion("Anular", false, v -> v.seleccion().ifPresent(this::anular));
        }
        onDobleClic(this::ver);
        refrescar();
    }

    private void ver(Venta v) {
        Venta completa = servicio.porId(v.id());
        StringBuilder sb = new StringBuilder("Venta " + completa.numero() + "  (" + completa.estado() + ")\n\n");
        for (VentaDetalle d : completa.detalles()) {
            sb.append(String.format("%-34s %6s x %8s = %8s%n", d.descripcion(),
                    d.cantidad().stripTrailingZeros().toPlainString(), d.precioUnitario().toPlainString(),
                    d.total().toPlainString()));
        }
        sb.append("\nSubtotal: ").append(completa.subtotal()).append("\nIVA: ").append(completa.impuestoTotal())
                .append("\nTotal: ").append(completa.total());
        Alertas.info("Venta " + completa.numero(), sb.toString());
    }

    private void anular(Venta v) {
        Bodega bodega = Combos.bodegaVenta();
        if (bodega == null) {
            Alertas.error("Anular", "No hay bodega disponible");
            return;
        }
        try {
            servicio.anular(v.id(), bodega.id());
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo anular", e.getMessage());
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
