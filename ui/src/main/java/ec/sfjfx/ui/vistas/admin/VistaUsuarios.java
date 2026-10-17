package ec.sfjfx.ui.vistas.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.modelo.Usuario;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioUsuario;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class VistaUsuarios extends VistaLista<Usuario> {

    private final ServicioUsuario servicio = new ServicioUsuario();

    public VistaUsuarios() {
        super("Usuarios");
        columna("Usuario", 140, Usuario::nombreUsuario);
        columna("Nombre", 220, Usuario::nombreCompleto);
        columna("Email", 220, Usuario::email);
        columna("Superadmin", 90, u -> Fx.siNo(u.esSuperadmin()));
        columna("Activo", 70, u -> Fx.siNo(u.activo()));
        buscarPor(u -> u.nombreUsuario() + " " + u.nombreCompleto() + " " + (u.email() == null ? "" : u.email()));
        proveedor(servicio::listar);

        if (Perm.tiene("usuarios.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("usuarios.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
            accion("Roles", false, v -> v.seleccion().ifPresent(this::asignarRoles));
            accion("Sucursales", false, v -> v.seleccion().ifPresent(this::asignarSucursales));
            accion("Contrasena", false, v -> v.seleccion().ifPresent(this::cambiarPassword));
        }
        onDobleClic(u -> {
            if (Perm.tiene("usuarios.editar")) {
                abrirFormulario(u);
            }
        });
        refrescar();
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    private void abrirFormulario(Usuario original) {
        boolean nuevo = original == null;
        Usuario base = nuevo
                ? new Usuario(null, empresaActual(), "", "", "", null, null, false, true)
                : original;

        TextField usuario = Fx.campo(base.nombreUsuario());
        TextField nombres = Fx.campo(base.nombres());
        TextField apellidos = Fx.campo(base.apellidos());
        TextField email = Fx.campo(base.email());
        TextField telefono = Fx.campo(base.telefono());
        CheckBox superadmin = Fx.check("Superadministrador", base.esSuperadmin());
        CheckBox activo = Fx.check("Activo", base.activo());
        PasswordField password = Fx.clave();

        DialogoFormulario dialogo = new DialogoFormulario(
                nuevo ? "Nuevo usuario" : "Editar usuario", ventana());
        dialogo.agregar("Usuario", usuario)
                .agregar("Nombres", nombres)
                .agregar("Apellidos", apellidos)
                .agregar("Email", email)
                .agregar("Telefono", telefono);
        if (nuevo) {
            dialogo.agregar("Contrasena", password);
        }
        dialogo.agregarAnchoCompleto(superadmin)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    Usuario modelo = new Usuario(base.id(), base.empresaId(), Fx.texto(usuario),
                            Fx.texto(nombres), Fx.nuloSiVacio(Fx.texto(apellidos)), Fx.nuloSiVacio(Fx.texto(email)),
                            Fx.nuloSiVacio(Fx.texto(telefono)), superadmin.isSelected(), activo.isSelected());
                    if (nuevo) {
                        servicio.crear(modelo, Fx.texto(password));
                    } else {
                        servicio.actualizar(modelo);
                    }
                    refrescar();
                })
                .mostrar();
    }

    private void asignarRoles(Usuario u) {
        List<Rol> roles = servicio.rolesDisponibles();
        Set<Long> actuales = new HashSet<>();
        servicio.rolesDe(u.id()).forEach(r -> actuales.add(r.id()));

        List<CheckBox> checks = new ArrayList<>();
        VBox caja = new VBox(8);
        caja.setPadding(new Insets(12));
        for (Rol rol : roles) {
            CheckBox check = new CheckBox(rol.nombre());
            check.setSelected(actuales.contains(rol.id()));
            check.setUserData(rol.id());
            checks.add(check);
            caja.getChildren().add(check);
        }
        if (mostrarDialogo("Roles de " + u.nombreUsuario(), caja)) {
            Set<Long> seleccion = new HashSet<>();
            for (CheckBox check : checks) {
                if (check.isSelected()) {
                    seleccion.add((Long) check.getUserData());
                }
            }
            servicio.asignarRoles(u.id(), seleccion);
            Alertas.info("Roles", "Roles actualizados para " + u.nombreUsuario());
        }
    }

    private void asignarSucursales(Usuario u) {
        List<Sucursal> sucursales = new SucursalRepositorio().porEmpresa(empresaActual());
        Set<Long> actuales = servicio.sucursalesDe(u.id());

        List<CheckBox> checks = new ArrayList<>();
        VBox caja = new VBox(8);
        caja.setPadding(new Insets(12));
        for (Sucursal s : sucursales) {
            CheckBox check = new CheckBox(s.nombre());
            check.setSelected(actuales.contains(s.id()));
            check.setUserData(s.id());
            checks.add(check);
            caja.getChildren().add(check);
        }
        if (mostrarDialogo("Sucursales de " + u.nombreUsuario(), caja)) {
            Set<Long> seleccion = new HashSet<>();
            for (CheckBox check : checks) {
                if (check.isSelected()) {
                    seleccion.add((Long) check.getUserData());
                }
            }
            servicio.asignarSucursales(u.id(), seleccion);
            Alertas.info("Sucursales", "Sucursales actualizadas para " + u.nombreUsuario());
        }
    }

    private void cambiarPassword(Usuario u) {
        PasswordField password = Fx.clave();
        PasswordField confirmar = Fx.clave();
        VBox caja = new VBox(8);
        caja.setPadding(new Insets(12));
        caja.getChildren().addAll(new javafx.scene.control.Label("Nueva contrasena"), password,
                new javafx.scene.control.Label("Confirmar"), confirmar);
        if (mostrarDialogo("Cambiar contrasena de " + u.nombreUsuario(), caja)) {
            if (!Fx.texto(password).equals(Fx.texto(confirmar))) {
                Alertas.error("Contrasena", "Las contrasenas no coinciden");
                return;
            }
            servicio.cambiarPassword(u.id(), Fx.texto(password));
            Alertas.info("Contrasena", "Contrasena actualizada");
        }
    }

    private boolean mostrarDialogo(String titulo, VBox contenido) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        if (ventana() != null) {
            dialogo.initOwner(ventana());
        }
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(contenido);
        return dialogo.showAndWait().filter(b -> b == guardar).isPresent();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
