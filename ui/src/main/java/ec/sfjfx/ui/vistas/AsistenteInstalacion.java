package ec.sfjfx.ui.vistas;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.servicio.ServicioInstalacion;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;
import ec.sfjfx.ui.util.Fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Asistente de primer arranque: conexion, empresa, organizacion y administrador.
 *
 * <p>No crea datos de demostracion. Al finalizar delega en {@link ServicioInstalacion}, que
 * ejecuta las migraciones y crea todo en una sola transaccion.
 */
public final class AsistenteInstalacion extends BorderPane {

    private static final String[] PASOS = {"Conexion", "Empresa", "Organizacion", "Administrador"};
    private static final String[] TITULOS = {
        "Conexion a la base de datos",
        "Datos de la empresa",
        "Sucursal, bodega y caja",
        "Administrador inicial"
    };
    private static final String[] DESCRIPCIONES = {
        "Configure PostgreSQL y verifique la conexion. Las migraciones se aplican al continuar.",
        "Estos datos identifican al emisor y definen el perfil de modulos de la instalacion.",
        "Se creara la estructura minima para operar. No se cargan datos de demostracion.",
        "El administrador se crea con la contrasena que usted defina. No hay credenciales por defecto."
    };

    private final Consumer<String> alCompletar;
    private final String mensajeInicial;
    private final List<Label> indicadores = new ArrayList<>();

    private final Label titulo = new Label();
    private final Label descripcion = new Label();
    private final Label etiquetaError = new Label();
    private final Label etiquetaInfo = new Label();
    private final VBox contenido = new VBox(12);
    private final Button anterior = new Button("Anterior");
    private final Button siguiente = new Button("Siguiente");
    private final Button instalar = new Button("Instalar ahora");
    private int paso;

    // Conexion
    private final TextField campoUrl = Fx.campo(config("db.url", "jdbc:postgresql://localhost:5432/sfjfx"));
    private final TextField campoUsuarioDb = Fx.campo(config("db.usuario", "sfjfx"));
    private final PasswordField campoClaveDb = Fx.clave();
    private final TextField campoEsquema = Fx.campo(config("db.esquema", "public"));
    private final ComboBox<String> comboEntorno = Fx.combo("Producción", "Desarrollo local (pruebas)");

    // Empresa
    private final TextField campoRuc = Fx.campo("");
    private final TextField campoRazonSocial = Fx.campo("");
    private final TextField campoNombreComercial = Fx.campo("");
    private final ComboBox<String> comboRegimen = Fx.combo(
            "GENERAL", "RIMPE_EMPRENDEDOR", "RIMPE_NEGOCIO_POPULAR");
    private final ComboBox<String> comboPerfil = Fx.combo("Supermercado", "Minimercado", "Tienda");
    private final ComboBox<String> comboSri = Fx.combo("Pruebas", "Producción");
    private final TextField campoDireccion = Fx.campo("");
    private final TextField campoTelefono = Fx.campo("");
    private final TextField campoEmail = Fx.campo("");
    private final CheckBox checkContribuyente = Fx.check("Contribuyente especial", false);
    private final CheckBox checkObligado = Fx.check("Obligado a llevar contabilidad", true);
    private final CheckBox checkMultiempresa = Fx.check("Permitir varias empresas (avanzado)", false);

    // Organizacion
    private final TextField campoSucursalCodigo = Fx.campo("001");
    private final TextField campoSucursalNombre = Fx.campo("Matriz");
    private final TextField campoSucursalDireccion = Fx.campo("");
    private final TextField campoSucursalTelefono = Fx.campo("");
    private final TextField campoBodegaCodigo = Fx.campo("ALM");
    private final TextField campoBodegaNombre = Fx.campo("Almacen principal");
    private final TextField campoCajaCodigo = Fx.campo("CAJA01");
    private final TextField campoCajaNombre = Fx.campo("Caja 1");

