package ec.sfjfx.ui.vistas.admin;

import ec.sfjfx.core.servicio.ServicioEstadoInstalacion;
import ec.sfjfx.core.servicio.ServicioSalud;
import ec.sfjfx.ui.util.Alertas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Estado de la instalacion: version, base, esquema, licencia y ultimo respaldo. */
public final class VistaEstadoInstalacion extends BorderPane {

    private final ServicioEstadoInstalacion servicio = new ServicioEstadoInstalacion();
    private final Label appVersion = valor();
    private final Label entorno = valor();
    private final Label dbVersion = valor();
    private final Label schemaVersion = valor();
    private final Label instalacion = valor();
    private final Label modo = valor();
    private final Label codigo = valor();
    private final Label licencia = valor();
    private final Label mantenimiento = valor();
    private final Label ultimoRespaldo = valor();
    private final Label salud = valor();
    private final Label error = new Label();

    public VistaEstadoInstalacion() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));

        Label titulo = new Label("Estado de la instalacion");
        titulo.getStyleClass().add("titulo-seccion");
        Label subtitulo = new Label("Version, base de datos, licencia y respaldo mas reciente.");
        subtitulo.getStyleClass().add("texto-suave");

        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("boton-primario");
        actualizar.setOnAction(e -> cargar());
        Button copiar = new Button("Copiar codigo");
        copiar.getStyleClass().add("boton-secundario");
        copiar.setOnAction(e -> copiarCodigo());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, espacio, copiar, actualizar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.getStyleClass().add("panel-tarjeta");
        int fila = 0;
        fila = agregar(grid, fila, "Version de la aplicacion", appVersion);
        fila = agregar(grid, fila, "Entorno", entorno);
        fila = agregar(grid, fila, "PostgreSQL", dbVersion);
        fila = agregar(grid, fila, "Version de esquema", schemaVersion);
        fila = agregar(grid, fila, "Instalacion", instalacion);
        fila = agregar(grid, fila, "Modo", modo);
        fila = agregar(grid, fila, "Codigo de instalacion", codigo);
        fila = agregar(grid, fila, "Licencia", licencia);
        fila = agregar(grid, fila, "Mantenimiento", mantenimiento);
        fila = agregar(grid, fila, "Ultimo respaldo", ultimoRespaldo);
        agregar(grid, fila, "Salud", salud);

        error.getStyleClass().add("wizard-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox contenido = new VBox(10, titulo, subtitulo, acciones, grid, error);
        setCenter(contenido);
        cargar();
    }

    private void cargar() {
        try {
            ServicioEstadoInstalacion.Estado estado = servicio.estado();
            appVersion.setText(estado.appVersion());
            entorno.setText(estado.entorno());
            dbVersion.setText(estado.dbVersion());
            schemaVersion.setText(estado.schemaVersion() == null ? "-" : estado.schemaVersion());
            instalacion.setText(estado.instalada()
                    ? (estado.instalacionNombre() == null ? "Instalada" : estado.instalacionNombre())
                    : "No instalada");
            modo.setText(estado.modo() == null ? "-" : estado.modo());
            codigo.setText(estado.codigoInstalacion() == null || estado.codigoInstalacion().isBlank()
                    ? "-" : estado.codigoInstalacion());
            licencia.setText(estado.licenciaMensaje());
            mantenimiento.setText(estado.mantenimientoVigente() ? "Vigente" : "Vencido o sin licencia");
            ultimoRespaldo.setText(estado.ultimoRespaldo()
                    + ("OK".equals(estado.ultimoRespaldoEstado()) ? "" : " (" + estado.ultimoRespaldoEstado() + ")"));

            ServicioSalud.Estado comprobacion = new ServicioSalud().comprobar();
            salud.setText((comprobacion.correcto() ? "OK · " : "CON PROBLEMAS · ")
                    + comprobacion.mensaje());

            error.setVisible(false);
            error.setManaged(false);
        } catch (RuntimeException e) {
            error.setText("No se pudo consultar el estado: " + e.getMessage());
            error.setVisible(true);
            error.setManaged(true);
        }
    }

    private void copiarCodigo() {
        if (codigo.getText() == null || codigo.getText().isBlank() || "-".equals(codigo.getText())) {
            Alertas.advertencia("Codigo de instalacion", "No hay un codigo disponible.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(codigo.getText());
        Clipboard.getSystemClipboard().setContent(content);
        Alertas.info("Codigo copiado", "El codigo de instalacion fue copiado al portapapeles.");
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
