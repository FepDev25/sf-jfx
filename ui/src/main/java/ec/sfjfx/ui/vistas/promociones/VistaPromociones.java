package ec.sfjfx.ui.vistas.promociones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.Marca;
import ec.sfjfx.core.modelo.Promocion;
import ec.sfjfx.core.modelo.PromocionAlcance;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCategoria;
import ec.sfjfx.core.servicio.ServicioMarca;
import ec.sfjfx.core.servicio.ServicioPromocion;
import ec.sfjfx.core.servicio.ServicioSucursal;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaPromociones extends VistaLista<Promocion> {

    private final ServicioPromocion servicio = new ServicioPromocion();

    public VistaPromociones() {
        super("Promociones");
        columna("Nombre", 220, Promocion::nombre);
        columna("Tipo", 130, Promocion::tipo);
        columna("Valor", 100, p -> p.valor() == null ? "" : p.valor().toPlainString());
        columna("Prioridad", 90, p -> String.valueOf(p.prioridad()));
        columna("Vigencia", 180, p -> (p.vigenteDesde() == null ? "" : p.vigenteDesde().toString()) + " - "
                + (p.vigenteHasta() == null ? "" : p.vigenteHasta().toString()));
        columna("Estado", 110, Promocion::estado);
        columna("Activo", 70, p -> Fx.siNo(p.activo()));
        buscarPor(p -> p.nombre() + " " + p.tipo());
        proveedor(servicio::listar);

        if (Perm.tiene("promociones.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("promociones.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(p -> {
            if (Perm.tiene("promociones.editar")) {
                abrirFormulario(p);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Promocion original) {
        boolean nuevo = original == null;
        Promocion base = nuevo
                ? new Promocion(null, SesionActual.requerida().empresa().id(), "", null, "PORCENTAJE",
                        BigDecimal.ZERO, null, null, 0, false, LocalDate.now(), LocalDate.now().plusMonths(1),
                        "ACTIVA", true, List.of(), List.of())
                : original;

        TextField nombre = Fx.campo(base.nombre());
        TextField descripcion = Fx.campo(base.descripcion());
        ComboBox<String> tipo = Fx.combo("PORCENTAJE", "MONTO", "NXM", "PRECIO_FIJO", "COMBO");
        tipo.setValue(base.tipo());
        TextField valor = Fx.campo(base.valor() == null ? "0" : base.valor().toPlainString());
        TextField requerida = Fx.campo(base.cantidadRequerida() == null ? "" : String.valueOf(base.cantidadRequerida()));
        TextField pagada = Fx.campo(base.cantidadPagada() == null ? "" : String.valueOf(base.cantidadPagada()));
        TextField prioridad = Fx.campo(String.valueOf(base.prioridad()));
        DatePicker desde = new DatePicker(base.vigenteDesde());
        DatePicker hasta = new DatePicker(base.vigenteHasta());
        ComboBox<String> estado = Fx.combo("BORRADOR", "ACTIVA", "INACTIVA");
        estado.setValue(base.estado());
        CheckBox activo = Fx.check("Activo", base.activo());

        ComboBox<Categoria> categoria = new ComboBox<>();
        categoria.getItems().add(sentinelCategoria());
        categoria.getItems().addAll(new ServicioCategoria().listar());
        categoria.setValue(categoria.getItems().get(0));
        categoria.getStyleClass().add("selector");
        categoria.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Marca> marca = new ComboBox<>();
        marca.getItems().add(sentinelMarca());
        marca.getItems().addAll(new ServicioMarca().listar());
        marca.setValue(marca.getItems().get(0));
        marca.getStyleClass().add("selector");
        marca.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Sucursal> sucursal = new ComboBox<>();
        sucursal.getItems().add(sentinelSucursal());
        sucursal.getItems().addAll(new ServicioSucursal().listar());
        sucursal.setValue(sucursal.getItems().get(0));
        sucursal.getStyleClass().add("selector");
        sucursal.setMaxWidth(Double.MAX_VALUE);

        new DialogoFormulario(nuevo ? "Nueva promocion" : "Editar promocion", ventana())
                .agregar("Nombre", nombre)
                .agregar("Descripcion", descripcion)
                .agregar("Tipo", tipo)
                .agregar("Valor (% o monto)", valor)
                .agregar("NxM - requerida", requerida)
                .agregar("NxM - pagada", pagada)
                .agregar("Prioridad", prioridad)
                .agregar("Vigente desde", desde)
                .agregar("Vigente hasta", hasta)
                .agregar("Estado", estado)
                .agregar("Categoria (alcance)", categoria)
                .agregar("Marca (alcance)", marca)
                .agregar("Sucursal (alcance)", sucursal)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    List<PromocionAlcance> alcances = new ArrayList<>();
                    if (categoria.getValue() != null && categoria.getValue().id() != null) {
                        alcances.add(new PromocionAlcance(null, 0, "CATEGORIA", categoria.getValue().id()));
                    }
                    if (marca.getValue() != null && marca.getValue().id() != null) {
                        alcances.add(new PromocionAlcance(null, 0, "MARCA", marca.getValue().id()));
                    }
                    if (sucursal.getValue() != null && sucursal.getValue().id() != null) {
                        alcances.add(new PromocionAlcance(null, 0, "SUCURSAL", sucursal.getValue().id()));
                    }
                    servicio.guardar(new Promocion(base.id(), base.empresaId(), Fx.texto(nombre),
                            Fx.nuloSiVacio(Fx.texto(descripcion)), tipo.getValue(), decimal(Fx.texto(valor)),
                            enteroONull(Fx.texto(requerida)), enteroONull(Fx.texto(pagada)), entero(Fx.texto(prioridad)),
                            base.combinable(), desde.getValue(), hasta.getValue(), estado.getValue(), activo.isSelected(),
                            alcances, List.of()));
                    refrescar();
                })
                .mostrar();
    }

    private Categoria sentinelCategoria() {
        return new Categoria(null, SesionActual.requerida().empresa().id(), null, null, "(ninguna)", null, 0, true);
    }

    private Marca sentinelMarca() {
        return new Marca(null, SesionActual.requerida().empresa().id(), "(ninguna)", null, null, true);
    }

    private Sucursal sentinelSucursal() {
        return new Sucursal(null, SesionActual.requerida().empresa().id(), "", "(ninguna)", null, null, false, true);
    }

    private int entero(String v) {
        try {
            return v == null || v.isBlank() ? 0 : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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
