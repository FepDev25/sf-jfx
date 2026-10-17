package ec.sfjfx.ui.vistas.inventario;

import java.time.LocalDate;

import ec.sfjfx.core.modelo.Lote;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.servicio.ServicioLote;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaLotes extends VistaLista<Lote> {

    private final ServicioLote servicio = new ServicioLote();

    public VistaLotes() {
        super("Lotes y vencimientos");
        columna("Codigo", 180, Lote::codigo);
        columna("Producto", 300, Lote::productoNombre);
        columna("Fabricacion", 130, l -> l.fechaFabricacion() == null ? "-" : l.fechaFabricacion().toString());
        columna("Vencimiento", 130, l -> l.fechaVencimiento() == null ? "-" : l.fechaVencimiento().toString());
        columna("Activo", 70, l -> Fx.siNo(l.activo()));
        buscarPor(l -> l.codigo() + " " + l.productoNombre());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("inventario.crear")) {
            accion("Nuevo", true, v -> nuevo());
        }
        refrescar();
    }

    private void nuevo() {
        ComboBox<Producto> producto = Combos.productos();
        TextField codigo = Fx.campo("");
        DatePicker fabricacion = new DatePicker();
        DatePicker vencimiento = new DatePicker(LocalDate.now().plusMonths(6));
        new DialogoFormulario("Nuevo lote", ventana())
                .agregar("Producto", producto)
                .agregar("Codigo de lote", codigo)
                .agregar("Fabricacion", fabricacion)
                .agregar("Vencimiento", vencimiento)
                .alGuardar(() -> {
                    servicio.crear(new Lote(null, producto.getValue().id(), null, Fx.texto(codigo),
                            fabricacion.getValue(), vencimiento.getValue(), true));
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
