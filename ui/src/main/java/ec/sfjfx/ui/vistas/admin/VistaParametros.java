package ec.sfjfx.ui.vistas.admin;

import ec.sfjfx.core.modelo.Configuracion;
import ec.sfjfx.core.servicio.ServicioConfiguracion;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaParametros extends VistaLista<Configuracion> {

    private final ServicioConfiguracion servicio = new ServicioConfiguracion();

    public VistaParametros() {
        super("Parametros de la empresa");
        columna("Clave", 220, Configuracion::clave);
        columna("Valor", 220, Configuracion::valor);
        columna("Grupo", 140, Configuracion::grupo);
        columna("Tipo", 110, Configuracion::tipo);
        buscarPor(c -> c.clave() + " " + c.valor() + " " + c.grupo());
        proveedor(servicio::listar);

        if (Perm.tiene("configuracion.crear") || Perm.tiene("configuracion.editar")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        if (Perm.tiene("configuracion.eliminar")) {
            accion("Eliminar", false, v -> v.seleccion().ifPresent(this::eliminar));
        }
        onDobleClic(c -> {
            if (Perm.tiene("configuracion.editar")) {
                abrirFormulario(c);
            }
        });
        refrescar();
    }

    private void abrirFormulario(Configuracion original) {
        Configuracion base = original == null
                ? new Configuracion(null, 0, "", "", "TEXTO", null, null)
                : original;

        TextField clave = Fx.campo(base.clave());
        TextField valor = Fx.campo(base.valor());
        ComboBox<String> tipo = Fx.combo("TEXTO", "NUMERO", "BOOLEAN", "JSON", "FECHA");
        tipo.setValue(base.tipo() == null ? "TEXTO" : base.tipo());
        TextField grupo = Fx.campo(base.grupo());
        TextField descripcion = Fx.campo(base.descripcion());

        new DialogoFormulario(original == null ? "Nuevo parametro" : "Editar parametro", ventana())
                .agregar("Clave", clave)
                .agregar("Valor", valor)
                .agregar("Tipo", tipo)
                .agregar("Grupo", grupo)
                .agregar("Descripcion", descripcion)
                .alGuardar(() -> {
                    servicio.guardar(Fx.texto(clave), Fx.texto(valor), tipo.getValue(),
                            Fx.nuloSiVacio(Fx.texto(grupo)), Fx.nuloSiVacio(Fx.texto(descripcion)));
                    refrescar();
                })
                .mostrar();
    }

    private void eliminar(Configuracion c) {
        try {
            servicio.eliminar(c.id());
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo eliminar", e.getMessage());
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
