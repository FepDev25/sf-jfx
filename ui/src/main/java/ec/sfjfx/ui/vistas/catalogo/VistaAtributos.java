package ec.sfjfx.ui.vistas.catalogo;

import ec.sfjfx.core.modelo.AtributoDefinicion;
import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.TipoDatoAtributo;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioAtributo;
import ec.sfjfx.core.servicio.ServicioCategoria;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaAtributos extends VistaLista<AtributoDefinicion> {

    private final ServicioAtributo servicio = new ServicioAtributo();
    private final ServicioCategoria servicioCategoria = new ServicioCategoria();

    public VistaAtributos() {
        super("Atributos personalizados");
        columna("Codigo", 160, AtributoDefinicion::codigo);
        columna("Nombre", 220, AtributoDefinicion::nombre);
        columna("Tipo", 120, a -> a.tipoDato().etiqueta());
        columna("Obligatorio", 100, a -> Fx.siNo(a.obligatorio()));
        columna("Orden", 70, AtributoDefinicion::orden);
        columna("Activo", 70, a -> Fx.siNo(a.activo()));
        buscarPor(a -> a.codigo() + " " + a.nombre());
        proveedor(servicio::listar);

        if (Perm.tiene("productos.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("productos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(a -> {
            if (Perm.tiene("productos.editar")) {
                abrirFormulario(a);
            }
        });
        refrescar();
    }

    private void abrirFormulario(AtributoDefinicion original) {
        long empresa = SesionActual.requerida().empresa().id();
        AtributoDefinicion base = original == null
                ? new AtributoDefinicion(null, empresa, null, "", "", TipoDatoAtributo.TEXTO, null, false, 0, true)
                : original;

        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());

        Categoria todas = new Categoria(null, empresa, null, null, "(todas las categorias)", null, 0, true);
        ComboBox<Categoria> categoria = new ComboBox<>();
        categoria.getItems().add(todas);
        categoria.getItems().addAll(servicioCategoria.listar());
        categoria.getStyleClass().add("selector");
        categoria.setMaxWidth(Double.MAX_VALUE);
        categoria.getItems().stream().filter(c -> c.id() != null && c.id().equals(base.categoriaId()))
                .findFirst().ifPresentOrElse(categoria::setValue, () -> categoria.setValue(todas));

        ComboBox<TipoDatoAtributo> tipo = new ComboBox<>();
        tipo.getItems().setAll(TipoDatoAtributo.values());
        tipo.setValue(base.tipoDato());
        tipo.getStyleClass().add("selector");
        tipo.setMaxWidth(Double.MAX_VALUE);

        TextField opciones = Fx.campo(base.opciones());
        opciones.setPromptText("Opciones separadas por coma (solo tipo Lista)");
        TextField orden = Fx.campo(String.valueOf(base.orden()));
        CheckBox obligatorio = Fx.check("Obligatorio", base.obligatorio());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(original == null ? "Nuevo atributo" : "Editar atributo", ventana())
                .agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Categoria", categoria)
                .agregar("Tipo de dato", tipo)
                .agregar("Opciones", opciones)
                .agregar("Orden", orden)
                .agregarAnchoCompleto(obligatorio)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    Long categoriaId = categoria.getValue() == null ? null : categoria.getValue().id();
                    servicio.guardar(new AtributoDefinicion(base.id(), base.empresaId(), categoriaId,
                            Fx.texto(codigo), Fx.texto(nombre), tipo.getValue(), Fx.nuloSiVacio(Fx.texto(opciones)),
                            obligatorio.isSelected(), entero(Fx.texto(orden)), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private int entero(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