    // Administrador
    private final TextField campoAdminUsuario = Fx.campo("");
    private final TextField campoAdminNombres = Fx.campo("");
    private final TextField campoAdminApellidos = Fx.campo("");
    private final TextField campoAdminEmail = Fx.campo("");
    private final TextField campoAdminTelefono = Fx.campo("");
    private final PasswordField campoAdminPassword = Fx.clave();
    private final PasswordField campoAdminPassword2 = Fx.clave();
    private final PasswordField campoAdminPin = Fx.clave();

    public AsistenteInstalacion(Consumer<String> alCompletar, String mensajeInicial) {
        this.alCompletar = alCompletar;
        this.mensajeInicial = mensajeInicial;
        inicializarValores();
        construir();
        actualizarPaso();
        if (mensajeInicial != null && !mensajeInicial.isBlank()) {
            mostrarError(mensajeInicial);
        }
    }

    private void inicializarValores() {
        campoClaveDb.setText(config("db.password", ""));
        comboEntorno.setValue("dev".equals(config("app.entorno", "prod"))
                ? "Desarrollo local (pruebas)" : "Producción");
        comboRegimen.setValue("GENERAL");
        comboPerfil.setValue("Supermercado");
        comboSri.setValue("Pruebas");
        campoAdminUsuario.setPromptText("admin");
        campoAdminNombres.setPromptText("Nombres del administrador");
        campoAdminApellidos.setPromptText("Apellidos");
        campoAdminEmail.setPromptText("admin@empresa.com");
        campoAdminTelefono.setPromptText("Telefono");
        campoAdminPassword.setPromptText("Minimo 8 caracteres");
        campoAdminPassword2.setPromptText("Repita la contrasena");
        campoAdminPin.setPromptText("4 a 8 digitos");
        campoRuc.setPromptText("1790011678001");
        campoRazonSocial.setPromptText("Razon social de la empresa");
        campoNombreComercial.setPromptText("Nombre comercial (opcional)");
        campoEmail.setPromptText("contacto@empresa.com");
        campoDireccion.setPromptText("Direccion de la matriz");
        campoTelefono.setPromptText("Telefono");
    }

    private void construir() {
        getStyleClass().add("wizard-fondo");

        Label marca = new Label("sf-jfx");
        marca.getStyleClass().add("login-marca");
        Label subtitulo = new Label("Instalacion inicial");
        subtitulo.getStyleClass().add("login-subtitulo");

        VBox rail = new VBox(6, marca, subtitulo);
        rail.getStyleClass().add("wizard-rail");
        for (String nombre : PASOS) {
            Label indicador = new Label(nombre);
            indicador.getStyleClass().add("wizard-paso");
            indicadores.add(indicador);
            rail.getChildren().add(indicador);
        }
        Separator separador = new Separator();
        Label nota = new Label("No se cargan datos de demostracion.\n"
                + "Las semillas quedan fuera del producto instalable.");
        nota.getStyleClass().add("texto-suave");
        nota.setWrapText(true);
        rail.getChildren().addAll(separador, nota);

        titulo.getStyleClass().add("wizard-titulo");
        descripcion.getStyleClass().add("wizard-descripcion");
        descripcion.setWrapText(true);

        etiquetaInfo.getStyleClass().add("wizard-info");
        etiquetaInfo.setWrapText(true);
        etiquetaInfo.setVisible(false);
        etiquetaInfo.setManaged(false);

        etiquetaError.getStyleClass().add("wizard-error");
        etiquetaError.setWrapText(true);
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("wizard-scroll");
        scroll.setMaxHeight(410);

        anterior.getStyleClass().add("boton-secundario");
        anterior.setAccessibleText("Paso anterior");
        anterior.setOnAction(e -> {
            if (paso > 0) {
                paso--;
                actualizarPaso();
            }
        });

        siguiente.getStyleClass().add("boton-primario");
        siguiente.setDefaultButton(true);
        siguiente.setOnAction(e -> siguiente());

        instalar.getStyleClass().add("boton-primario");
        instalar.setOnAction(e -> instalar());
        instalar.setVisible(false);
        instalar.setManaged(false);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox botones = new HBox(8, anterior, espacio, siguiente, instalar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox cuerpo = new VBox(12, titulo, descripcion, etiquetaInfo, etiquetaError, scroll, botones);
        cuerpo.setPadding(new Insets(26));
        cuerpo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cuerpo, Priority.ALWAYS);

        HBox tarjeta = new HBox(24, rail, cuerpo);
        tarjeta.getStyleClass().add("wizard-tarjeta");
        tarjeta.setMaxWidth(920);
        tarjeta.setAlignment(Pos.TOP_LEFT);

        StackPane centro = new StackPane(tarjeta);
        centro.setPadding(new Insets(32));
        setCenter(centro);
    }

