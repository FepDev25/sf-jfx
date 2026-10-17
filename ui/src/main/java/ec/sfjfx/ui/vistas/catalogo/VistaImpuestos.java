package ec.sfjfx.ui.vistas.catalogo;

import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.TipoImpuesto;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioImpuesto;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import java.math.BigDecimal;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaImpuestos extends VistaLista<Impuesto> {

    private final ServicioImpuesto servicio = new ServicioImpuesto();

    public VistaImpuestos() {
        super("Impuestos");
        columna("Nombre", 220, Impuesto::nombre);
        columna("Tipo", 110, i -> i.tipo().etiqueta());
        columna("Codigo SRI", 100, Impuesto::codigoSri);
        columna("Porcentaje", 110, i -> i.porcentaje() == null ? "" : i.porcentaje().toPlainString() + " %");
        columna("Vigente hasta", 120, i -> i.vigenteHasta() == null ? "-" : i.vigenteHasta().toString());
        columna("Activo", 70, i -> Fx.siNo(i.activo()));
        buscarPor(i -> i.nombre() + " " + i.tipo().name());
        proveedor(servicio::listar);

        if (Perm.tiene("productos.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("productos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(i -> {
            if (Perm.tiene("productos.editar")) {
                abrirFormulario(i);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Impuesto original) {
        Impuesto base = original == null
                ? new Impuesto(null, SesionActual.requerida().empresa().id(), null, "", TipoImpuesto.IVA,
                        BigDecimal.ZERO, null, null, false, true)
                : original;
        TextField nombre = Fx.campo(base.nombre());
        ComboBox<TipoImpuesto> tipo = new ComboBox<>();
        tipo.getItems().setAll(TipoImpuesto.values());
        tipo.setValue(base.tipo());
        tipo.getStyleClass().add("selector");
        tipo.setMaxWidth(Double.MAX_VALUE);
        TextField codigoSri = Fx.campo(base.codigoSri());
        TextField porcentaje = Fx.campo(base.porcentaje() == null ? "0" : base.porcentaje().toPlainString());
        DatePicker desde = new DatePicker(base.vigenteDesde());
        DatePicker hasta = new DatePicker(base.vigenteHasta());
        CheckBox retencion = Fx.check("Es impuesto de retencion", base.esRetencion());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(original == null ? "Nuevo impuesto" : "Editar impuesto", ventana())
                .agregar("Nombre", nombre)
                .agregar("Tipo", tipo)
                .agregar("Codigo SRI", codigoSri)
                .agregar("Porcentaje", porcentaje)
                .agregar("Vigente desde", desde)
                .agregar("Vigente hasta", hasta)
                .agregarAnchoCompleto(retencion)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Impuesto(base.id(), base.empresaId(), Fx.nuloSiVacio(Fx.texto(codigoSri)),
                            Fx.texto(nombre), tipo.getValue(), decimal(Fx.texto(porcentaje)),
                            desde.getValue(), hasta.getValue(), retencion.isSelected(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private BigDecimal decimal(String v) {
        try {
            return new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
