package ec.sfjfx.ui.vistas.admin;

import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.modelo.RegistroAuditoria;
import ec.sfjfx.core.servicio.ServicioAuditoria;
import ec.sfjfx.ui.componentes.VistaLista;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public final class VistaAuditoria extends VistaLista<RegistroAuditoria> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final ServicioAuditoria servicio = new ServicioAuditoria();

    public VistaAuditoria() {
        super("Auditoria (ultimos 200 movimientos)");
        columna("Fecha", 150, a -> a.creadoEn() == null ? "" : a.creadoEn().format(FORMATO));
        columna("Usuario", 130, a -> a.usuarioNombre() == null ? "sistema" : a.usuarioNombre());
        columna("Modulo", 140, RegistroAuditoria::modulo);
        columna("Entidad", 140, RegistroAuditoria::entidad);
        columna("Id", 70, RegistroAuditoria::entidadId);
        columna("Accion", 110, RegistroAuditoria::accion);
        buscarPor(a -> (a.usuarioNombre() == null ? "" : a.usuarioNombre()) + " "
                + a.modulo() + " " + a.entidad() + " " + a.accion());
        proveedor(() -> servicio.listar(null, null, 200));

        accion("Refrescar", false, v -> refrescar());
        onDobleClic(this::verDetalle);
        refrescar();
    }

    private void verDetalle(RegistroAuditoria a) {
        TextArea detalle = new TextArea("Anterior:\n" + valor(a.valoresAnteriores())
                + "\n\nNuevo:\n" + valor(a.valoresNuevos()));
        detalle.setEditable(false);
        detalle.setPrefRowCount(12);
        detalle.setPrefColumnCount(48);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Detalle de auditoria");
        dialogo.setHeaderText(a.modulo() + " / " + a.entidad() + " (" + a.accion() + ")");
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        VBox caja = new VBox(6, new Label("Valores registrados"), detalle);
        caja.setPadding(new Insets(12));
        dialogo.getDialogPane().setContent(caja);
        dialogo.showAndWait();
    }

    private String valor(String json) {
        return json == null || json.isBlank() ? "-" : json;
    }
}
