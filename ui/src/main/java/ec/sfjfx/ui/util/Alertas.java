package ec.sfjfx.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/** Utilidades de dialogo. */
public final class Alertas {

    private Alertas() {
    }

    public static void error(String titulo, String mensaje) {
        mostrar(AlertType.ERROR, titulo, mensaje);
    }

    public static void info(String titulo, String mensaje) {
        mostrar(AlertType.INFORMATION, titulo, mensaje);
    }

    public static void advertencia(String titulo, String mensaje) {
        mostrar(AlertType.WARNING, titulo, mensaje);
    }

    private static void mostrar(AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
