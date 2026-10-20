package ec.sfjfx.ui.vistas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.GestorModulos;
import ec.sfjfx.core.servicio.ServicioLicencia;
import ec.sfjfx.core.servicio.ServicioRespaldo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Shell principal: cabecera, menu lateral por modulos y area de contenido. */
public final class Shell extends BorderPane {

    private final Sesion sesion;
    private final SucursalRepositorio sucursales = new SucursalRepositorio();
    private final CajaRepositorio cajas = new CajaRepositorio();

    private final StackPane contenido = new StackPane();
    private final ComboBox<Caja> selectorCaja = new ComboBox<>();
    private final AtomicBoolean respaldoEnCurso = new AtomicBoolean();
    private Timeline programador;

    public Shell(Sesion sesion, Runnable alSalir) {
        this.sesion = sesion;
        getStyleClass().add("raiz");
        VBox superior = new VBox();
        superior.getChildren().addAll(construirCabecera(alSalir), construirAvisoLicencia());
        setTop(superior);
        setLeft(construirSidebar());
        setCenter(contenido);
        mostrarInicio();
        iniciarProgramadorRespaldos();
    }

    private Region construirCabecera(Runnable alSalir) {
        Label empresa = new Label(sesion.empresa().nombreComercial() != null
                ? sesion.empresa().nombreComercial()
                : sesion.empresa().razonSocial());
        empresa.getStyleClass().add("cabecera-empresa");
        ComboBox<Sucursal> selectorSucursal = new ComboBox<>();
        selectorSucursal.getItems().setAll(
                sucursales.porUsuario(sesion.usuario().id(), sesion.empresa().id()));
        selectorSucursal.getStyleClass().add("selector");
        selectorSucursal.valueProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                sesion.seleccionarSucursal(nueva);
                recargarCajas();
            }
        });
        if (!selectorSucursal.getItems().isEmpty()) {
            selectorSucursal.getSelectionModel().selectFirst();
        }

        Label etiquetaCaja = new Label("Caja:");
        etiquetaCaja.getStyleClass().add("texto-suave");
        selectorCaja.getStyleClass().add("selector");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Label usuario = new Label(sesion.usuario().nombreCompleto());
        usuario.getStyleClass().add("cabecera-usuario");

        Button salir = new Button("Salir");
        salir.getStyleClass().add("boton-secundario");
        salir.setOnAction(e -> {
            if (programador != null) {
                programador.stop();
            }
            SesionActual.cerrar();
            alSalir.run();
        });

        HBox cabecera = new HBox(12, empresa, selectorSucursal, etiquetaCaja, selectorCaja,
                espaciador, usuario, salir);
        cabecera.getStyleClass().add("cabecera");
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(10, 16, 10, 16));
        return cabecera;
    }

    /**
     * Banda de aviso cuando la instalacion esta en modo consulta (licencia vencida o periodo de
     * prueba agotado). Solo informa; el bloqueo real esta en los servicios.
     */
    private Region construirAvisoLicencia() {
        Label aviso = new Label();
        aviso.getStyleClass().add("aviso-licencia");
        try {
            ServicioLicencia.EstadoLicencia estado = new ServicioLicencia().estado();
            if (estado.modoConsulta()) {
                aviso.setText("Modo consulta: " + (estado.motivo() == null
                        ? "la licencia no esta vigente" : estado.motivo())
                        + " No se pueden registrar ventas, compras ni comprobantes.");
                aviso.setVisible(true);
                aviso.setManaged(true);
            }
        } catch (RuntimeException e) {
            aviso.setVisible(false);
            aviso.setManaged(false);
        }
        return aviso;
    }

    private void recargarCajas() {
        selectorCaja.getItems().clear();
        if (sesion.sucursal() == null) {
            return;
        }
        List<Caja> disponibles = cajas.porSucursal(sesion.sucursal().id());
        selectorCaja.getItems().setAll(disponibles);
        if (!disponibles.isEmpty()) {
            selectorCaja.getSelectionModel().selectFirst();
            sesion.seleccionarCaja(selectorCaja.getValue());
        }
    }

    private Region construirSidebar() {
        Label marca = new Label("sf-jfx");
        marca.getStyleClass().add("marca");
        VBox encabezado = new VBox(marca);
        encabezado.getStyleClass().add("sidebar-encabezado");

        VBox items = new VBox(2);
        items.getStyleClass().add("sidebar-items");

        Button inicio = botonNav("Inicio", () -> mostrarInicio());
        inicio.getStyleClass().add("activo");
        items.getChildren().add(inicio);
        items.getChildren().add(separador());

        List<ModuloSistema> visibles = new GestorModulos().visibles(sesion, sesion.empresa().id());
        Map<String, List<ModuloSistema>> porGrupo = agrupar(visibles);
        for (Map.Entry<String, List<ModuloSistema>> grupo : porGrupo.entrySet()) {
            Label titulo = new Label(grupo.getKey().toUpperCase());
            titulo.getStyleClass().add("sidebar-grupo");
            items.getChildren().add(titulo);
            for (ModuloSistema modulo : grupo.getValue()) {
                items.getChildren().add(botonNav(modulo.etiqueta(), () -> mostrarModulo(modulo)));
            }
            items.getChildren().add(separador());
        }

        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("sidebar-scroll");

        VBox sidebar = new VBox(encabezado, scroll);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return sidebar;
    }

    private Map<String, List<ModuloSistema>> agrupar(List<ModuloSistema> modulos) {
        Map<String, List<ModuloSistema>> mapa = new LinkedHashMap<>();
        for (ModuloSistema m : modulos) {
            mapa.computeIfAbsent(m.grupo(), k -> new java.util.ArrayList<>()).add(m);
        }
        return mapa;
    }

    private Button botonNav(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("nav-item");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setAlignment(Pos.CENTER_LEFT);
        boton.setOnAction(e -> {
            for (Node nodo : ((VBox) boton.getParent()).getChildren()) {
                nodo.getStyleClass().remove("activo");
            }
            boton.getStyleClass().add("activo");
            accion.run();
        });
        return boton;
    }

    private Region separador() {
        Region linea = new Region();
        linea.getStyleClass().add("sidebar-separador");
        return linea;
    }

    private void mostrarInicio() {
        contenido.getChildren().setAll(new ec.sfjfx.ui.vistas.VistaDashboard());
    }

    private void iniciarProgramadorRespaldos() {
        programador = new Timeline(new KeyFrame(Duration.minutes(15), e -> ejecutarRespaldoProgramado()));
        programador.setCycleCount(Timeline.INDEFINITE);
        programador.play();
    }

    private void ejecutarRespaldoProgramado() {
        if (!respaldoEnCurso.compareAndSet(false, true)) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                new ServicioRespaldo().ejecutarProgramadoSiCorresponde();
            } catch (RuntimeException e) {
                System.err.println("Respaldo programado fallo: " + e.getMessage());
            } finally {
                respaldoEnCurso.set(false);
            }
        }, "respaldo-programado");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarModulo(ModuloSistema modulo) {
        Node vista = ec.sfjfx.ui.navegacion.RegistroVistas.para(modulo);
        if (vista != null) {
            contenido.getChildren().setAll(vista);
            return;
        }
        Label titulo = new Label(modulo.etiqueta());
        titulo.getStyleClass().add("titulo-vista");
        Label detalle = new Label("Modulo habilitado. Implementacion segun roadmap.");
        detalle.getStyleClass().add("texto-suave");
        Label codigo = new Label("Modulo: " + modulo.codigo() + "  ·  Grupo: " + modulo.grupo());
        codigo.getStyleClass().add("texto-suave");
        contenido.getChildren().setAll(envolver(new VBox(6, titulo, detalle, codigo)));
    }

    private VBox envolver(VBox interior) {
        interior.setPadding(new Insets(28));
        interior.setAlignment(Pos.TOP_LEFT);
        VBox caja = new VBox(interior);
        caja.getStyleClass().add("contenido");
        VBox.setVgrow(interior, Priority.ALWAYS);
        return caja;
    }
}
