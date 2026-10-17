package ec.sfjfx.ui.vistas.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioBodega;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaBodegas extends VistaLista<Bodega> {

    private final ServicioBodega servicio = new ServicioBodega();
    private final BodegaRepositorio repo = new BodegaRepositorio();
    private final SucursalRepositorio sucursales = new SucursalRepositorio();
    private final Map<Long, String> nombreSucursal = new HashMap<>();

    public VistaBodegas() {
        super("Bodegas");
        columna("Codigo", 100, Bodega::codigo);
        columna("Nombre", 220, Bodega::nombre);
        columna("Sucursal", 180, b -> nombreSucursal.getOrDefault(b.sucursalId(), "-"));
        columna("Tipo", 130, Bodega::tipo);
        columna("Activo", 70, b -> Fx.siNo(b.activo()));
        buscarPor(b -> b.codigo() + " " + b.nombre() + " " + b.tipo());
        proveedor(() -> repo.porEmpresa(empresaActual()));

        if (Perm.tiene("bodegas.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("bodegas.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(b -> {
            if (Perm.tiene("bodegas.editar")) {
                abrirFormulario(b);
            }
        });
        recargar();
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    private void recargar() {
        nombreSucursal.clear();
        for (Sucursal s : sucursales.porEmpresa(empresaActual())) {
            nombreSucursal.put(s.id(), s.nombre());
        }
        refrescar();
    }

    private void abrirFormulario(Bodega original) {
        List<Sucursal> disponibles = sucursales.porEmpresa(empresaActual());
        if (disponibles.isEmpty()) {
            ec.sfjfx.ui.util.Alertas.advertencia("Sin sucursales", "Cree una sucursal antes de agregar bodegas.");
            return;
        }
        boolean nuevo = original == null;
        Bodega base = nuevo
                ? new Bodega(null, disponibles.get(0).id(), "", "", "ALMACEN", null, true)
                : original;

        ComboBox<Sucursal> sucursal = new ComboBox<>();
        sucursal.getItems().setAll(disponibles);
        sucursal.getStyleClass().add("selector");
        sucursal.setMaxWidth(Double.MAX_VALUE);
        seleccionar(sucursal, base.sucursalId());

        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());
        ComboBox<String> tipo = Fx.combo("VENTA", "ALMACEN", "TRANSITO", "MERMA", "CONSIGNA", "PRODUCCION");
        tipo.setValue(base.tipo() == null ? "ALMACEN" : base.tipo());
        TextField direccion = Fx.campo(base.direccion());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nueva bodega" : "Editar bodega", ventana())
                .agregar("Sucursal", sucursal)
                .agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Tipo", tipo)
                .agregar("Direccion", direccion)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Bodega(base.id(), sucursal.getValue().id(), Fx.texto(codigo),
                            Fx.texto(nombre), tipo.getValue(), Fx.nuloSiVacio(Fx.texto(direccion)),
                            activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private void seleccionar(ComboBox<Sucursal> combo, long sucursalId) {
        combo.getItems().stream().filter(s -> s.id() == sucursalId).findFirst()
                .ifPresentOrElse(combo::setValue, () -> {
                    if (!combo.getItems().isEmpty()) {
                        combo.setValue(combo.getItems().get(0));
                    }
                });
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
