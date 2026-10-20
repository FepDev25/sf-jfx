package ec.sfjfx.ui;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioInstalacion;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.vistas.AsistenteInstalacion;
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
        iniciar();
        escenario.show();
    }
    private void iniciar() {
        boolean conectado;
        try {
            conectado = ConexionBD.probar();
        } catch (RuntimeException e) {
            conectado = false;
        }
        if (!conectado) {
            mostrarAsistente("No se pudo conectar a la base de datos.\n"
                    + "Configure la conexion en este asistente para continuar.");
            return;
        }
        try {
            ServicioInstalacion servicio = new ServicioInstalacion();
            servicio.prepararEsquema();
            if (servicio.requiereInstalacion()) {
                mostrarAsistente(null);
            } else {
                mostrarLogin(null, null);
            }
        } catch (RuntimeException e) {
            mostrarAsistente("No se pudo preparar la base de datos: " + e.getMessage());
        }
    }

    private void verificarBaseDeDatos() {
        try {
            AppConfig.cargar();
        } catch (RuntimeException e) {
            Alertas.error("Configuracion", e.getMessage());
        }
    }

    private void mostrarAsistente(String mensaje) {
        aplicarEscena(new AsistenteInstalacion(this::alInstalar, mensaje));
        escenario.setTitle(TITULO + " — Instalacion");
    }

    private void alInstalar(String usuario) {
        Alertas.info("Instalacion completada",
                "La instalacion se completo. Ingrese con el usuario " + usuario + ".");
        mostrarLogin(usuario, "Instalacion completada. Ingrese con su usuario.");
    }

    private void mostrarLogin() {
        mostrarLogin(null, null);
    }

    private void mostrarLogin(String usuarioInicial, String mensaje) {
        LoginView login = new LoginView(this::alIngresar, usuarioInicial, mensaje);
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
