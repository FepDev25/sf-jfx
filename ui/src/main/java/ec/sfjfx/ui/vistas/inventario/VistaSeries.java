package ec.sfjfx.ui.vistas.inventario;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Serie;
import ec.sfjfx.core.servicio.ServicioSerie;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaSeries extends VistaLista<Serie> {

    private final ServicioSerie servicio = new ServicioSerie();

    public VistaSeries() {
        super("Numeros de serie");
        columna("Serie", 220, Serie::numeroSerie);
        columna("Producto", 300, Serie::productoNombre);
        columna("Estado", 130, Serie::estado);
        columna("Bodega", 170, s -> s.bodega() == null ? "-" : s.bodega());
        buscarPor(s -> s.numeroSerie() + " " + s.productoNombre() + " " + s.estado());
        proveedor(() -> servicio.listar(null));

        if (Perm.tiene("inventario.crear")) {
            accion("Registrar", true, v -> registrar());
        }
        if (Perm.tiene("inventario.editar")) {
            accion("Cambiar estado", false, v -> v.seleccion().ifPresent(this::cambiarEstado));
        }
        refrescar();
    }

    private void registrar() {
        ComboBox<Producto> producto = Combos.productos();
        TextField numero = Fx.campo("");
        ComboBox<Bodega> bodega = Combos.bodegas();
        new DialogoFormulario("Registrar numero de serie", ventana())
                .agregar("Producto", producto)
                .agregar("Numero de serie", numero)
                .agregar("Bodega", bodega)
                .alGuardar(() -> {
                    servicio.registrar(producto.getValue().id(), Fx.texto(numero),
                            bodega.getValue() == null ? null : bodega.getValue().id(), null);
                    refrescar();
                })
                .mostrar();
    }

    private void cambiarEstado(Serie s) {
        ComboBox<String> estado = Fx.combo("DISPONIBLE", "RESERVADO", "VENDIDO", "BAJA");
        estado.setValue(s.estado());
        new DialogoFormulario("Estado de la serie " + s.numeroSerie(), ventana())
                .agregar("Estado", estado)
                .alGuardar(() -> {
                    servicio.cambiarEstado(s.id(), estado.getValue());
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