    private void actualizarPaso() {
        titulo.setText(TITULOS[paso]);
        descripcion.setText(DESCRIPCIONES[paso]);
        for (int i = 0; i < indicadores.size(); i++) {
            Label indicador = indicadores.get(i);
            indicador.getStyleClass().removeAll("activo", "hecho");
            if (i == paso) {
                indicador.getStyleClass().add("activo");
            } else if (i < paso) {
                indicador.getStyleClass().add("hecho");
            }
        }
        contenido.getChildren().setAll(switch (paso) {
            case 0 -> pasoConexion();
            case 1 -> pasoEmpresa();
            case 2 -> pasoOrganizacion();
            default -> pasoAdministrador();
        });
        anterior.setDisable(paso == 0);
        siguiente.setVisible(paso < PASOS.length - 1);
        siguiente.setManaged(paso < PASOS.length - 1);
        instalar.setVisible(paso == PASOS.length - 1);
        instalar.setManaged(paso == PASOS.length - 1);
        ocultarMensajes();
        javafx.application.Platform.runLater(() -> {
            if (paso == 0) {
                campoUrl.requestFocus();
            }
        });
    }

    private void siguiente() {
        boolean valido = switch (paso) {
            case 0 -> validarConexion();
            case 1 -> validarEmpresa();
            case 2 -> validarOrganizacion();
            default -> false;
        };
        if (valido && paso < PASOS.length - 1) {
            paso++;
            actualizarPaso();
        }
    }

    private void instalar() {
        if (!validarAdministrador()) {
            return;
        }
        bloquearBotones(true);
        try {
            ServicioInstalacion.ResultadoInstalacion resultado =
                    new ServicioInstalacion().instalar(datos());
            alCompletar.accept(resultado.nombreUsuario());
        } catch (RuntimeException e) {
            mostrarError(mensaje(e));
        } finally {
            bloquearBotones(false);
        }
    }

    // ------------------------------------------------------------------ pasos

    private Node pasoConexion() {
        GridPane grid = grilla();
        grid.add(campo("URL JDBC", campoUrl), 0, 0, 2, 1);
        grid.add(campo("Usuario de base de datos", campoUsuarioDb), 0, 1);
        grid.add(campo("Contraseña", campoClaveDb), 1, 1);
        grid.add(campo("Esquema", campoEsquema), 0, 2);
        grid.add(campo("Entorno", comboEntorno), 1, 2);

        Label aviso = new Label("Produccion bloquea las semillas. Use Desarrollo local solo en "
                + "una PC de pruebas, nunca en un cliente real.");
        aviso.getStyleClass().add("texto-suave");
        aviso.setWrapText(true);

        Button probar = new Button("Probar conexion");
        probar.getStyleClass().add("boton-secundario");
        probar.setOnAction(e -> probarConexion());

        return new VBox(12, grid, aviso, probar);
    }

    private Node pasoEmpresa() {
        GridPane grid = grilla();
        grid.add(campo("RUC", campoRuc), 0, 0);
        grid.add(campo("Razon social", campoRazonSocial), 1, 0);
        grid.add(campo("Nombre comercial", campoNombreComercial), 0, 1);
        grid.add(campo("Regimen", comboRegimen), 1, 1);
        grid.add(campo("Perfil de instalacion", comboPerfil), 0, 2);
        grid.add(campo("Ambiente SRI", comboSri), 1, 2);
        grid.add(campo("Direccion matriz", campoDireccion), 0, 3, 2, 1);
        grid.add(campo("Telefono", campoTelefono), 0, 4);
        grid.add(campo("Email", campoEmail), 1, 4);

        VBox checks = new VBox(6, checkContribuyente, checkObligado, checkMultiempresa);
        Label aviso = new Label("El modo multiempresa es avanzado y solo aplica a cadenas o "
                + "holdings. El valor por defecto es una sola empresa.");
        aviso.getStyleClass().add("texto-suave");
        aviso.setWrapText(true);
        return new VBox(12, grid, checks, aviso);
    }

