package ec.sfjfx.ui.vistas.admin;

import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioSucursal;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaSucursales extends VistaLista<Sucursal> {

    private final ServicioSucursal servicio = new ServicioSucursal();

    public VistaSucursales() {
        super("Sucursales");
        columna("Codigo", 90, Sucursal::codigo);
        columna("Nombre", 220, Sucursal::nombre);
        columna("Direccion", 260, Sucursal::direccion);
        columna("Telefono", 130, Sucursal::telefono);
        columna("Matriz", 70, s -> Fx.siNo(s.esMatriz()));
        columna("Activo", 70, s -> Fx.siNo(s.activo()));
        buscarPor(s -> s.codigo() + " " + s.nombre());

        if (Perm.tiene("sucursales.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("sucursales.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(s -> {
            if (Perm.tiene("sucursales.editar")) {
                abrirFormulario(s);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Sucursal original) {
        boolean nuevo = original == null;
        long empresaId = SesionActual.requerida().empresa().id();
        Sucursal base = nuevo
                ? new Sucursal(null, empresaId, "", "", null, null, false, true)
                : original;

        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());
        TextField direccion = Fx.campo(base.direccion());
        TextField telefono = Fx.campo(base.telefono());
        CheckBox matriz = Fx.check("Es matriz", base.esMatriz());
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nueva sucursal" : "Editar sucursal", ventana())
                .agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Direccion", direccion)
                .agregar("Telefono", telefono)
                .agregarAnchoCompleto(matriz)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    servicio.guardar(new Sucursal(base.id(), base.empresaId(), Fx.texto(codigo),
                            Fx.texto(nombre), Fx.nuloSiVacio(Fx.texto(direccion)),
                            Fx.nuloSiVacio(Fx.texto(telefono)), matriz.isSelected(), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
