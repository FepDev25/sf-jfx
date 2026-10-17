package ec.sfjfx.ui.vistas.ventas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioAutorizacion;
import ec.sfjfx.core.servicio.ServicioSesionCaja;
import ec.sfjfx.core.servicio.ServicioImpuesto;
import ec.sfjfx.core.servicio.ServicioCupon;
import ec.sfjfx.core.servicio.ServicioPromocion;
import ec.sfjfx.core.servicio.ServicioProducto;
import ec.sfjfx.core.servicio.ServicioVenta;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Modo Caja (POS): carrito, cobro con pago mixto, apartado y descuentos. */
public final class VistaPos extends BorderPane {

    private final ServicioVenta ventas = new ServicioVenta();
    private final ServicioSesionCaja servicioCaja = new ServicioSesionCaja();
    private final ServicioPromocion promociones = new ServicioPromocion();
    private final ServicioCupon cupones = new ServicioCupon();
    private final long empresaId = SesionActual.requerida().empresa().id();

    private final ObservableList<Producto> catalogo = FXCollections.observableArrayList();
    private final FilteredList<Producto> catalogoFiltrado = new FilteredList<>(catalogo, p -> true);
    private final ObservableList<LineaCarrito> carrito = FXCollections.observableArrayList();
    private final Map<Long, BigDecimal> precios = new HashMap<>();
    private final Map<Long, Long> impuestoPorProducto = new HashMap<>();
    private final Map<Long, BigDecimal> porcentajeImpuesto = new HashMap<>();

    private ListaPrecio lista;
    private Bodega bodegaVenta;
    private SesionCaja sesion;
    private Long ventaAparcadaId;
    private Long cuponId;
    private TableView<LineaCarrito> tablaCarrito;

    private final Label etiquetaSesion = new Label();
    private final TextField busqueda = new TextField();
    private final ListView<Producto> listaProductos = new ListView<>(catalogoFiltrado);
    private final TextField descuentoGlobal = Fx.campo("0");
    private final TextField propina = Fx.campo("0");
    private final Label etiquetaTotales = new Label("$ 0.00");
    private final Label etiquetaDetalle = new Label("Subtotal $0.00 · IVA $0.00");

    public VistaPos() {
        getStyleClass().add("contenido");
        cargarDatos();
        setTop(construirBarra());
        setCenter(construirCuerpo());
        configurarAtajos();
        recargar();
    }

    private void cargarDatos() {
        catalogo.setAll(new ServicioProducto().listar(null));
        lista = ventas.listaPrecioDefault().orElse(null);
        if (lista != null) {
            precios.putAll(ventas.mapaPrecios(lista.id()));
        }
        impuestoPorProducto.putAll(new ServicioProducto().listar(null).isEmpty()
                ? Map.of() : impuestoIva());
        for (Impuesto i : new ServicioImpuesto().listar()) {
            porcentajeImpuesto.put(i.id(), i.porcentaje());
        }
        bodegaVenta = Combos.bodegaVenta();
    }

    private Map<Long, Long> impuestoIva() {
        return new ec.sfjfx.core.repositorio.ProductoRepositorio().impuestoIvaPorProducto(empresaId);
    }

