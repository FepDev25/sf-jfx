package ec.sfjfx.ui.vistas.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.modelo.Permiso;
import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.repositorio.PermisoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioRol;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaRoles extends VistaLista<Rol> {

    private final ServicioRol servicio = new ServicioRol();
    private final PermisoRepositorio permisosRepo = new PermisoRepositorio();

    public VistaRoles() {
        super("Roles y permisos");
        columna("Rol", 200, Rol::nombre);
        columna("Descripcion", 320, Rol::descripcion);
        columna("Sistema", 90, r -> Fx.siNo(r.esSistema()));
        columna("Activo", 70, r -> Fx.siNo(r.activo()));
        buscarPor(r -> r.nombre() + " " + (r.descripcion() == null ? "" : r.descripcion()));
        proveedor(servicio::listar);

        if (Perm.tiene("roles.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("roles.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        if (Perm.tiene("roles.eliminar")) {
            accion("Eliminar", false, v -> v.seleccion().ifPresent(this::eliminar));
        }
        onDobleClic(r -> {
            if (Perm.tiene("roles.editar")) {
                abrirFormulario(r);
            }
        });
        refrescar();
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    private void abrirFormulario(Rol original) {
        boolean nuevo = original == null;
        Rol base = nuevo ? new Rol(null, empresaActual(), "", null, false, true) : original;

        TextField nombre = Fx.campo(base.nombre());
        TextArea descripcion = new TextArea(base.descripcion() == null ? "" : base.descripcion());
        descripcion.setPrefRowCount(2);
        descripcion.getStyleClass().add("campo");
        CheckBox activo = Fx.check("Activo", base.activo());

        Set<String> seleccionados = nuevo ? Set.of() : servicio.permisosDe(base.id());
        MatrizPermisos matriz = new MatrizPermisos(permisosRepo.listarTodos(), seleccionados);

        new DialogoFormulario(nuevo ? "Nuevo rol" : "Editar rol: " + base.nombre(), ventana())
                .agregar("Nombre", nombre)
                .agregar("Descripcion", descripcion)
                .agregarAnchoCompleto(activo)
                .agregarAnchoCompleto(new Label("Permisos"))
                .agregarAnchoCompleto(matriz)
                .alGuardar(() -> {
                    servicio.guardar(new Rol(base.id(), base.empresaId(), Fx.texto(nombre),
                            Fx.nuloSiVacio(descripcion.getText()), base.esSistema(), activo.isSelected()),
                            matriz.seleccionados());
                    refrescar();
                })
                .mostrar();
    }

    private void eliminar(Rol rol) {
        try {
            servicio.eliminar(rol);
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo eliminar", e.getMessage());
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }

    /** Matriz de permisos agrupada por modulo. */
    private static final class MatrizPermisos extends VBox {

        private final List<CheckBox> checks = new ArrayList<>();

        MatrizPermisos(List<Permiso> permisos, Set<String> seleccionados) {
            setSpacing(10);
            setPadding(new Insets(4, 0, 4, 0));

            Map<String, List<Permiso>> porModulo = new LinkedHashMap<>();
            for (Permiso p : permisos) {
                porModulo.computeIfAbsent(p.modulo(), k -> new ArrayList<>()).add(p);
            }

            for (Map.Entry<String, List<Permiso>> entrada : porModulo.entrySet()) {
                VBox bloque = new VBox(4);
                Label titulo = new Label(entrada.getKey());
                titulo.getStyleClass().add("etiqueta-form");

                FlowPane acciones = new FlowPane(10, 6);
                for (Permiso p : entrada.getValue()) {
                    CheckBox check = new CheckBox(p.accion());
                    check.setSelected(seleccionados.contains(p.codigo()));
                    check.setUserData(p.codigo());
                    checks.add(check);
                    acciones.getChildren().add(check);
                }
                bloque.getChildren().addAll(titulo, acciones);
                getChildren().add(bloque);
            }
        }

        Set<String> seleccionados() {
            Set<String> codigos = new LinkedHashSet<>();
            for (CheckBox check : checks) {
                if (check.isSelected()) {
                    codigos.add((String) check.getUserData());
                }
            }
            return codigos;
        }
    }
}
