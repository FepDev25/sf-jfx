package ec.sfjfx.ui.vistas.admin;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import ec.sfjfx.core.modelo.Respaldo;
import ec.sfjfx.core.servicio.ServicioRespaldo;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class VistaRespaldos extends VistaLista<Respaldo> {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioRespaldo servicio = new ServicioRespaldo();

    public VistaRespaldos() {
        super("Respaldos de la base de datos");
        columna("Fecha", 150, r -> r.creadoEn() == null ? "" : r.creadoEn().format(FECHA));
        columna("Tipo", 110, Respaldo::tipo);
        columna("Estado", 90, Respaldo::estado);
        columna("Tamano", 100, Respaldo::tamanoLegible);
        columna("Archivo", 300, Respaldo::rutaArchivo);
        columna("Detalle", 300, Respaldo::detalle);
        buscarPor(r -> r.rutaArchivo() + " " + r.estado() + " " + r.tipo());
        proveedor(() -> servicio.historial(200));

        if (Perm.tiene("respaldo.crear")) {
            accion("Respaldar ahora", true, v -> respaldar());
            accion("Restaurar", false, v -> v.seleccion().ifPresent(this::restaurar));
        }
        if (Perm.tiene("respaldo.editar")) {
            accion("Programar", false, v -> programar());
        }
        if (Perm.tiene("respaldo.eliminar")) {
            accion("Eliminar", false, v -> v.seleccion().ifPresent(this::eliminar));
        }
        refrescar();
    }

    private void respaldar() {
        if (!Alertas.confirmar("Respaldo", "¿Generar un respaldo de la base de datos ahora?")) {
            return;
        }
        enSegundoPlano("respaldar", () -> {
            Respaldo r = servicio.respaldar(false);
            return "Respaldo generado:\n" + r.rutaArchivo() + "\n(" + r.tamanoLegible() + ")";
        });
    }

    private void programar() {
        CheckBox activo = Fx.check("Respaldos automaticos activos", servicio.programadoActivo());
        TextField hora = Fx.campo(servicio.horaProgramada());
        TextField directorio = Fx.campo(servicio.directorio().toString());
        new DialogoFormulario("Programacion de respaldos", ventana())
                .agregar("Activo", activo)
                .agregar("Hora diaria (HH:mm)", hora)
                .agregar("Directorio", directorio)
                .alGuardar(() -> {
                    servicio.guardarProgramacion(activo.isSelected(), Fx.texto(hora), Fx.texto(directorio));
                    Alertas.info("Programacion", "La programacion de respaldos fue actualizada.");
                })
                .mostrar();
    }

    private void restaurar(Respaldo r) {
        if (r.rutaArchivo() == null || r.rutaArchivo().isBlank()) {
            Alertas.advertencia("Restaurar", "El registro no tiene un archivo asociado.");
            return;
        }
        Path archivo = Path.of(r.rutaArchivo());
        if (!Alertas.confirmar("Restaurar respaldo",
                "Esta operacion reemplaza los objetos de la base de datos actual con los del archivo:\n"
                + archivo + "\n\n¿Desea continuar?")) {
            return;
        }
        enSegundoPlano("restaurar", () -> {
            servicio.restaurar(archivo);
            return "Restauracion completada. Reinicie la aplicacion para evitar conexiones con estado obsoleto.";
        });
    }

    private void eliminar(Respaldo r) {
        if (!Alertas.confirmar("Eliminar registro",
                "¿Eliminar el registro del respaldo? El archivo no se borra del disco.\n" + r.rutaArchivo())) {
            return;
        }
        try {
            servicio.eliminarRegistro(r.id());
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo eliminar", e.getMessage());
        }
    }

    private void enSegundoPlano(String nombre, Callable<String> operacion) {
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() throws Exception {
                return operacion.call();
            }
        };
        tarea.setOnSucceeded(e -> {
            refrescar();
            Alertas.info("Operacion completada", tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            refrescar();
            Throwable error = tarea.getException();
            Alertas.error("Operacion fallida", error == null ? "Error desconocido" : error.getMessage());
        });
        Thread hilo = new Thread(tarea, "respaldo-" + nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    private Window ventana() {
        return getScene() == null ? null : Platform.isFxApplicationThread()
                ? getScene().getWindow() : null;
    }
}
