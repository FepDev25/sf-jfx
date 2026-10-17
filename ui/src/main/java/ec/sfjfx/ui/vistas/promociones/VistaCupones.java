package ec.sfjfx.ui.vistas.promociones;

import java.math.BigDecimal;
import java.time.LocalDate;

import ec.sfjfx.core.modelo.Cupon;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCupon;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaCupones extends VistaLista<Cupon> {

    private final ServicioCupon servicio = new ServicioCupon();

    public VistaCupones() {
        super("Cupones");
        columna("Codigo", 180, Cupon::codigo);
        columna("Tipo", 120, Cupon::tipo);
        columna("Valor", 100, c -> c.valor() == null ? "" : c.valor().toPlainString());
        columna("Vigencia", 200, c -> (c.vigenteDesde() == null ? "" : c.vigenteDesde().toString()) + " - "
                + (c.vigenteHasta() == null ? "" : c.vigenteHasta().toString()));
        columna("Usos", 100, c -> c.usosActuales() + (c.usosMaximos() == null ? "" : "/" + c.usosMaximos()));
        columna("Activo", 70, c -> Fx.siNo(c.activo()));
        buscarPor(c -> c.codigo() + " " + c.tipo());
        proveedor(servicio::listar);

        if (Perm.tiene("promociones.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("promociones.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(c -> {
            if (Perm.tiene("promociones.editar")) {
                abrirFormulario(c);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Cupon original) {
        boolean nuevo = original == null;
        Cupon base = nuevo
                ? new Cupon(null, SesionActual.requerida().empresa().id(), "", "PORCENTAJE", BigDecimal.ZERO,
                        LocalDate.now(), LocalDate.now().plusMonths(1), null, 0, true)
                : original;

        TextField codigo = Fx.campo(base.codigo());
        ComboBox<String> tipo = Fx.combo("PORCENTAJE", "MONTO");
        tipo.setValue(base.tipo());
        TextField valor = Fx.campo(base.valor() == null ? "0" : base.valor().toPlainString());
        DatePicker desde = new DatePicker(base.vigenteDesde());
        DatePicker hasta = new DatePicker(base.vigenteHasta());
        TextField usos = Fx.campo(base.usosMaximos() == null ? "" : String.valueOf(base.usosMaximos()));
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nuevo cupon" : "Editar cupon", ventana())
                .agregar("Codigo", codigo)
                .agregar("Tipo", tipo)
                .agregar("Valor", valor)
                .agregar("Vigente desde", desde)
                .agregar("Vigente hasta", hasta)
                .agregar("Usos maximos (opcional)", usos)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Cupon(base.id(), base.empresaId(), Fx.texto(codigo), tipo.getValue(),
                            decimal(Fx.texto(valor)), desde.getValue(), hasta.getValue(),
                            enteroONull(Fx.texto(usos)), base.usosActuales(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private Integer enteroONull(String v) {
        try {
            return v == null || v.isBlank() ? null : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
