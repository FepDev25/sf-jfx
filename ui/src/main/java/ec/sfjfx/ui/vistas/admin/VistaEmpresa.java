package ec.sfjfx.ui.vistas.admin;

import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.servicio.ServicioEmpresa;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class VistaEmpresa extends VistaLista<Empresa> {

    private final ServicioEmpresa servicio = new ServicioEmpresa();

    public VistaEmpresa() {
        super("Empresas");
        columna("RUC", 130, Empresa::ruc);
        columna("Razon social", 260, Empresa::razonSocial);
        columna("Nombre comercial", 200, Empresa::nombreComercial);
        columna("Regimen", 190, Empresa::regimen);
        columna("Activo", 70, e -> Fx.siNo(e.activo()));
        buscarPor(e -> e.ruc() + " " + e.razonSocial() + " " + e.nombreComercial());

        if (Perm.tiene("empresa.crear")) {
            accion("Nueva", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("empresa.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        onDobleClic(e -> {
            if (Perm.tiene("empresa.editar")) {
                abrirFormulario(e);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Empresa original) {
        boolean nuevo = original == null;
        Empresa base = nuevo
                ? new Empresa(null, "", "", null, null, null, null, null, false, true, "GENERAL", "USD", true)
                : original;

        TextField ruc = Fx.campo(base.ruc());
        TextField razon = Fx.campo(base.razonSocial());
        TextField comercial = Fx.campo(base.nombreComercial());
        TextField direccion = Fx.campo(base.direccionMatriz());
        TextField telefono = Fx.campo(base.telefono());
        TextField email = Fx.campo(base.email());
        ComboBox<String> regimen = Fx.combo("GENERAL", "RIMPE_EMPRENDEDOR", "RIMPE_NEGOCIO_POPULAR");
        regimen.setValue(base.regimen() == null ? "GENERAL" : base.regimen());
        CheckBox especial = Fx.check("Contribuyente especial", base.contribuyenteEspecial());
        CheckBox contabilidad = Fx.check("Obligado a llevar contabilidad", base.obligadoContabilidad());
        CheckBox activo = Fx.check("Activo", base.activo());

        DialogoFormulario dialogo = new DialogoFormulario(
                nuevo ? "Nueva empresa" : "Editar empresa", ventana());
        dialogo.agregar("RUC", ruc)
                .agregar("Razon social", razon)
                .agregar("Nombre comercial", comercial)
                .agregar("Direccion", direccion)
                .agregar("Telefono", telefono)
                .agregar("Email", email)
                .agregar("Regimen", regimen)
                .agregarAnchoCompleto(especial)
                .agregarAnchoCompleto(contabilidad)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    Empresa guardada = new Empresa(
                            base.id(), Fx.texto(ruc), Fx.texto(razon), Fx.nuloSiVacio(Fx.texto(comercial)),
                            Fx.nuloSiVacio(Fx.texto(direccion)), Fx.nuloSiVacio(Fx.texto(telefono)),
                            Fx.nuloSiVacio(Fx.texto(email)), base.logoPath(), especial.isSelected(),
                            contabilidad.isSelected(), regimen.getValue(), "USD", activo.isSelected());
                    servicio.guardar(guardada);
                    refrescar();
                })
                .mostrar();
    }

    private javafx.stage.Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
