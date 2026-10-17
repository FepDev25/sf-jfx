package ec.sfjfx.ui.vistas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.modelo.Usuario;
import ec.sfjfx.core.reporte.CatalogoReportes;
import ec.sfjfx.core.reporte.ColumnaReporte;
import ec.sfjfx.core.reporte.DefinicionReporte;
import ec.sfjfx.core.reporte.ExportadorReporte;
import ec.sfjfx.core.reporte.FiltroReporte;
import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.core.reporte.GrupoReporte;
import ec.sfjfx.core.reporte.ReporteGuardado;
import ec.sfjfx.core.reporte.ResultadoReporte;
import ec.sfjfx.core.reporte.ServicioReportes;
import ec.sfjfx.core.reporte.TipoColumna;
import ec.sfjfx.core.repositorio.CategoriaRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.repositorio.UsuarioRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.ui.componentes.GraficoSeries;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Opcion;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Visor de reportes con filtros, graficos, exportacion y configuraciones guardadas. */
public final class VistaReportes extends BorderPane {

    private final ServicioReportes servicio = new ServicioReportes();

    private final ComboBox<GrupoReporte> grupo = new ComboBox<>();
    private final ListView<DefinicionReporte> lista = new ListView<>();
    private final DatePicker desde = new DatePicker(LocalDate.now().minusDays(29));
    private final DatePicker hasta = new DatePicker(LocalDate.now());
    private final ComboBox<Opcion<Sucursal>> sucursal = new ComboBox<>();
    private final ComboBox<Opcion<Categoria>> categoria = new ComboBox<>();
    private final ComboBox<Opcion<Usuario>> usuario = new ComboBox<>();
    private final ComboBox<ReporteGuardado> guardados = new ComboBox<>();

    private final Label tituloReporte = new Label("Seleccione un reporte");
    private final Label descripcionReporte = new Label();
    private final Label estado = new Label();
    private final VBox panelGrafico = new VBox();
    private final TableView<ObservableList<Object>> tabla = new TableView<>();
    private final Label totales = new Label();
    private final Button exportarCsv = new Button("CSV");
    private final Button exportarXlsx = new Button("Excel");
    private final Button exportarPdf = new Button("PDF");
    private final Button guardar = new Button("Guardar vista");
    private final Button eliminarGuardado = new Button("Eliminar");

    private ResultadoReporte actual;

