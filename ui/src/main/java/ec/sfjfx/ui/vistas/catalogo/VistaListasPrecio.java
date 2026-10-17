package ec.sfjfx.ui.vistas.catalogo;

import java.math.BigDecimal;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.TipoListaPrecio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCategoria;
import ec.sfjfx.core.servicio.ServicioListaPrecio;
import ec.sfjfx.core.servicio.ServicioPrecioMasivo;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaListasPrecio extends VistaLista<ListaPrecio> {

    private final ServicioListaPrecio servicio = new ServicioListaPrecio();

    public VistaListasPrecio() {
        super("Listas de precio");
        columna("Nombre", 220, ListaPrecio::nombre);
        columna("Tipo", 140, l -> l.tipo().etiqueta());
        columna("Moneda", 90, ListaPrecio::moneda);
        columna("Incluye IVA", 110, l -> Fx.siNo(l.incluyeIva()));
        columna("Default", 90, l -> Fx.siNo(l.esDefault()));
        columna("Activo", 70, l -> Fx.siNo(l.activo()));
        buscarPor(l -> l.nombre() + " " + l.tipo().name());
        proveedor(servicio::listar);

        if (Perm.tiene("listas_precio.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("listas_precio.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
            accion("Ajuste masivo", false, v -> v.seleccion().ifPresent(this::ajusteMasivo));
        }
        onDobleClic(l -> {
            if (Perm.tiene("listas_precio.editar")) {
                abrirFormulario(l);
            }
        });
        refrescar();
    }

    private void abrirFormulario(ListaPrecio original) {
        ListaPrecio base = original == null
                ? new ListaPrecio(null, SesionActual.requerida().empresa().id(), "", TipoListaPrecio.PUBLICO,
                        "USD", false, false, true)
                : original;
        TextField nombre = Fx.campo(base.nombre());
        ComboBox<TipoListaPrecio> tipo = new ComboBox<>();
        tipo.getItems().setAll(TipoListaPrecio.values());
        tipo.setValue(base.tipo());
        tipo.getStyleClass().add("selector");
        tipo.setMaxWidth(Double.MAX_VALUE);
        TextField moneda = Fx.campo(base.moneda());
        CheckBox incluyeIva = Fx.check("Precio incluye IVA", base.incluyeIva());
        CheckBox esDefault = Fx.check("Lista por defecto", base.esDefault());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(original == null ? "Nueva lista" : "Editar lista", ventana())
                .agregar("Nombre", nombre)
                .agregar("Tipo", tipo)
                .agregar("Moneda", moneda)
                .agregarAnchoCompleto(incluyeIva)
                .agregarAnchoCompleto(esDefault)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new ListaPrecio(base.id(), base.empresaId(), Fx.texto(nombre), tipo.getValue(),
                            Fx.texto(moneda), incluyeIva.isSelected(), esDefault.isSelected(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private void ajusteMasivo(ListaPrecio lista) {
        ComboBox<Categoria> categoria = new ComboBox<>();
        categoria.getItems().add(new Categoria(null, lista.empresaId(), null, null, "(todas las categorias)", null, 0, true));
        categoria.getItems().addAll(new ServicioCategoria().listar());
        categoria.setValue(categoria.getItems().get(0));
        categoria.getStyleClass().add("selector");
        categoria.setMaxWidth(Double.MAX_VALUE);
        TextField porcentaje = Fx.campo("0");
        new DialogoFormulario("Ajuste masivo - " + lista.nombre(), ventana())
                .agregar("Categoria", categoria)
                .agregar("Porcentaje (%)", porcentaje)
                .alGuardar(() -> {
                    Long categoriaId = categoria.getValue() == null ? null : categoria.getValue().id();
                    int n = new ServicioPrecioMasivo().aplicar(lista.id(), categoriaId, decimal(Fx.texto(porcentaje)));
                    Alertas.info("Ajuste masivo", "Precios actualizados: " + n);
                })
                .mostrar();
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}