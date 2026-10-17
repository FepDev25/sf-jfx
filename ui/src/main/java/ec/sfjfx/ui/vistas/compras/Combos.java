package ec.sfjfx.ui.vistas.compras;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioProducto;
import ec.sfjfx.core.servicio.ServicioProveedor;

import javafx.scene.control.ComboBox;

/** Combos compartidos para las vistas de compras. */
final class Combos {

    private Combos() {
    }

    static long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    static ComboBox<Proveedor> proveedores() {
        ComboBox<Proveedor> combo = new ComboBox<>();
        combo.getItems().setAll(new ServicioProveedor().listar(null));
        combo.getStyleClass().add("selector");
        combo.setMaxWidth(Double.MAX_VALUE);
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        return combo;
    }

    static ComboBox<Producto> productos() {
        ComboBox<Producto> combo = new ComboBox<>();
        combo.getItems().setAll(new ServicioProducto().listar(null));
        combo.getStyleClass().add("selector");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
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

    static ComboBox<Sucursal> sucursales() {
        ComboBox<Sucursal> combo = new ComboBox<>();
        combo.getItems().setAll(new SucursalRepositorio().porEmpresa(empresa()));
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
}
