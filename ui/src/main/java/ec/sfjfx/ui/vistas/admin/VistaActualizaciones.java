package ec.sfjfx.ui.vistas.admin;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.config.AppInfo;
import ec.sfjfx.core.servicio.ServicioActualizacion;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Actualizacion remota asistida: consulta, descarga y verificacion del paquete. */
public final class VistaActualizaciones extends BorderPane {

    private final ServicioActualizacion servicio = new ServicioActualizacion();
    private final boolean puedeEditar = Perm.tiene("configuracion.editar");

    private final Label versionActual = valor();
    private final Label manifiesto = valor();
    private final Label estado = valor();
    private final Label notas = valor();
    private final Label paquete = valor();
    private final Label error = new Label();
    private final Button buscar = new Button("Buscar actualizaciones");
    private final Button descargar = new Button("Descargar y verificar");
    private ServicioActualizacion.Estado ultimoEstado;

    public VistaActualizaciones() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));

        Label titulo = new Label("Actualizaciones");
        titulo.getStyleClass().add("titulo-seccion");
        Label subtitulo = new Label("Canal remoto asistido: el paquete se descarga y verifica; "
                + "el tecnico aprueba y ejecuta el instalador/actualizador.");
        subtitulo.getStyleClass().add("texto-suave");
        subtitulo.setWrapText(true);

        Button buscar = new Button("Buscar actualizaciones");
        buscar.getStyleClass().add("boton-primario");
        buscar.setOnAction(e -> buscar());
        descargar.getStyleClass().add("boton-secundario");
        descargar.setDisable(true);
        descargar.setOnAction(e -> descargar());
        if (!puedeEditar) {
            buscar.setDisable(true);
            descargar.setDisable(true);
        }

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, espacio, buscar, descargar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.getStyleClass().add("panel-tarjeta");
        int fila = 0;
        fila = agregar(grid, fila, "Version actual", versionActual);
        fila = agregar(grid, fila, "Canal", manifiesto);
        fila = agregar(grid, fila, "Estado", estado);
        fila = agregar(grid, fila, "Notas", notas);
        agregar(grid, fila, "Paquete descargado", paquete);

        error.getStyleClass().add("wizard-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox contenido = new VBox(10, titulo, subtitulo, acciones, grid, error);
        setCenter(contenido);
        cargarInicial();
    }

    private void cargarInicial() {
        versionActual.setText(AppInfo.version());
        manifiesto.setText(AppConfig.obtener(ServicioActualizacion.CLAVE_MANIFIESTO,
                "(no configurado)"));
        estado.setText("Pulse 'Buscar actualizaciones' para consultar el canal.");
        notas.setText("-");
        paquete.setText("-");
    }

    private void buscar() {
        bloquear(true);
        javafx.concurrent.Task<ServicioActualizacion.Estado> tarea = new javafx.concurrent.Task<>() {
            @Override
            protected ServicioActualizacion.Estado call() {
                return servicio.consultar();
            }
        };
        tarea.setOnSucceeded(e -> {
            ultimoEstado = tarea.getValue();
            versionActual.setText(ultimoEstado.versionActual());
            manifiesto.setText(AppConfig.obtener(ServicioActualizacion.CLAVE_MANIFIESTO,
                    "(no configurado)"));
            estado.setText(ultimoEstado.mensaje());
            notas.setText(ultimoEstado.disponible() == null ? "-" : ultimoEstado.disponible().notas());
            descargar.setDisable(!ultimoEstado.hayActualizacion() || !puedeEditar);
            error.setVisible(false);
            error.setManaged(false);
            bloquear(false);
        });
        tarea.setOnFailed(e -> {
            mostrarError(tarea.getException() == null ? null : tarea.getException().getMessage());
            bloquear(false);
        });
        new Thread(tarea, "actualizacion-consulta").start();
    }

    private void descargar() {
        if (ultimoEstado == null || ultimoEstado.disponible() == null) {
            return;
        }
        bloquear(true);
        javafx.concurrent.Task<java.nio.file.Path> tarea = new javafx.concurrent.Task<>() {
            @Override
            protected java.nio.file.Path call() {
                return servicio.descargar(ultimoEstado.disponible());
            }
        };
        tarea.setOnSucceeded(e -> {
            var archivo = tarea.getValue();
            paquete.setText(archivo.toAbsolutePath().toString());
            bloquear(false);
            Alertas.info("Paquete verificado",
                    "El paquete se descargo y su SHA-256 es correcto:\n" + archivo
                    + "\n\nCierre la aplicacion y ejecute el instalador/actualizador. "
                    + "El procedimiento debe respaldar la base antes de aplicar migraciones.");
        });
        tarea.setOnFailed(e -> {
            mostrarError(tarea.getException() == null ? null : tarea.getException().getMessage());
            bloquear(false);
        });
        new Thread(tarea, "actualizacion-descarga").start();
    }

    private void bloquear(boolean bloquear) {
        // Los botones se deshabilitan mientras la tarea de red esta en curso.
        buscar.setDisable(bloquear || !puedeEditar);
        descargar.setDisable(bloquear || !puedeEditar
                || ultimoEstado == null || !ultimoEstado.hayActualizacion());
    }

    private void mostrarError(String mensaje) {
        error.setText(mensaje == null ? "No se pudo completar la operacion" : mensaje);
        error.setVisible(true);
        error.setManaged(true);
    }

    private static Label valor() {
        Label label = new Label("-");
        label.getStyleClass().add("panel-titulo");
        label.setWrapText(true);
        return label;
    }

    private static int agregar(GridPane grid, int fila, String titulo, Label valor) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("etiqueta-form");
        grid.add(etiqueta, 0, fila);
        grid.add(valor, 1, fila);
        GridPane.setHgrow(valor, Priority.ALWAYS);
        return fila + 1;
    }
}