    public VistaReportes() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));
        setTop(construirCabecera());
        setLeft(construirLateral());
        setCenter(construirResultados());
        cargarCatalogos();
        cargarGuardados();
    }

    private Node construirCabecera() {
        Label titulo = new Label("Reportes y analítica");
        titulo.getStyleClass().add("titulo-vista");
        Label detalle = new Label("Información del negocio filtrable y exportable.");
        detalle.getStyleClass().add("texto-suave");

        grupo.getStyleClass().add("selector");
        grupo.setPrefWidth(200);
        grupo.valueProperty().addListener((o, a, b) -> actualizarLista());
        FlowPane filtros = new FlowPane(10, 8,
                campo("Grupo", grupo),
                campo("Desde", desde),
                campo("Hasta", hasta),
                campo("Sucursal", sucursal),
                campo("Categoría", categoria),
                campo("Cajero / usuario", usuario));
        filtros.setPadding(new Insets(12, 0, 0, 0));

        Button ejecutar = new Button("Ejecutar");
        ejecutar.getStyleClass().add("boton-primario");
        ejecutar.setOnAction(e -> ejecutar());

        Button limpiar = new Button("Limpiar filtros");
        limpiar.getStyleClass().add("boton-secundario");
        limpiar.setOnAction(e -> limpiarFiltros());

        guardar.getStyleClass().add("boton-secundario");
        guardar.setOnAction(e -> guardarVista());
        eliminarGuardado.getStyleClass().add("boton-secundario");
        eliminarGuardado.setOnAction(e -> eliminarVista());
        eliminarGuardado.setDisable(true);
        boolean puedeGuardar = Perm.tiene("reportes.crear");
        guardar.setVisible(puedeGuardar);
        guardar.setManaged(puedeGuardar);
        eliminarGuardado.setVisible(puedeGuardar);
        eliminarGuardado.setManaged(puedeGuardar);
        guardados.getStyleClass().add("selector");
        guardados.setPrefWidth(220);
        guardados.setPromptText("Vistas guardadas");
        guardados.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                cargarVista(b);
            }
            eliminarGuardado.setDisable(b == null);
        });

        HBox acciones = new HBox(8, campo("Vista guardada", guardados), guardar, eliminarGuardado,
                separadorVertical(), ejecutar, limpiar);
        acciones.setAlignment(Pos.CENTER_LEFT);
        acciones.setPadding(new Insets(10, 0, 0, 0));

        VBox cabecera = new VBox(2, titulo, detalle, filtros, acciones);
        cabecera.setPadding(new Insets(0, 0, 12, 0));
        return cabecera;
    }

    private Node construirLateral() {
        VBox lateral = new VBox(8);
        lateral.setPrefWidth(250);
        lateral.setMinWidth(230);
        lateral.setPadding(new Insets(0, 16, 0, 0));

        Label etiqueta = new Label("Reportes disponibles");
        etiqueta.getStyleClass().add("panel-titulo");
        lista.setPrefHeight(600);
        VBox.setVgrow(lista, Priority.ALWAYS);
        lista.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                prepararReporte(b);
            }
        });
        lateral.getChildren().addAll(etiqueta, lista);
        return lateral;
    }

    private Node construirResultados() {
        tituloReporte.getStyleClass().add("titulo-seccion");
        descripcionReporte.getStyleClass().add("texto-suave");
        descripcionReporte.setWrapText(true);
        estado.getStyleClass().add("texto-suave");

        panelGrafico.setPadding(new Insets(4, 0, 8, 0));

        Node botones = botonesExportacion();
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(8, estado, espacio, botones);
        barra.setAlignment(Pos.CENTER_LEFT);

        totales.getStyleClass().add("total-reporte");
        totales.setWrapText(true);

        tabla.setPlaceholder(new Label("Ejecute un reporte para ver los datos"));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox caja = new VBox(6, tituloReporte, descripcionReporte, panelGrafico, barra, tabla, totales);
        caja.setFillWidth(true);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return caja;
    }

    private Node botonesExportacion() {
        for (Button b : List.of(exportarCsv, exportarXlsx, exportarPdf)) {
            b.getStyleClass().add("boton-secundario");
            b.setDisable(true);
        }
        exportarCsv.setOnAction(e -> exportar("csv"));
        exportarXlsx.setOnAction(e -> exportar("xlsx"));
        exportarPdf.setOnAction(e -> exportar("pdf"));
        HBox caja = new HBox(6, exportarCsv, exportarXlsx, exportarPdf);
        boolean puedeExportar = Perm.tiene("reportes.exportar");
        caja.setVisible(puedeExportar);
        caja.setManaged(puedeExportar);
        return caja;
    }

    private void cargarCatalogos() {
        grupo.getItems().setAll(GrupoReporte.values());
        grupo.setValue(GrupoReporte.VENTAS);
        actualizarLista();

        long empresaId = SesionActual.requerida().empresa().id();
        sucursal.getItems().add(new Opcion<>("Todas", null));
        new SucursalRepositorio().porEmpresa(empresaId).forEach(s ->
                sucursal.getItems().add(new Opcion<>(s.nombre(), s)));
        sucursal.getSelectionModel().selectFirst();

        categoria.getItems().add(new Opcion<>("Todas", null));
        new CategoriaRepositorio().listar(empresaId).forEach(c ->
                categoria.getItems().add(new Opcion<>(c.nombre(), c)));
        categoria.getSelectionModel().selectFirst();

        usuario.getItems().add(new Opcion<>("Todos", null));
        new UsuarioRepositorio().listar(empresaId).forEach(u ->
                usuario.getItems().add(new Opcion<>(u.nombreCompleto(), u)));
        usuario.getSelectionModel().selectFirst();

        desde.getStyleClass().add("selector");
        hasta.getStyleClass().add("selector");
        desde.setPrefWidth(150);
        hasta.setPrefWidth(150);
    }

    private void cargarGuardados() {
        guardados.getItems().setAll(servicio.guardados());
        guardados.getSelectionModel().clearSelection();
    }

    private void actualizarLista() {
        GrupoReporte seleccionado = grupo.getValue();
        List<DefinicionReporte> definiciones = seleccionado == null
                ? servicio.catalogo()
                : CatalogoReportes.porGrupo(seleccionado);
        lista.getItems().setAll(definiciones);
        if (!definiciones.isEmpty()) {
            lista.getSelectionModel().selectFirst();
        }
    }

    private void prepararReporte(DefinicionReporte definicion) {
        tituloReporte.setText(definicion.nombre());
        descripcionReporte.setText(definicion.descripcion());
        ejecutar();
    }

    private void ejecutar() {
        DefinicionReporte definicion = lista.getSelectionModel().getSelectedItem();
        if (definicion == null) {
            return;
        }
        if (desde.getValue() != null && hasta.getValue() != null && desde.getValue().isAfter(hasta.getValue())) {
            Alertas.advertencia("Filtros", "La fecha inicial no puede ser posterior a la final.");
            return;
        }
        try {
            FiltroReporte filtro = filtro();
            ResultadoReporte resultado = servicio.ejecutar(definicion.codigo(), filtro);
            mostrar(resultado);
        } catch (RuntimeException e) {
            Alertas.error("No se pudo ejecutar el reporte", e.getMessage());
        }
    }

    private FiltroReporte filtro() {
        Sucursal s = sucursal.getValue() == null ? null : sucursal.getValue().valor();
        Categoria c = categoria.getValue() == null ? null : categoria.getValue().valor();
        Usuario u = usuario.getValue() == null ? null : usuario.getValue().valor();
        return new FiltroReporte(desde.getValue(), hasta.getValue(),
                s == null ? null : s.id(), c == null ? null : c.id(), u == null ? null : u.id());
    }

    private void mostrar(ResultadoReporte resultado) {
        actual = resultado;
        tituloReporte.setText(resultado.definicion().nombre());
        descripcionReporte.setText(resultado.definicion().descripcion()
                + "  ·  " + resultado.filtro().descripcion());
        estado.setText(resultado.filas().size() + " fila(s)");

        boolean conGrafico = resultado.definicion().tieneGrafico() && !resultado.serie().isEmpty();
        panelGrafico.setVisible(conGrafico);
        panelGrafico.setManaged(conGrafico);
        if (conGrafico) {
            Node grafico = GraficoSeries.crear(resultado.definicion().grafico(), resultado.serie(), "");
            if (grafico instanceof javafx.scene.chart.Chart chart) {
                chart.setPrefHeight(260);
                chart.setMinHeight(230);
            }
            panelGrafico.getChildren().setAll(grafico);
        } else {
            panelGrafico.getChildren().clear();
        }

        tabla.getColumns().clear();
        for (int i = 0; i < resultado.columnas().size(); i++) {
            tabla.getColumns().add(columna(resultado.columnas().get(i), i));
        }
        ObservableList<ObservableList<Object>> filas = FXCollections.observableArrayList();
        for (List<Object> fila : resultado.filas()) {
            filas.add(FXCollections.observableArrayList(fila));
        }
        tabla.setItems(filas);
        tabla.setPlaceholder(new Label("Sin datos para los filtros seleccionados"));

        totales.setText(textoTotales(resultado));

        boolean hayResultado = true;
        exportarCsv.setDisable(!hayResultado);
        exportarXlsx.setDisable(!hayResultado);
        exportarPdf.setDisable(!hayResultado);
        guardar.setDisable(false);
    }

    private TableColumn<ObservableList<Object>, Object> columna(ColumnaReporte columna, int indice) {
        TableColumn<ObservableList<Object>, Object> tc = new TableColumn<>(columna.titulo());
        tc.setCellValueFactory(datos -> {
            ObservableList<Object> fila = datos.getValue();
            return new ReadOnlyObjectWrapper<>(indice < fila.size() ? fila.get(indice) : null);
        });
        tc.setCellFactory(t -> new TableCell<>() {
            @Override
            protected void updateItem(Object valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio ? null : FormatoReporte.valor(valor, columna.tipo()));
                setAlignment(columna.tipo().esNumerica() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            }
        });
        tc.setPrefWidth(columna.tipo().esNumerica() ? 110 : 180);
        return tc;
    }

    private String textoTotales(ResultadoReporte resultado) {
        List<String> partes = new java.util.ArrayList<>();
        for (int i = 0; i < resultado.columnas().size(); i++) {
            if (resultado.totales().get(i) != null) {
                partes.add(resultado.columnas().get(i).titulo() + ": "
                        + FormatoReporte.valor(resultado.totales().get(i), resultado.columnas().get(i).tipo()));
            }
        }
        return partes.isEmpty() ? "" : "Totales — " + String.join("   ·   ", partes);
    }

    private void exportar(String formato) {
        if (actual == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar reporte");
        selector.setInitialFileName(actual.codigo() + "-" + LocalDate.now() + "." + formato);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                formato.toUpperCase(), "*." + formato));
        File destino = selector.showSaveDialog(ventana());
        if (destino == null) {
            return;
        }
        try {
            byte[] datos = switch (formato) {
                case "csv" -> ExportadorReporte.csv(actual);
                case "xlsx" -> ExportadorReporte.xlsx(actual);
                case "pdf" -> ExportadorReporte.pdf(actual);
                default -> throw new IllegalArgumentException("Formato no soportado: " + formato);
            };
            Files.write(destino.toPath(), datos);
            Alertas.info("Exportación", "Reporte exportado en " + destino.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            Alertas.error("No se pudo exportar", e.getMessage());
        }
    }

    private void guardarVista() {
        if (actual == null) {
            Alertas.advertencia("Guardar vista", "Ejecute un reporte antes de guardar la vista.");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog(actual.definicion().nombre() + " " + LocalDate.now());
        dialogo.setTitle("Guardar vista");
        dialogo.setHeaderText("Nombre de la configuración");
        dialogo.setContentText("Nombre:");
        if (ventana() != null) {
            dialogo.initOwner(ventana());
        }
        Optional<String> nombre = dialogo.showAndWait();
        if (nombre.isEmpty() || nombre.get().isBlank()) {
            return;
        }
        try {
            servicio.guardar(actual.codigo(), nombre.get(), actual.filtro());
            cargarGuardados();
            Alertas.info("Vista guardada", "La configuración quedó disponible para este usuario.");
        } catch (RuntimeException e) {
            Alertas.error("No se pudo guardar", e.getMessage());
        }
    }

    private void cargarVista(ReporteGuardado guardado) {
        Optional<DefinicionReporte> definicion = CatalogoReportes.porCodigo(guardado.codigo());
        if (definicion.isEmpty()) {
            return;
        }
        grupo.setValue(definicion.get().grupo());
        actualizarLista();
        lista.getSelectionModel().select(definicion.get());
        desde.setValue(guardado.desde());
        hasta.setValue(guardado.hasta());
        seleccionarSucursal(guardado.sucursalId());
        seleccionarCategoria(guardado.categoriaId());
        seleccionarUsuario(guardado.filtroUsuarioId());
        ejecutar();
    }

    private void eliminarVista() {
        ReporteGuardado seleccionado = guardados.getValue();
        if (seleccionado == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la vista \"" + seleccionado.nombre() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        confirmacion.initOwner(ventana());
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            servicio.eliminarGuardado(seleccionado.id());
            cargarGuardados();
        }
    }

    private void seleccionarSucursal(Long id) {
        for (Opcion<Sucursal> opcion : sucursal.getItems()) {
            if (igual(opcion.valor() == null ? null : opcion.valor().id(), id)) {
                sucursal.setValue(opcion);
                return;
            }
        }
        sucursal.getSelectionModel().selectFirst();
    }

    private void seleccionarCategoria(Long id) {
        for (Opcion<Categoria> opcion : categoria.getItems()) {
            if (igual(opcion.valor() == null ? null : opcion.valor().id(), id)) {
                categoria.setValue(opcion);
                return;
            }
        }
        categoria.getSelectionModel().selectFirst();
    }

    private void seleccionarUsuario(Long id) {
        for (Opcion<Usuario> opcion : usuario.getItems()) {
            if (igual(opcion.valor() == null ? null : opcion.valor().id(), id)) {
                usuario.setValue(opcion);
                return;
            }
        }
        usuario.getSelectionModel().selectFirst();
    }

    private static boolean igual(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private void limpiarFiltros() {
        desde.setValue(LocalDate.now().minusDays(29));
        hasta.setValue(LocalDate.now());
        sucursal.getSelectionModel().selectFirst();
        categoria.getSelectionModel().selectFirst();
        usuario.getSelectionModel().selectFirst();
        ejecutar();
    }

    private static VBox campo(String etiqueta, javafx.scene.control.Control control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("etiqueta-form");
        control.setMaxWidth(220);
        return new VBox(3, label, control);
    }

    private static Node separadorVertical() {
        Region separador = new Region();
        separador.setPrefWidth(1);
        separador.setMinHeight(30);
        separador.getStyleClass().add("sidebar-separador");
        return separador;
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
