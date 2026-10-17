package ec.sfjfx.ui.vistas;

import java.util.function.Consumer;

import ec.sfjfx.core.seguridad.AutenticacionExcepcion;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.Sesion;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Pantalla de inicio de sesion. */
public final class LoginView extends BorderPane {

    private final ServicioAutenticacion autenticacion = new ServicioAutenticacion();
    private final Consumer<Sesion> alIngresar;

    private final TextField campoUsuario = new TextField();
    private final PasswordField campoClave = new PasswordField();
    private final Label etiquetaError = new Label();
    private final Button botonIngresar = new Button("Ingresar");

    public LoginView(Consumer<Sesion> alIngresar) {
        this.alIngresar = alIngresar;
        getStyleClass().add("login-fondo");

        Label marca = new Label("sf-jfx");
        marca.getStyleClass().add("login-marca");
        Label subtitulo = new Label("Sistema de facturacion");
        subtitulo.getStyleClass().add("login-subtitulo");

        campoUsuario.setPromptText("Usuario");
        campoUsuario.getStyleClass().add("campo");
        campoClave.setPromptText("Contrasena");
        campoClave.getStyleClass().add("campo");

        etiquetaError.getStyleClass().add("login-error");
        etiquetaError.setWrapText(true);
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);

        botonIngresar.getStyleClass().add("boton-primario");
        botonIngresar.setMaxWidth(Double.MAX_VALUE);
        botonIngresar.setDefaultButton(true);
        botonIngresar.setOnAction(e -> intentar());

        VBox tarjeta = new VBox(10, marca, subtitulo, campoUsuario, campoClave, etiquetaError, botonIngresar);
        tarjeta.getStyleClass().add("login-tarjeta");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setMaxWidth(340);
        tarjeta.setPadding(new Insets(28));

        StackPane centro = new StackPane(tarjeta);
        centro.setPadding(new Insets(40));
        setCenter(centro);

        javafx.application.Platform.runLater(campoUsuario::requestFocus);
    }

    private void intentar() {
        ocultarError();
        botonIngresar.setDisable(true);
        try {
            Sesion sesion = autenticacion.iniciar(campoUsuario.getText(), campoClave.getText());
            alIngresar.accept(sesion);
        } catch (AutenticacionExcepcion e) {
            mostrarError(e.getMessage());
        } catch (RuntimeException e) {
            mostrarError("No se pudo conectar con la base de datos.");
        } finally {
            botonIngresar.setDisable(false);
        }
    }

    private void mostrarError(String mensaje) {
        etiquetaError.setText(mensaje);
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    private void ocultarError() {
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
    }
}
