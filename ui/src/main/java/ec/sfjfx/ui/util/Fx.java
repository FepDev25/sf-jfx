package ec.sfjfx.ui.util;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Helpers para construir formularios. */
public final class Fx {

    private Fx() {
    }

    public static String siNo(boolean valor) {
        return valor ? "Si" : "No";
    }

    public static TextField campo(String valor) {
        TextField campo = new TextField(valor == null ? "" : valor);
        campo.getStyleClass().add("campo");
        return campo;
    }

    public static PasswordField clave() {
        PasswordField campo = new PasswordField();
        campo.getStyleClass().add("campo");
        return campo;
    }

    public static CheckBox check(String texto, boolean valor) {
        CheckBox check = new CheckBox(texto);
        check.setSelected(valor);
        return check;
    }

    public static ComboBox<String> combo(String... opciones) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(opciones);
        combo.getStyleClass().add("selector");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    public static String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    public static String nuloSiVacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