    private Node pasoOrganizacion() {
        GridPane grid = grilla();
        grid.add(campo("Codigo de sucursal", campoSucursalCodigo), 0, 0);
        grid.add(campo("Nombre de sucursal", campoSucursalNombre), 1, 0);
        grid.add(campo("Direccion de sucursal", campoSucursalDireccion), 0, 1, 2, 1);
        grid.add(campo("Telefono de sucursal", campoSucursalTelefono), 0, 2);
        grid.add(campo("Codigo de bodega", campoBodegaCodigo), 1, 2);
        grid.add(campo("Nombre de bodega", campoBodegaNombre), 0, 3);
        grid.add(campo("Codigo de caja", campoCajaCodigo), 1, 3);
        grid.add(campo("Nombre de caja", campoCajaNombre), 0, 4);

        Label nota = new Label("Tambien se creara la bodega de venta PISO para el POS. "
                + "La caja queda disponible para abrir turno.");
        nota.getStyleClass().add("texto-suave");
        nota.setWrapText(true);
        return new VBox(12, grid, nota);
    }

    private Node pasoAdministrador() {
        GridPane grid = grilla();
        grid.add(campo("Usuario", campoAdminUsuario), 0, 0);
        grid.add(campo("Email", campoAdminEmail), 1, 0);
        grid.add(campo("Nombres", campoAdminNombres), 0, 1);
        grid.add(campo("Apellidos", campoAdminApellidos), 1, 1);
        grid.add(campo("Telefono", campoAdminTelefono), 0, 2);
        grid.add(campo("PIN (4 a 8 digitos)", campoAdminPin), 1, 2);
        grid.add(campo("Contraseña (minimo 8 caracteres)", campoAdminPassword), 0, 3);
        grid.add(campo("Confirmar contraseña", campoAdminPassword2), 1, 3);

        Label resumen = new Label(
                "Se creara la instalacion para " + valor(campoRazonSocial) + ".\n"
                + "Sucursal: " + valor(campoSucursalNombre)
                + " · Caja: " + valor(campoCajaNombre) + "\n"
                + "Usuario administrador: "
                + (valor(campoAdminUsuario).isBlank() ? "(complete el usuario)" : valor(campoAdminUsuario))
                + "\n\nNo se cargaran datos de demostracion ni semillas.");
        resumen.getStyleClass().add("wizard-resumen");
        resumen.setWrapText(true);
        return new VBox(12, grid, resumen);
    }

    // ------------------------------------------------------------------ validaciones

    private boolean probarConexion() {
        ocultarMensajes();
        try {
            probarConexionActual();
            mostrarInfo("Conexion verificada.");
            return true;
        } catch (ValidacionExcepcion e) {
            mostrarError(e.getMessage());
            return false;
        } catch (SQLException e) {
            mostrarError("No se pudo conectar: " + e.getMessage());
            return false;
        }
    }

    private void probarConexionActual() throws SQLException {
        Validaciones.requerido(campoUrl.getText(), "URL JDBC");
        Validaciones.requerido(campoUsuarioDb.getText(), "Usuario de base de datos");
        String esquema = valor(campoEsquema);
        ConexionBD.probarConfiguracion(
                valor(campoUrl),
                valor(campoUsuarioDb),
                campoClaveDb.getText() == null ? "" : campoClaveDb.getText(),
                esquema.isBlank() ? "public" : esquema);
    }

