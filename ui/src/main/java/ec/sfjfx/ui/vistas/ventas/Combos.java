package ec.sfjfx.ui.vistas.ventas;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;

import javafx.scene.control.ComboBox;

/** Combos compartidos para las vistas de ventas. */
final class Combos {

    private Combos() {
    }

    static long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    static ComboBox<Bodega> bodegas() {
        ComboBox<Bodega> combo = new ComboBox<>();
        combo.getItems().setAll(new BodegaRepositorio().porEmpresa(empresa()));
        combo.getStyleClass().add("selector");
        combo.setMaxWidth(Double.MAX_VALUE);
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        return combo;
    }

    static ComboBox<MedioPagoRepositorio.MedioPago> mediosPago() {
        ComboBox<MedioPagoRepositorio.MedioPago> combo = new ComboBox<>();
        combo.getItems().setAll(new MedioPagoRepositorio().listar());
        combo.getStyleClass().add("selector");
        combo.setMaxWidth(Double.MAX_VALUE);
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        return combo;
    }

    static Bodega bodegaVenta() {
        Sucursal sucursal = SesionActual.requerida().sucursal();
        BodegaRepositorio repo = new BodegaRepositorio();
        if (sucursal != null) {
            Bodega b = repo.bodegaVentaDeSucursal(sucursal.id()).orElse(null);
            if (b != null) {
                return b;
            }
        }
        return repo.porEmpresa(empresa()).stream().findFirst().orElse(null);
    }
}
