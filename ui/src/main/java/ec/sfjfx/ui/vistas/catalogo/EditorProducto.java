package ec.sfjfx.ui.vistas.catalogo;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ec.sfjfx.core.modelo.AtributoDefinicion;
import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.CodigoBarra;
import ec.sfjfx.core.modelo.ComponenteKit;
import ec.sfjfx.core.modelo.EmpaqueProducto;
import ec.sfjfx.core.modelo.ImagenProducto;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.Marca;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoCodigoBarra;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.UnidadMedida;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioAtributo;
import ec.sfjfx.core.servicio.ServicioCategoria;
import ec.sfjfx.core.servicio.ServicioImpuesto;
import ec.sfjfx.core.servicio.ServicioListaPrecio;
import ec.sfjfx.core.servicio.ServicioMarca;
import ec.sfjfx.core.servicio.ServicioProducto;
import ec.sfjfx.core.servicio.ServicioUnidadMedida;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Editor de producto con pestanas. */
public final class EditorProducto {

    private final ServicioProducto servicio = new ServicioProducto();
    private final long empresaId = SesionActual.requerida().empresa().id();
    private final ProductoCompleto existente;
    private final Window propietario;

    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private boolean guardado;

    // General
    private final TextField codigo = Fx.campo("");
    private final TextField nombre = Fx.campo("");
    private final TextArea descripcion = new TextArea();
    private final ComboBox<Categoria> categoria = new ComboBox<>();
    private final ComboBox<Marca> marca = new ComboBox<>();
    private final ComboBox<UnidadMedida> unidad = new ComboBox<>();
    private final ComboBox<TipoProducto> tipo = new ComboBox<>();
    private final ComboBox<Producto> padreProducto = new ComboBox<>();
    private final TextField costo = Fx.campo("0");
    private final TextField costoPromedio = Fx.campo("0");
    private final TextField codigoPlu = Fx.campo("");
    private final TextField pesoNeto = Fx.campo("");
    private final TextField volumen = Fx.campo("");
    private final TextField ubicacion = Fx.campo("");
    private final CheckBox controlaLote = new CheckBox("Controla lote");
    private final CheckBox controlaSerie = new CheckBox("Controla serie");
    private final CheckBox esGranel = new CheckBox("Es granel (pesable)");
    private final CheckBox activo = new CheckBox("Activo");

    // Colecciones
    private final ObservableList<CodigoBarra> codigos = FXCollections.observableArrayList();
    private final ObservableList<EmpaqueProducto> empaques = FXCollections.observableArrayList();
    private final ObservableList<ImagenProducto> imagenes = FXCollections.observableArrayList();
    private final ObservableList<ComponenteKit> componentes = FXCollections.observableArrayList();
    private final Map<Long, TextField> precios = new LinkedHashMap<>();
    private final Map<Long, CheckBox> impuestos = new LinkedHashMap<>();
    private final Map<Long, TextField> atributosCampos = new LinkedHashMap<>();
    private VBox panelAtributos = new VBox(8);

    private final ServicioCategoria servicioCategoria = new ServicioCategoria();
    private final ServicioMarca servicioMarca = new ServicioMarca();
    private final ServicioUnidadMedida servicioUnidad = new ServicioUnidadMedida();
    private final ServicioImpuesto servicioImpuesto = new ServicioImpuesto();
    private final ServicioListaPrecio servicioLista = new ServicioListaPrecio();
    private final ServicioAtributo servicioAtributo = new ServicioAtributo();

    public EditorProducto(ProductoCompleto existente, Window propietario) {
        this.existente = existente;
        this.propietario = propietario;
        construir();
        cargarDatos();
    }

    public boolean abrir() {
        dialogo.showAndWait();
        return guardado;
    }