    private boolean validarConexion() {
        ocultarMensajes();
        try {
            probarConexionActual();
        } catch (ValidacionExcepcion e) {
            mostrarError(e.getMessage());
            return false;
        } catch (SQLException e) {
            mostrarError("No se pudo conectar: " + e.getMessage());
            return false;
        }

        String entorno = comboEntorno.getValue() != null
                && comboEntorno.getValue().startsWith("Desarrollo") ? "dev" : "prod";
        try {
            AppConfig.guardar(Map.of(
                    "db.url", valor(campoUrl),
                    "db.usuario", valor(campoUsuarioDb),
                    "db.password", campoClaveDb.getText() == null ? "" : campoClaveDb.getText(),
                    "db.esquema", valor(campoEsquema).isBlank() ? "public" : valor(campoEsquema),
                    "app.entorno", entorno));
        } catch (RuntimeException e) {
            mostrarError("No se pudo guardar la configuracion: " + mensaje(e));
            return false;
        }
        ConexionBD.reiniciar();

        try {
            ServicioInstalacion servicio = new ServicioInstalacion();
            servicio.prepararEsquema();
            if (servicio.estado().instalada()) {
                mostrarError("Ya existe una instalacion en esa base de datos. "
                        + "Reinicie la aplicacion para iniciar sesion.");
                return false;
            }
            mostrarInfo("Conexion verificada y esquema actualizado. Continue con la empresa.");
            return true;
        } catch (RuntimeException e) {
            mostrarError("No se pudo preparar el esquema: " + mensaje(e));
            return false;
        }
    }

    private boolean validarEmpresa() {
        ocultarMensajes();
        try {
            Validaciones.requerido(campoRuc.getText(), "RUC");
            if (!Validaciones.identificacionEcuador(valor(campoRuc))) {
                throw new ValidacionExcepcion("El RUC no es valido");
            }
            Validaciones.requerido(campoRazonSocial.getText(), "Razon social");
            if (!Validaciones.email(valor(campoEmail))) {
                throw new ValidacionExcepcion("El email no es valido");
            }
            return true;
        } catch (ValidacionExcepcion e) {
            mostrarError(e.getMessage());
            return false;
        }
    }

    private boolean validarOrganizacion() {
        ocultarMensajes();
        try {
            Validaciones.requerido(campoSucursalCodigo.getText(), "Codigo de sucursal");
            Validaciones.requerido(campoSucursalNombre.getText(), "Nombre de sucursal");
            Validaciones.requerido(campoBodegaCodigo.getText(), "Codigo de bodega");
            Validaciones.requerido(campoBodegaNombre.getText(), "Nombre de bodega");
            Validaciones.requerido(campoCajaCodigo.getText(), "Codigo de caja");
            Validaciones.requerido(campoCajaNombre.getText(), "Nombre de caja");
            return true;
        } catch (ValidacionExcepcion e) {
            mostrarError(e.getMessage());
            return false;
        }
    }

