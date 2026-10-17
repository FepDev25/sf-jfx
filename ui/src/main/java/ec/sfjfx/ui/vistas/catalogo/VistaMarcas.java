package ec.sfjfx.ui.vistas.catalogo;

import ec.sfjfx.core.modelo.Marca;
import ec.sfjfx.core.servicio.ServicioMarca;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaMarcas extends VistaLista<Marca> {

    private final ServicioMarca servicio = new ServicioMarca();

    public VistaMarcas() {
        super("Marcas");
        columna("Nombre", 240, Marca::nombre);
        columna("Descripcion", 360, Marca::descripcion);
        columna("Activo", 70, m -> Fx.siNo(m.activo()));
        buscarPor(m -> m.nombre() + " " + (m.descripcion() == null ? "" : m.descripcion()));
        proveedor(servicio::listar);

        if (Perm.tiene("productos.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("productos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(m -> {
            if (Perm.tiene("productos.editar")) {
                abrirFormulario(m);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Marca original) {
        Marca base = original == null ? new Marca(null, 0, "", "", null, true) : original;
        TextField nombre = Fx.campo(base.nombre());
        TextArea descripcion = new TextArea(base.descripcion() == null ? "" : base.descripcion());
        descripcion.getStyleClass().add("campo");
        descripcion.setPrefRowCount(3);
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(original == null ? "Nueva marca" : "Editar marca", ventana())
                .agregar("Nombre", nombre)
                .agregar("Descripcion", descripcion)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Marca(base.id(),
                            base.id() == null ? ec.sfjfx.core.seguridad.SesionActual.requerida().empresa().id()
                                    : base.empresaId(),
                            Fx.texto(nombre), Fx.nuloSiVacio(descripcion.getText()), base.logoPath(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
