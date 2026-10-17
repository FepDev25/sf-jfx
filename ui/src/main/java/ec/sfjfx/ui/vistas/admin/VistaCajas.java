package ec.sfjfx.ui.vistas.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCaja;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaCajas extends VistaLista<Caja> {

    private final ServicioCaja servicio = new ServicioCaja();
    private final CajaRepositorio repo = new CajaRepositorio();
    private final SucursalRepositorio sucursales = new SucursalRepositorio();
    private final Map<Long, String> nombreSucursal = new HashMap<>();

    public VistaCajas() {
        super("Cajas");
        columna("Codigo", 100, Caja::codigo);
        columna("Nombre", 200, Caja::nombre);
        columna("Sucursal", 180, k -> nombreSucursal.getOrDefault(k.sucursalId(), "-"));
        columna("Estado", 130, Caja::estado);
        columna("Activo", 70, k -> Fx.siNo(k.activo()));
        buscarPor(k -> k.codigo() + " " + k.nombre() + " " + k.estado());
        proveedor(() -> repo.porEmpresa(empresaActual()));

        if (Perm.tiene("cajas.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("cajas.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(k -> {
            if (Perm.tiene("cajas.editar")) {
                abrirFormulario(k);
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

    private void abrirFormulario(Caja original) {
        List<Sucursal> disponibles = sucursales.porEmpresa(empresaActual());
        if (disponibles.isEmpty()) {
            Alertas.advertencia("Sin sucursales", "Cree una sucursal antes de agregar cajas.");
            return;
        }
        boolean nuevo = original == null;
        Caja base = nuevo
                ? new Caja(null, disponibles.get(0).id(), "", "", "DISPONIBLE", true)
                : original;

        ComboBox<Sucursal> sucursal = new ComboBox<>();
        sucursal.getItems().setAll(disponibles);
        sucursal.getStyleClass().add("selector");
        sucursal.setMaxWidth(Double.MAX_VALUE);
        sucursal.getItems().stream().filter(s -> s.id() == base.sucursalId()).findFirst()
                .ifPresentOrElse(sucursal::setValue, () -> sucursal.setValue(disponibles.get(0)));

        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());
        ComboBox<String> estado = Fx.combo("DISPONIBLE", "OCUPADA", "MANTENIMIENTO");
        estado.setValue(base.estado() == null ? "DISPONIBLE" : base.estado());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nueva caja" : "Editar caja", ventana())
                .agregar("Sucursal", sucursal)
                .agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Estado", estado)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Caja(base.id(), sucursal.getValue().id(), Fx.texto(codigo),
                            Fx.texto(nombre), estado.getValue(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