    private void construir() {
        dialogo.setTitle(existente == null ? "Nuevo producto" : "Editar producto");
        dialogo.setHeaderText(null);
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        ButtonType guardarBtnType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardarBtnType, ButtonType.CANCEL);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("General", tabGeneral()),
                new Tab("Precios", tabPrecios()),
                new Tab("Impuestos", tabImpuestos()),
                new Tab("Codigos de barras", tabCodigos()),
                new Tab("Empaques", tabEmpaques()),
                new Tab("Imagenes", tabImagenes()),
                new Tab("Atributos", tabAtributos()),
                new Tab("Componentes (kits)", tabComponentes()));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        dialogo.getDialogPane().setContent(tabs);
        dialogo.getDialogPane().setPrefSize(940, 640);

        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardarBtnType);
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                guardar();
                guardado = true;
            } catch (RuntimeException e) {
                Alertas.error("No se pudo guardar", e.getMessage());
                evento.consume();
            }
        });
    }

    // ---------------------------------------------------------------- pestanas

    private GridPane tabGeneral() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(16));
        categoria.setMaxWidth(Double.MAX_VALUE);
        marca.setMaxWidth(Double.MAX_VALUE);
        unidad.setMaxWidth(Double.MAX_VALUE);
        tipo.getItems().setAll(TipoProducto.values());
        tipo.setMaxWidth(Double.MAX_VALUE);
        tipo.valueProperty().addListener((o, a, b) -> {
            recargarAtributos();
            padreProducto.setDisable(b != TipoProducto.VARIANTE);
        });
        padreProducto.setMaxWidth(Double.MAX_VALUE);
        padreProducto.setDisable(true);
        descripcion.getStyleClass().add("campo");
        descripcion.setPrefRowCount(2);
        costoPromedio.setDisable(true);

        int r = 0;
        g.add(etiqueta("Codigo (SKU)"), 0, r);
        g.add(codigo, 1, r++);
        g.add(etiqueta("Nombre"), 2, r);
        g.add(nombre, 3, r++);
        g.add(etiqueta("Descripcion"), 0, r);
        g.add(descripcion, 1, r, 3, 1);
        r++;
        g.add(etiqueta("Categoria"), 0, r);
        g.add(categoria, 1, r);
        g.add(etiqueta("Marca"), 2, r);
        g.add(marca, 3, r++);
        g.add(etiqueta("Unidad"), 0, r);
        g.add(unidad, 1, r);
        g.add(etiqueta("Tipo"), 2, r);
        g.add(tipo, 3, r++);
        g.add(etiqueta("Producto padre (variantes)"), 0, r);
        g.add(padreProducto, 1, r, 3, 1);
        r++;
        g.add(etiqueta("Costo"), 0, r);
        g.add(costo, 1, r);
        g.add(etiqueta("Costo promedio (PPP)"), 2, r);
        g.add(costoPromedio, 3, r++);
        g.add(etiqueta("Codigo PLU"), 0, r);
        g.add(codigoPlu, 1, r);
        g.add(etiqueta("Ubicacion"), 2, r);
        g.add(ubicacion, 3, r++);
        g.add(etiqueta("Peso neto"), 0, r);
        g.add(pesoNeto, 1, r);
        g.add(etiqueta("Volumen"), 2, r);
        g.add(volumen, 3, r++);
        HBox checks = new HBox(16, controlaLote, controlaSerie, esGranel, activo);
        g.add(checks, 0, r, 4, 1);
        return g;
    }

    private VBox tabPrecios() {
        VBox v = new VBox(8);
        v.setPadding(new Insets(16));
        v.getChildren().add(new Label("Precio por lista (deje 0 para no asignar)"));
        for (ListaPrecio lista : servicioLista.activas()) {
            TextField campo = Fx.campo("0");
            precios.put(lista.id(), campo);
            HBox fila = new HBox(10, new Label(lista.nombre()), campo);
            fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            campo.setPrefWidth(140);
            v.getChildren().add(fila);
        }
        return v;
    }

    private VBox tabImpuestos() {
        VBox v = new VBox(8);
        v.setPadding(new Insets(16));
        v.getChildren().add(new Label("Impuestos aplicables"));
        for (Impuesto imp : servicioImpuesto.deVenta()) {
            CheckBox check = new CheckBox(imp.nombre());
            impuestos.put(imp.id(), check);
            v.getChildren().add(check);
        }
        return v;
    }

    private VBox tabCodigos() {
        TableView<CodigoBarra> tabla = new TableView<>(codigos);
        tabla.setPrefHeight(240);
        var cCodigo = new javafx.scene.control.TableColumn<CodigoBarra, String>("Codigo");
        cCodigo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().codigo()));
        cCodigo.setPrefWidth(220);
        var cTipo = new javafx.scene.control.TableColumn<CodigoBarra, String>("Tipo");
        cTipo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().tipo().etiqueta()));
        cTipo.setPrefWidth(120);
        var cPrinc = new javafx.scene.control.TableColumn<CodigoBarra, String>("Principal");
        cPrinc.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esPrincipal())));
        cPrinc.setPrefWidth(90);
        tabla.getColumns().addAll(cCodigo, cTipo, cPrinc);

        TextField campoCodigo = Fx.campo("");
        campoCodigo.setPromptText("Codigo");
        ComboBox<TipoCodigoBarra> campoTipo = new ComboBox<>();
        campoTipo.getItems().setAll(TipoCodigoBarra.values());
        campoTipo.setValue(TipoCodigoBarra.EAN13);
        CheckBox principal = new CheckBox("Principal");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            if (Fx.texto(campoCodigo).isBlank()) {
                return;
            }
            if (principal.isSelected()) {
                codigos.replaceAll(c -> new CodigoBarra(c.id(), c.productoId(), c.codigo(), c.tipo(), false, c.empaqueId()));
            }
            codigos.add(new CodigoBarra(null, 0, Fx.texto(campoCodigo), campoTipo.getValue(), principal.isSelected(), null));
            campoCodigo.clear();
            principal.setSelected(false);
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> codigos.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, campoCodigo, campoTipo, principal, agregar, quitar);
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox v = new VBox(10, tabla, barra);
        v.setPadding(new Insets(16));
        return v;
    }

    private VBox tabEmpaques() {
        TableView<EmpaqueProducto> tabla = new TableView<>(empaques);
        tabla.setPrefHeight(240);
        var cNombre = new javafx.scene.control.TableColumn<EmpaqueProducto, String>("Nombre");
        cNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().nombre()));
        cNombre.setPrefWidth(180);
        var cFactor = new javafx.scene.control.TableColumn<EmpaqueProducto, String>("Factor");
        cFactor.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().factor() == null ? "" : c.getValue().factor().toPlainString()));
        cFactor.setPrefWidth(100);
        var cCompra = new javafx.scene.control.TableColumn<EmpaqueProducto, String>("Compra");
        cCompra.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esCompra())));
        cCompra.setPrefWidth(80);
        var cVenta = new javafx.scene.control.TableColumn<EmpaqueProducto, String>("Venta");
        cVenta.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esVenta())));
        cVenta.setPrefWidth(80);
        tabla.getColumns().addAll(cNombre, cFactor, cCompra, cVenta);

        TextField campoNombre = Fx.campo("");
        campoNombre.setPromptText("Nombre (ej. Caja x24)");
        TextField campoFactor = Fx.campo("1");
        campoFactor.setPrefWidth(80);
        CheckBox compra = new CheckBox("Compra");
        CheckBox venta = new CheckBox("Venta");
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            if (Fx.texto(campoNombre).isBlank()) {
                return;
            }
            empaques.add(new EmpaqueProducto(null, 0, Fx.texto(campoNombre), null, decimal(Fx.texto(campoFactor)),
                    compra.isSelected(), venta.isSelected(), null, true));
            campoNombre.clear();
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> empaques.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, campoNombre, campoFactor, compra, venta, agregar, quitar);
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox v = new VBox(10, tabla, barra);
        v.setPadding(new Insets(16));
        return v;
    }

    private VBox tabImagenes() {
        TableView<ImagenProducto> tabla = new TableView<>(imagenes);
        tabla.setPrefHeight(260);
        var cNombre = new javafx.scene.control.TableColumn<ImagenProducto, String>("Archivo");
        cNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().nombreArchivo() == null ? c.getValue().ruta() : c.getValue().nombreArchivo()));
        cNombre.setPrefWidth(320);
        var cPrinc = new javafx.scene.control.TableColumn<ImagenProducto, String>("Principal");
        cPrinc.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Fx.siNo(c.getValue().esPrincipal())));
        cPrinc.setPrefWidth(90);
        tabla.getColumns().addAll(cNombre, cPrinc);

        Button agregar = new Button("Agregar imagen");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> agregarImagen());
        Button principal = new Button("Marcar principal");
        principal.getStyleClass().add("boton-secundario");
        principal.setOnAction(e -> {
            ImagenProducto sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                for (int i = 0; i < imagenes.size(); i++) {
                    ImagenProducto img = imagenes.get(i);
                    imagenes.set(i, new ImagenProducto(img.id(), img.productoId(), img.ruta(),
                            img.nombreArchivo(), img.orden(), img.equals(sel)));
                }
            }
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> imagenes.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, agregar, principal, quitar);
        VBox v = new VBox(10, tabla, barra);
        v.setPadding(new Insets(16));
        return v;
    }

    private VBox tabAtributos() {
        panelAtributos = new VBox(8);
        panelAtributos.setPadding(new Insets(16));
        recargarAtributos();
        return new VBox(panelAtributos);
    }

    private VBox tabComponentes() {
        TableView<ComponenteKit> tabla = new TableView<>(componentes);
        tabla.setPrefHeight(240);
        var cNombre = new javafx.scene.control.TableColumn<ComponenteKit, String>("Componente");
        cNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().componenteNombre()));
        cNombre.setPrefWidth(280);
        var cCant = new javafx.scene.control.TableColumn<ComponenteKit, String>("Cantidad");
        cCant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad().toPlainString()));
        cCant.setPrefWidth(100);
        tabla.getColumns().addAll(cNombre, cCant);

        ComboBox<Producto> campoProducto = new ComboBox<>();
        campoProducto.getItems().setAll(servicio.listar(null));
        campoProducto.setPrefWidth(260);
        TextField campoCantidad = Fx.campo("1");
        campoCantidad.setPrefWidth(70);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            Producto comp = campoProducto.getValue();
            if (comp == null) {
                return;
            }
            componentes.add(new ComponenteKit(null, 0, comp.id(), comp.nombre(), decimal(Fx.texto(campoCantidad))));
        });
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setOnAction(e -> componentes.remove(tabla.getSelectionModel().getSelectedItem()));

        HBox barra = new HBox(8, campoProducto, campoCantidad, agregar, quitar);
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox v = new VBox(10, new Label("Solo para productos tipo Kit"), tabla, barra);
        v.setPadding(new Insets(16));
        return v;
    }

    // ---------------------------------------------------------------- datos

    private void cargarDatos() {
        categoria.getItems().add(new Categoria(null, empresaId, null, null, "(sin categoria)", null, 0, true));
        categoria.getItems().addAll(servicioCategoria.listar());
        marca.getItems().add(new Marca(null, empresaId, "(sin marca)", null, null, true));
        marca.getItems().addAll(servicioMarca.listar());
        unidad.getItems().addAll(servicioUnidad.listar());
        padreProducto.getItems().setAll(servicio.listar(null).stream()
                .filter(p -> p.tipo() == TipoProducto.VARIANTE_PADRE).toList());

        if (existente == null) {
            categoria.setValue(categoria.getItems().get(0));
            marca.setValue(marca.getItems().get(0));
            if (!unidad.getItems().isEmpty()) {
                unidad.setValue(unidad.getItems().get(0));
            }
            tipo.setValue(TipoProducto.SIMPLE);
            activo.setSelected(true);
            return;
        }

        Producto p = existente.producto();
        codigo.setText(p.codigo());
        nombre.setText(p.nombre());
        descripcion.setText(p.descripcion() == null ? "" : p.descripcion());
        tipo.setValue(p.tipo());
        padreProducto.setDisable(p.tipo() != TipoProducto.VARIANTE);
        if (p.productoPadreId() != null) {
            padreProducto.getItems().stream().filter(x -> p.productoPadreId().equals(x.id()))
                    .findFirst().ifPresent(padreProducto::setValue);
        }
        costo.setText(valor(p.costo()));
        costoPromedio.setText(valor(p.costoPromedio()));
        codigoPlu.setText(p.codigoPlu() == null ? "" : p.codigoPlu());
        pesoNeto.setText(valor(p.pesoNeto()));
        volumen.setText(valor(p.volumen()));
        ubicacion.setText(p.ubicacion() == null ? "" : p.ubicacion());
        controlaLote.setSelected(p.controlaLote());
        controlaSerie.setSelected(p.controlaSerie());
        esGranel.setSelected(p.esGranel());
        activo.setSelected(p.activo());
        seleccionar(categoria, p.categoriaId());
        seleccionar(marca, p.marcaId());
        seleccionar(unidad, p.unidadMedidaId());
        codigos.setAll(existente.codigos());
        empaques.setAll(existente.empaques());
        imagenes.setAll(existente.imagenes());
        componentes.setAll(existente.componentes());
        existente.impuestoIds().forEach(id -> {
            if (impuestos.containsKey(id)) {
                impuestos.get(id).setSelected(true);
            }
        });
        for (PrecioProducto pp : existente.precios()) {
            if (precios.containsKey(pp.listaPrecioId())) {
                precios.get(pp.listaPrecioId()).setText(valor(pp.precio()));
            }
        }
        recargarAtributos();
    }

    private void recargarAtributos() {
        if (panelAtributos == null) {
            return;
        }
        atributosCampos.clear();
        panelAtributos.getChildren().clear();
        panelAtributos.getChildren().add(new Label("Atributos de la categoria (definidos en el catalogo)"));
        Long categoriaId = categoria.getValue() == null ? null : categoria.getValue().id();
        Map<Long, String> valores = existente == null ? Map.of() : existente.atributos();
        for (AtributoDefinicion def : servicioAtributo.aplicables(categoriaId)) {
            TextField campo = Fx.campo(valores.getOrDefault(def.id(), ""));
            campo.setPrefWidth(240);
            atributosCampos.put(def.id(), campo);
            HBox fila = new HBox(10, new Label(def.nombre()
                    + (def.obligatorio() ? " *" : "")), campo);
            fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            panelAtributos.getChildren().add(fila);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void seleccionar(ComboBox combo, Long id) {
        if (id == null) {
            return;
        }
        for (Object o : combo.getItems()) {
            if (id.equals(idDe(o))) {
                combo.getSelectionModel().select(o);
                return;
            }
        }
    }

    private Long idDe(Object o) {
        if (o instanceof Categoria c) {
            return c.id();
        }
        if (o instanceof Marca m) {
            return m.id();
        }
        if (o instanceof UnidadMedida u) {
            return u.id();
        }
        return null;
    }

    private void agregarImagen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File archivo = chooser.showOpenDialog(propietario);
        if (archivo == null) {
            return;
        }
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".sfjfx", "uploads", "productos");
            Files.createDirectories(dir);
            String nombreArchivo = archivo.getName();
            Path destino = dir.resolve(UUID.randomUUID() + "_" + nombreArchivo);
            Files.copy(archivo.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            imagenes.add(new ImagenProducto(null, 0, destino.toString(), nombreArchivo, imagenes.size(),
                    imagenes.isEmpty()));
        } catch (Exception e) {
            Alertas.error("Imagen", "No se pudo copiar la imagen: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- guardar

    private void guardar() {
        Long categoriaId = categoria.getValue() == null ? null : categoria.getValue().id();
        Long marcaId = marca.getValue() == null ? null : marca.getValue().id();
        Long unidadId = unidad.getValue() == null ? null : unidad.getValue().id();

        Long padreId = null;
        if (tipo.getValue() == TipoProducto.VARIANTE) {
            padreId = padreProducto.getValue() != null ? padreProducto.getValue().id()
                    : (existente == null ? null : existente.producto().productoPadreId());
        }

        Producto producto = new Producto(
                existente == null ? null : existente.producto().id(),
                empresaId,
                Fx.texto(codigo),
                Fx.texto(nombre),
                Fx.nuloSiVacio(descripcion.getText()),
                categoriaId, marcaId, unidadId,
                tipo.getValue(),
                padreId,
                decimal(Fx.texto(costo)),
                existente == null ? BigDecimal.ZERO : existente.producto().costoPromedio(),
                controlaLote.isSelected(), controlaSerie.isSelected(), esGranel.isSelected(),
                Fx.nuloSiVacio(Fx.texto(codigoPlu)), decimalSiHay(Fx.texto(pesoNeto)), decimalSiHay(Fx.texto(volumen)),
                Fx.nuloSiVacio(Fx.texto(ubicacion)),
                existente == null ? null : existente.producto().imagenPrincipalId(),
                activo.isSelected());

        List<PrecioProducto> listaPrecios = new ArrayList<>();
        precios.forEach((listaId, campo) -> {
            BigDecimal valor = decimalSiHay(Fx.texto(campo));
            listaPrecios.add(new PrecioProducto(null, listaId, null, 0, valor, null, LocalDate.now(), null));
        });

        List<ComponenteKit> listaComponentes = tipo.getValue() == TipoProducto.KIT ? new ArrayList<>(componentes) : List.of();

        ProductoCompleto completo = new ProductoCompleto(
                producto,
                new ArrayList<>(codigos),
                new ArrayList<>(empaques),
                idsSeleccionados(),
                new ArrayList<>(imagenes),
                valoresAtributos(),
                listaPrecios,
                listaComponentes);

        servicio.guardar(completo);
    }

    private java.util.Set<Long> idsSeleccionados() {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        impuestos.forEach((id, check) -> {
            if (check.isSelected()) {
                ids.add(id);
            }
        });
        return ids;
    }

    private Map<Long, String> valoresAtributos() {
        Map<Long, String> valores = new LinkedHashMap<>();
        atributosCampos.forEach((id, campo) -> {
            if (!Fx.texto(campo).isBlank()) {
                valores.put(id, Fx.texto(campo));
            }
        });
        return valores;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta-form");
        return l;
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal decimalSiHay(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String valor(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