    private Region construirBarra() {
        etiquetaSesion.getStyleClass().add("texto-suave");
        busqueda.setPromptText("Escanee codigo de barras o busque por nombre... (F2)");
        busqueda.getStyleClass().add("campo");
        busqueda.setPrefWidth(340);
        busqueda.textProperty().addListener((o, a, b) -> catalogoFiltrado
                .setPredicate(p -> b == null || b.isBlank() || p.nombre().toLowerCase().contains(b.toLowerCase())
                        || p.codigo().toLowerCase().contains(b.toLowerCase())));
        busqueda.setOnAction(e -> agregarPrimero());

        Button abrirCaja = new Button("Abrir caja");
        abrirCaja.getStyleClass().add("boton-secundario");
        abrirCaja.setOnAction(e -> abrirCaja());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox barra = new HBox(12, etiquetaSesion, espacio, busqueda, abrirCaja);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(12, 16, 12, 16));
        return barra;
    }

    private Region construirCuerpo() {
        listaProductos.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setText(null);
                } else {
                    BigDecimal precio = precios.getOrDefault(p.id(), BigDecimal.ZERO);
                    setText(p.nombre() + "    $ " + precio.toPlainString());
                }
            }
        });
        listaProductos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && listaProductos.getSelectionModel().getSelectedItem() != null) {
                agregar(listaProductos.getSelectionModel().getSelectedItem());
                busqueda.clear();
                listaProductos.getSelectionModel().clearSelection();
            }
        });

        VBox panelCatalogo = new VBox(8, new Label("Catalogo"), listaProductos);
        panelCatalogo.setPadding(new Insets(10));
        VBox.setVgrow(listaProductos, Priority.ALWAYS);

        TableView<LineaCarrito> tabla = new TableView<>(carrito);
        this.tablaCarrito = tabla;
        TableColumn<LineaCarrito, String> cDesc = new TableColumn<>("Producto");
        cDesc.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().descripcion()));
        cDesc.setPrefWidth(200);
        TableColumn<LineaCarrito, String> cCant = new TableColumn<>("Cant.");
        cCant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad().stripTrailingZeros().toPlainString()));
        cCant.setPrefWidth(60);
        TableColumn<LineaCarrito, String> cPrecio = new TableColumn<>("Precio");
        cPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>("$ " + c.getValue().precioUnitario().toPlainString()));
        cPrecio.setPrefWidth(80);
        TableColumn<LineaCarrito, String> cDescuento = new TableColumn<>("Desc.");
        cDescuento.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().descuento().signum() > 0 ? "$ " + money(c.getValue().descuento()) : ""));
        cDescuento.setPrefWidth(70);
        TableColumn<LineaCarrito, String> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>("$ " + c.getValue().totalLinea().toPlainString()));
        cTotal.setPrefWidth(80);
        tabla.getColumns().addAll(cDesc, cCant, cPrecio, cDescuento, cTotal);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button mas = new Button("+");
        Button menos = new Button("-");
        Button quitar = new Button("Quitar");
        mas.getStyleClass().add("boton-secundario");
        menos.getStyleClass().add("boton-secundario");
        quitar.getStyleClass().add("boton-secundario");
        mas.setOnAction(e -> cambiarCantidad(tabla, BigDecimal.ONE));
        menos.setOnAction(e -> cambiarCantidad(tabla, BigDecimal.ONE.negate()));
        quitar.setOnAction(e -> {
            LineaCarrito l = tabla.getSelectionModel().getSelectedItem();
            if (l != null) {
                carrito.remove(l);
                actualizarTotales();
            }
        });
        Button descuentoLinea = new Button("Descuento linea");
        descuentoLinea.getStyleClass().add("boton-secundario");
        descuentoLinea.setOnAction(e -> descuentoLinea(tabla));
        HBox accionesLinea = new HBox(6, mas, menos, quitar, descuentoLinea);
        accionesLinea.setAlignment(Pos.CENTER_LEFT);

        VBox panelCarrito = new VBox(8, new Label("Carrito"), tabla, accionesLinea, constructorTotales());
        panelCarrito.setPadding(new Insets(10));
        VBox.setVgrow(panelCarrito, Priority.ALWAYS);

        SplitPane split = new SplitPane(panelCatalogo, panelCarrito);
        split.setDividerPositions(0.55);
        return split;
    }

    private Region constructorTotales() {
        etiquetaTotales.getStyleClass().add("titulo-vista");
        etiquetaDetalle.getStyleClass().add("texto-suave");

        HBox lineaDescuento = new HBox(8, new Label("Descuento global:"), descuentoGlobal);
        HBox lineaPropina = new HBox(8, new Label("Propina:"), propina);
        descuentoGlobal.setPrefWidth(90);
        propina.setPrefWidth(90);
        descuentoGlobal.textProperty().addListener((o, a, b) -> actualizarTotales());
        propina.textProperty().addListener((o, a, b) -> actualizarTotales());

        Button cobrar = new Button("Cobrar (F12)");
        cobrar.getStyleClass().add("boton-primario");
        cobrar.setOnAction(e -> cobrar());
        Button aparcar = new Button("Aparcar (F8)");
        aparcar.getStyleClass().add("boton-secundario");
        aparcar.setOnAction(e -> aparcar());
        Button recuperar = new Button("Recuperar");
        recuperar.getStyleClass().add("boton-secundario");
        recuperar.setOnAction(e -> recuperar());
        Button cancelar = new Button("Cancelar venta");
        cancelar.getStyleClass().add("boton-secundario");
        cancelar.setOnAction(e -> {
            carrito.clear();
            ventaAparcadaId = null;
            actualizarTotales();
        });
        Button rapida = new Button("Rapida (F9)");
        rapida.getStyleClass().add("boton-secundario");
        rapida.setOnAction(e -> ventaRapida());
        Button nueva = new Button("Nueva venta (F7)");
        nueva.getStyleClass().add("boton-secundario");
        nueva.setOnAction(e -> nuevaVentaEnParalelo());
        FlowPane botones = new FlowPane(8, 8, cobrar, rapida, aparcar, recuperar, nueva, cancelar);
        Button promos = new Button("Promociones");
        promos.getStyleClass().add("boton-secundario");
        promos.setOnAction(e -> aplicarPromociones());
        Button cupon = new Button("Cupon");
        cupon.getStyleClass().add("boton-secundario");
        cupon.setOnAction(e -> aplicarCupon());
        HBox extras = new HBox(8, promos, cupon);

        VBox caja = new VBox(6, lineaDescuento, lineaPropina, etiquetaDetalle, etiquetaTotales, botones, extras);
        caja.setPadding(new Insets(10, 0, 0, 0));
        return caja;
    }

    private void aplicarPromociones() {
        if (carrito.isEmpty()) {
            return;
        }
        Long sucursalId = SesionActual.requerida().sucursal() == null ? null
                : SesionActual.requerida().sucursal().id();
        int aplicadas = 0;
        for (LineaCarrito l : carrito) {
            Producto p = catalogo.stream().filter(x -> x.id().equals(l.productoId())).findFirst().orElse(null);
            if (p == null) {
                continue;
            }
            var oferta = promociones.evaluar(p.id(), p.categoriaId(), p.marcaId(), l.cantidad(),
                    l.precioUnitario(), sucursalId);
            if (oferta.isPresent()) {
                l.descuento(oferta.get().descuento());
                aplicadas++;
            } else {
                l.descuento(BigDecimal.ZERO);
            }
        }
        tablaCarrito.refresh();
        actualizarTotales();
        Alertas.info("Promociones", aplicadas + " linea(s) con oferta aplicada");
    }

    private void aplicarCupon() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Cupon");
        dialogo.setHeaderText("Ingrese el codigo del cupon");
        if (ventana() != null) {
            dialogo.initOwner(ventana());
        }
        dialogo.showAndWait().map(String::trim).filter(c -> !c.isBlank()).ifPresent(codigo -> {
            try {
                BigDecimal base = carrito.stream().map(LineaCarrito::totalLinea)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal descuento = cupones.validarYCalcular(codigo, base);
                descuentoGlobal.setText(descuento.toPlainString());
                cuponId = cupones.porCodigo(codigo).id();
                actualizarTotales();
                Alertas.info("Cupon", "Descuento aplicado: $ " + money(descuento));
            } catch (RuntimeException e) {
                Alertas.error("Cupon", e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ operaciones

    private void agregarPrimero() {
        if (catalogoFiltrado.isEmpty()) {
            return;
        }
        Producto exacto = catalogoFiltrado.stream()
                .filter(p -> p.codigo().equalsIgnoreCase(Fx.texto(busqueda)))
                .findFirst().orElse(catalogoFiltrado.get(0));
        agregar(exacto);
        busqueda.clear();
    }

    private void agregar(Producto p) {
        if (sesion == null || !sesion.abierta()) {
            Alertas.advertencia("Caja cerrada", "Abra la caja antes de vender.");
            return;
        }
        carrito.stream().filter(l -> l.productoId().equals(p.id())).findFirst().ifPresentOrElse(
                l -> l.cantidad(l.cantidad().add(BigDecimal.ONE)),
                () -> carrito.add(new LineaCarrito(p.id(), p.nombre(), BigDecimal.ONE,
                        precios.getOrDefault(p.id(), BigDecimal.ZERO), impuestoPorProducto.get(p.id()), BigDecimal.ZERO)));
        actualizarTotales();
    }

    private void cambiarCantidad(TableView<LineaCarrito> tabla, BigDecimal delta) {
        LineaCarrito l = tabla.getSelectionModel().getSelectedItem();
        if (l == null) {
            return;
        }
        BigDecimal nueva = l.cantidad().add(delta);
        if (nueva.signum() <= 0) {
            carrito.remove(l);
        } else {
            l.cantidad(nueva);
            tabla.refresh();
        }
        actualizarTotales();
    }

    private void actualizarTotales() {
        boolean incluye = lista != null && lista.incluyeIva();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (LineaCarrito l : carrito) {
            BigDecimal base = l.cantidad().multiply(l.precioUnitario()).subtract(l.descuento());
            BigDecimal rate = porcentaje(l.impuestoId());
            if (incluye) {
                BigDecimal imp = base.subtract(base.divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP));
                subtotal = subtotal.add(base.subtract(imp));
                iva = iva.add(imp);
                total = total.add(base);
            } else {
                BigDecimal imp = base.multiply(rate).setScale(4, RoundingMode.HALF_UP);
                subtotal = subtotal.add(base);
                iva = iva.add(imp);
                total = total.add(base.add(imp));
            }
        }
        total = total.subtract(decimal(Fx.texto(descuentoGlobal))).add(decimal(Fx.texto(propina)));
        etiquetaTotales.setText("$ " + money(total));
        etiquetaDetalle.setText("Subtotal $ " + money(subtotal) + "  ·  IVA $ " + money(iva));
    }

    private void cobrar() {
        if (sesion == null || !sesion.abierta()) {
            Alertas.advertencia("Caja cerrada", "Abra la caja antes de cobrar.");
            return;
        }
        if (carrito.isEmpty()) {
            Alertas.advertencia("Venta vacia", "Agregue productos al carrito.");
            return;
        }
        boolean incluye = lista != null && lista.incluyeIva();
        BigDecimal total = totalActual();
        List<VentaPago> pagos = dialogoPagos(total);
        if (pagos == null) {
            return;
        }
        List<VentaDetalle> lineas = lineasVenta();
        try {
            long id = ventas.cobrar(sesion, bodegaVenta.id(), ventaAparcadaId, null, null, incluye, lineas, pagos,
                    decimal(Fx.texto(descuentoGlobal)), decimal(Fx.texto(propina)), null);
            Venta v = ventas.porId(id);
            Alertas.info("Venta registrada", "Venta " + v.numero() + "\nTotal cobrado: $ " + money(total));
            if (cuponId != null) {
                cupones.usar(cuponId);
                cuponId = null;
            }
            limpiarVenta();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo cobrar", e.getMessage());
        }
    }

    private List<VentaPago> dialogoPagos(BigDecimal total) {
        ObservableList<VentaPago> pagos = FXCollections.observableArrayList();
        TableView<VentaPago> tabla = new TableView<>(pagos);
        tabla.setPrefHeight(140);
        TableColumn<VentaPago, String> cMedio = new TableColumn<>("Medio");
        cMedio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().medioPago()));
        cMedio.setPrefWidth(160);
        TableColumn<VentaPago, String> cMonto = new TableColumn<>("Monto");
        cMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>("$ " + c.getValue().monto().toPlainString()));
        cMonto.setPrefWidth(100);
        tabla.getColumns().addAll(cMedio, cMonto);

        Label etiquetaVuelto = new Label();
        ComboBox<MedioPagoRepositorio.MedioPago> medio = Combos.mediosPago();
        medio.setPrefWidth(160);
        TextField monto = Fx.campo(total.toPlainString());
        monto.setPrefWidth(100);
        Button agregar = new Button("Agregar pago");
        agregar.getStyleClass().add("boton-secundario");
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        agregar.setOnAction(e -> {
            if (medio.getValue() == null) {
                return;
            }
            pagos.add(new VentaPago(null, 0, medio.getValue().id(), medio.getValue().nombre(),
                    decimal(Fx.texto(monto)), null, BigDecimal.ZERO));
            actualizarVuelto(pagos, total, etiquetaVuelto);
        });
        quitar.setOnAction(e -> {
            pagos.remove(tabla.getSelectionModel().getSelectedItem());
            actualizarVuelto(pagos, total, etiquetaVuelto);
        });
        // pago por defecto con el total
        if (medio.getValue() != null) {
            pagos.add(new VentaPago(null, 0, medio.getValue().id(), medio.getValue().nombre(), total, null, BigDecimal.ZERO));
        }
        actualizarVuelto(pagos, total, etiquetaVuelto);
        HBox barra = new HBox(8, medio, monto, agregar, quitar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(8, new Label("Total a cobrar: $ " + money(total)), tabla, barra, etiquetaVuelto);
        caja.setPadding(new Insets(8));

        boolean[] confirmado = {false};
        new DialogoFormulario("Cobrar venta", ventana())
                .agregarAnchoCompleto(caja)
                .alGuardar(() -> {
                    BigDecimal suma = pagos.stream().map(VentaPago::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (suma.compareTo(total) < 0) {
                        throw new ec.sfjfx.core.util.ValidacionExcepcion("El pago es menor al total");
                    }
                    confirmado[0] = true;
                })
                .mostrar();
        return confirmado[0] ? new ArrayList<>(pagos) : null;
    }

    private void actualizarVuelto(List<VentaPago> pagos, BigDecimal total, Label etiqueta) {
        BigDecimal suma = pagos.stream().map(VentaPago::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        etiqueta.setText("Pagado: $ " + money(suma) + "   Vuelto: $ " + money(suma.subtract(total).max(BigDecimal.ZERO)));
    }

    private void descuentoLinea(TableView<LineaCarrito> tabla) {
        LineaCarrito linea = tabla.getSelectionModel().getSelectedItem();
        if (linea == null) {
            Alertas.advertencia("Descuento", "Seleccione una linea del carrito.");
            return;
        }
        TextField monto = Fx.campo("0");
        TextField usuario = Fx.campo("");
        PasswordField pin = Fx.clave();
        new DialogoFormulario("Descuento por linea", ventana())
                .agregar("Descuento ($)", monto)
                .agregar("Autoriza (usuario)", usuario)
                .agregar("PIN", pin)
                .alGuardar(() -> {
                    BigDecimal descuento = decimal(Fx.texto(monto));
                    if (descuento.signum() <= 0) {
                        throw new IllegalArgumentException("El descuento debe ser mayor a cero");
                    }
                    if (descuento.compareTo(linea.totalLinea()) > 0) {
                        throw new IllegalArgumentException("El descuento no puede superar el total de la linea");
                    }
                    new ServicioAutorizacion().autorizar(Fx.texto(usuario), Fx.texto(pin), "pos.editar");
                    linea.descuento(linea.descuento().add(descuento));
                    tabla.refresh();
                    actualizarTotales();
                })
                .mostrar();
    }

    private void ventaRapida() {
        if (sesion == null || !sesion.abierta() || bodegaVenta == null) {
            Alertas.advertencia("Caja cerrada", "Abra la caja antes de vender.");
            return;
        }
        if (carrito.size() != 1) {
            Alertas.advertencia("Venta rapida", "La venta rapida requiere exactamente un item en el carrito.");
            return;
        }
        BigDecimal total = totalActual();
        if (total.signum() <= 0) {
            return;
        }
        var medios = new MedioPagoRepositorio().listar();
        if (medios.isEmpty()) {
            Alertas.error("Venta rapida", "No hay medios de pago configurados.");
            return;
        }
        var efectivo = medios.stream().filter(m -> "EFECTIVO".equals(m.tipo())).findFirst().orElse(medios.get(0));
        List<VentaDetalle> lineas = lineasVenta();
        List<VentaPago> pagos = List.of(new VentaPago(null, 0, efectivo.id(), efectivo.nombre(),
                total, null, BigDecimal.ZERO));
        try {
            long id = ventas.cobrar(sesion, bodegaVenta.id(), ventaAparcadaId, null, "Venta rapida",
                    lista != null && lista.incluyeIva(), lineas, pagos,
                    decimal(Fx.texto(descuentoGlobal)), decimal(Fx.texto(propina)), null);
            if (cuponId != null) {
                cupones.usar(cuponId);
                cuponId = null;
            }
            Alertas.info("Venta rapida", "Venta " + ventas.porId(id).numero() + " cobrada en efectivo: $ " + money(total));
            limpiarVenta();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo cobrar", e.getMessage());
        }
    }

    private void nuevaVentaEnParalelo() {
        if (carrito.isEmpty()) {
            Alertas.info("Nueva venta", "El carrito ya esta vacio.");
            return;
        }
        if (!Alertas.confirmar("Nueva venta en paralelo",
                "Se aparcara la venta actual y se abrira un carrito nuevo. ¿Desea continuar?")) {
            return;
        }
        if (!aparcar()) {
            return;
        }
        limpiarVenta();
        Alertas.info("Nueva venta", "La venta anterior quedo aparcada. Use Recuperar para retomarla.");
    }

    private List<VentaDetalle> lineasVenta() {
        List<VentaDetalle> lineas = new ArrayList<>();
        for (LineaCarrito l : carrito) {
            lineas.add(new VentaDetalle(null, 0, l.productoId(), l.descripcion(), l.cantidad(), l.precioUnitario(),
                    l.descuento(), l.impuestoId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        return lineas;
    }

    private void limpiarVenta() {
        carrito.clear();
        ventaAparcadaId = null;
        descuentoGlobal.setText("0");
        propina.setText("0");
        actualizarTotales();
    }

    private boolean aparcar() {
        if (carrito.isEmpty()) {
            return false;
        }
        if (sesion == null || !sesion.abierta() || bodegaVenta == null) {
            Alertas.advertencia("Caja cerrada", "Abra la caja antes de aparcar ventas.");
            return false;
        }
        boolean incluye = lista != null && lista.incluyeIva();
        List<VentaDetalle> lineas = lineasVenta();
        try {
            ventas.apartar(sesion, bodegaVenta.id(), null, null, incluye, lineas, decimal(Fx.texto(descuentoGlobal)));
        } catch (RuntimeException e) {
            Alertas.error("No se pudo aparcar", e.getMessage());
            return false;
        }
        limpiarVenta();
        return true;
    }

    private void recuperar() {
        List<Venta> aparcadas = ventas.aparcadas(sesion.cajaId());
        if (aparcadas.isEmpty()) {
            Alertas.info("Sin ventas aparcadas", "No hay ventas aparcadas en esta caja.");
            return;
        }
        ComboBox<Venta> combo = new ComboBox<>();
        combo.getItems().setAll(aparcadas);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setValue(aparcadas.get(0));
        new DialogoFormulario("Recuperar venta aparcada", ventana())
                .agregar("Venta", combo)
                .alGuardar(() -> {
                    Venta v = combo.getValue();
                    carrito.clear();
                    precios.forEach((k, val) -> { });
                    for (VentaDetalle d : ventas.porId(v.id()).detalles()) {
                        carrito.add(new LineaCarrito(d.productoId(), d.descripcion(), d.cantidad(), d.precioUnitario(),
                                d.impuestoId(), d.descuento()));
                    }
                    ventaAparcadaId = v.id();
                    actualizarTotales();
                })
                .mostrar();
    }

    private void abrirCaja() {
        Caja caja = SesionActual.requerida().caja();
        if (caja == null) {
            Alertas.advertencia("Sin caja", "Seleccione una caja en la barra superior.");
            return;
        }
        TextField monto = Fx.campo("0");
        new DialogoFormulario("Apertura de caja", ventana())
                .agregar("Monto inicial", monto)
                .alGuardar(() -> {
                    servicioCaja.abrir(caja.id(), decimal(Fx.texto(monto)));
                    recargar();
                })
                .mostrar();
    }

    private void recargar() {
        Caja caja = SesionActual.requerida().caja();
        sesion = caja == null ? null : servicioCaja.sesionAbierta(caja.id());
        if (sesion == null) {
            etiquetaSesion.setText("Turno cerrado — abra la caja para vender");
        } else {
            etiquetaSesion.setText("Caja " + sesion.caja() + " · Turno abierto · Inicial $ "
                    + money(sesion.montoInicial()));
        }
    }

    private BigDecimal totalActual() {
        boolean incluye = lista != null && lista.incluyeIva();
        BigDecimal total = BigDecimal.ZERO;
        for (LineaCarrito l : carrito) {
            BigDecimal base = l.cantidad().multiply(l.precioUnitario()).subtract(l.descuento());
            BigDecimal rate = porcentaje(l.impuestoId());
            total = total.add(incluye ? base : base.add(base.multiply(rate)));
        }
        return total.subtract(decimal(Fx.texto(descuentoGlobal))).add(decimal(Fx.texto(propina)));
    }

    private BigDecimal porcentaje(Long impuestoId) {
        if (impuestoId == null) {
            return BigDecimal.ZERO;
        }
        return porcentajeImpuesto.getOrDefault(impuestoId, BigDecimal.ZERO)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
    }

    private void configurarAtajos() {
        sceneProperty().addListener((o, a, escena) -> {
            if (escena == null) {
                return;
            }
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), busqueda::requestFocus);
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F12), this::cobrar);
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F8), this::aparcar);
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F9), this::ventaRapida);
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F7), this::nuevaVentaEnParalelo);
            escena.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), () -> {
                if (escena.getWindow() instanceof javafx.stage.Stage escenario) {
                    escenario.setFullScreen(!escenario.isFullScreen());
                }
            });
        });
    }

    private BigDecimal decimal(String v) {
        try {
            return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String money(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private javafx.stage.Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
