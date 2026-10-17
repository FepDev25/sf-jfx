package ec.sfjfx.ui.vistas.catalogo;

import ec.sfjfx.core.modelo.TipoUnidad;
import ec.sfjfx.core.modelo.UnidadMedida;
import ec.sfjfx.core.servicio.ServicioUnidadMedida;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaUnidades extends VistaLista<UnidadMedida> {

    private final ServicioUnidadMedida servicio = new ServicioUnidadMedida();

    public VistaUnidades() {
        super("Unidades de medida");
        columna("Codigo", 120, UnidadMedida::codigo);
        columna("Nombre", 220, UnidadMedida::nombre);
        columna("Abrev.", 90, UnidadMedida::abreviatura);
        columna("Tipo", 120, u -> u.tipo().etiqueta());
        columna("Decimales", 90, u -> Fx.siNo(u.permiteDecimales()));
        columna("Activo", 70, u -> Fx.siNo(u.activo()));
        buscarPor(u -> u.codigo() + " " + u.nombre());
        proveedor(servicio::listar);

        if (Perm.tiene("productos.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("productos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(u -> {
            if (Perm.tiene("productos.editar")) {
                abrirFormulario(u);
            }
        });
        refrescar();
    }

    private void abrirFormulario(UnidadMedida original) {
        UnidadMedida base = original == null
                ? new UnidadMedida(null, "", "", "", TipoUnidad.UNIDAD, false, true) : original;
        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());
        TextField abreviatura = Fx.campo(base.abreviatura());
        ComboBox<TipoUnidad> tipo = new ComboBox<>();
        tipo.getItems().setAll(TipoUnidad.values());
        tipo.setValue(base.tipo());
        tipo.getStyleClass().add("selector");
        tipo.setMaxWidth(Double.MAX_VALUE);
        CheckBox decimales = Fx.check("Permite decimales", base.permiteDecimales());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(original == null ? "Nueva unidad" : "Editar unidad", ventana())
                .agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Abreviatura", abreviatura)
                .agregar("Tipo", tipo)
                .agregarAnchoCompleto(decimales)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new UnidadMedida(base.id(), Fx.texto(codigo), Fx.texto(nombre),
                            Fx.texto(abreviatura), tipo.getValue(), decimales.isSelected(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