    private boolean validarAdministrador() {
        ocultarMensajes();
        try {
            Validaciones.requerido(campoAdminUsuario.getText(), "Usuario");
            Validaciones.requerido(campoAdminNombres.getText(), "Nombres");
            if (!Validaciones.email(valor(campoAdminEmail))) {
                throw new ValidacionExcepcion("El email del administrador no es valido");
            }
            String password = campoAdminPassword.getText() == null ? "" : campoAdminPassword.getText();
            String confirmacion = campoAdminPassword2.getText() == null ? "" : campoAdminPassword2.getText();
            if (password.length() < 8) {
                throw new ValidacionExcepcion("La contrasena debe tener al menos 8 caracteres");
            }
            if (!password.equals(confirmacion)) {
                throw new ValidacionExcepcion("Las contrasenas no coinciden");
            }
            String pin = campoAdminPin.getText() == null ? "" : campoAdminPin.getText();
            if (!pin.matches("\\d{4,8}")) {
                throw new ValidacionExcepcion("El PIN debe tener entre 4 y 8 digitos");
            }
            return true;
        } catch (ValidacionExcepcion e) {
            mostrarError(e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ datos

    private ServicioInstalacion.DatosInstalacion datos() {
        String ambiente = comboSri.getValue() != null && comboSri.getValue().startsWith("Producción")
                ? "PRODUCCION" : "PRUEBAS";
        String nombreInstalacion = valor(campoNombreComercial).isBlank()
                ? valor(campoRazonSocial) : valor(campoNombreComercial);
        String modo = checkMultiempresa.isSelected()
                ? Instalacion.MODO_MULTIEMPRESA : Instalacion.MODO_UNICA_EMPRESA;
        return new ServicioInstalacion.DatosInstalacion(
                nombreInstalacion,
                modo,
                new ServicioInstalacion.DatosEmpresa(
                        valor(campoRuc),
                        valor(campoRazonSocial),
                        Fx.nuloSiVacio(valor(campoNombreComercial)),
                        valor(comboRegimen),
                        valor(comboPerfil),
                        Fx.nuloSiVacio(valor(campoDireccion)),
                        Fx.nuloSiVacio(valor(campoTelefono)),
                        Fx.nuloSiVacio(valor(campoEmail)),
                        checkContribuyente.isSelected(),
                        checkObligado.isSelected()),
                new ServicioInstalacion.DatosOrganizacion(
                        valor(campoSucursalCodigo),
                        valor(campoSucursalNombre),
                        Fx.nuloSiVacio(valor(campoSucursalDireccion)),
                        Fx.nuloSiVacio(valor(campoSucursalTelefono)),
                        valor(campoBodegaCodigo),
                        valor(campoBodegaNombre),
                        valor(campoCajaCodigo),
                        valor(campoCajaNombre)),
                new ServicioInstalacion.DatosSri(ambiente),
                new ServicioInstalacion.DatosAdmin(
                        valor(campoAdminUsuario),
                        valor(campoAdminNombres),
                        Fx.nuloSiVacio(valor(campoAdminApellidos)),
                        Fx.nuloSiVacio(valor(campoAdminEmail)),
                        Fx.nuloSiVacio(valor(campoAdminTelefono)),
                        campoAdminPassword.getText() == null ? "" : campoAdminPassword.getText(),
                        campoAdminPin.getText() == null ? "" : campoAdminPin.getText()));
    }

    // ------------------------------------------------------------------ helpers

    private static GridPane grilla() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        ColumnConstraints columna = new ColumnConstraints();
        columna.setHgrow(Priority.ALWAYS);
        columna.setPercentWidth(50);
        grid.getColumnConstraints().addAll(columna, new ColumnConstraints());
        grid.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().get(1).setPercentWidth(50);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private static VBox campo(String texto, Region input) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("etiqueta-form");
        input.setMaxWidth(Double.MAX_VALUE);
        VBox caja = new VBox(4, etiqueta, input);
        caja.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    private static String valor(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private static String valor(ComboBox<String> combo) {
        return combo.getValue() == null ? "" : combo.getValue().trim();
    }

    private static String config(String clave, String porDefecto) {
        try {
            return AppConfig.obtener(clave, porDefecto);
        } catch (RuntimeException e) {
            return porDefecto;
        }
    }

    private void bloquearBotones(boolean bloquear) {
        anterior.setDisable(bloquear || paso == 0);
        siguiente.setDisable(bloquear);
        instalar.setDisable(bloquear);
    }

    private void mostrarError(String mensaje) {
        etiquetaInfo.setVisible(false);
        etiquetaInfo.setManaged(false);
        etiquetaError.setText(mensaje == null ? "No se pudo completar la operacion" : mensaje);
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    private void mostrarInfo(String mensaje) {
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
        etiquetaInfo.setText(mensaje);
        etiquetaInfo.setVisible(true);
        etiquetaInfo.setManaged(true);
    }

    private void ocultarMensajes() {
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
        etiquetaInfo.setVisible(false);
        etiquetaInfo.setManaged(false);
    }

    private String mensaje(RuntimeException e) {
        String mensaje = e.getMessage();
        return mensaje == null || mensaje.isBlank() ? e.getClass().getSimpleName() : mensaje;
    }
}
