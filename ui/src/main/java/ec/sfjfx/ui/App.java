package ec.sfjfx.ui;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.vistas.LoginView;
import ec.sfjfx.ui.vistas.Shell;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la interfaz JavaFX.
 *
 * <p>Flujo: verifica la base de datos, muestra el login y, al autenticar,
 * monta el {@link Shell} con los modulos activos para el usuario.
 */
public class App extends Application {

    private static final String TITULO = "sf-jfx";
    private static final String CSS = "/css/estilos.css";

    private Stage escenario;

    @Override
    public void start(Stage escenario) {
        this.escenario = escenario;
        escenario.setTitle(TITULO);
        escenario.setMinWidth(1000);
        escenario.setMinHeight(650);

        verificarBaseDeDatos();
        mostrarLogin();
        escenario.show();
    }

    private void verificarBaseDeDatos() {
        try {
            AppConfig.cargar();
            if (!ConexionBD.probar()) {
                Alertas.error("Sin conexion",
                        "No se pudo conectar a la base de datos.\n"
                        + "Verifique la configuracion y que el contenedor este en marcha.");
            }
        } catch (RuntimeException e) {
            Alertas.error("Configuracion", e.getMessage());
        }
    }

    private void mostrarLogin() {
        LoginView login = new LoginView(this::alIngresar);
        aplicarEscena(login);
        escenario.setTitle(TITULO);
    }

    private void alIngresar(Sesion sesion) {
        SesionActual.iniciar(sesion);
        mostrarShell(sesion);
    }

    private void mostrarShell(Sesion sesion) {
        Shell shell = new Shell(sesion, this::mostrarLogin);
        aplicarEscena(shell);
        String empresa = sesion.empresa().nombreComercial() != null
                ? sesion.empresa().nombreComercial()
                : sesion.empresa().razonSocial();
        escenario.setTitle(TITULO + " — " + empresa);
    }

    private void aplicarEscena(Parent raiz) {
        Scene escena = new Scene(raiz, 1100, 680);
        escena.getStylesheets().add(App.class.getResource(CSS).toExternalForm());
        escenario.setScene(escena);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
