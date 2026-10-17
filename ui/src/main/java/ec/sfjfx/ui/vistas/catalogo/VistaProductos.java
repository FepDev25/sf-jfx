package ec.sfjfx.ui.vistas.catalogo;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.modelo.HistorialPrecio;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCatalogoCsv;
import ec.sfjfx.core.servicio.ServicioEtiquetas;
import ec.sfjfx.core.servicio.ServicioProducto;
import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class VistaProductos extends VistaLista<Producto> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioProducto servicio = new ServicioProducto();
    private final ServicioCatalogoCsv csv = new ServicioCatalogoCsv();
    private final ServicioEtiquetas etiquetas = new ServicioEtiquetas();

    public VistaProductos() {
        super("Productos");
        columna("Codigo", 130, Producto::codigo);
        columna("Nombre", 300, Producto::nombre);
        columna("Tipo", 150, p -> p.tipo().etiqueta());
        columna("Costo", 100, p -> p.costo() == null ? "" : FormatoReporte.moneda(p.costo()));
        columna("Costo PPP", 110, p -> p.costoPromedio() == null ? "" : FormatoReporte.moneda(p.costoPromedio()));
        columna("Activo", 70, p -> Fx.siNo(p.activo()));
        buscarPor(p -> p.codigo() + " " + p.nombre());
        proveedorPaginado(servicio::pagina);

        if (Perm.tiene("productos.crear")) {
            accion("Nuevo", true, v -> nuevoProducto());
        }
        if (Perm.tiene("productos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::editarProducto));
        }
        accion("Historial", false, v -> v.seleccion().ifPresent(this::verHistorial));
        accion("Etiquetas", false, v -> v.seleccion().ifPresent(this::etiquetas));
        accion("Importar CSV/Excel", false, v -> importar());
        accion("Exportar CSV", false, v -> exportar());
        onDobleClic(p -> {
            if (Perm.tiene("productos.editar")) {
                editarProducto(p);
            }
        });
        refrescar();
    }

    private void nuevoProducto() {
        if (new EditorProducto(null, ventana()).abrir()) {
            refrescar();
        }
    }

    private void editarProducto(Producto p) {
        ProductoCompleto completo = servicio.cargar(p.id());
        if (new EditorProducto(completo, ventana()).abrir()) {
            refrescar();
        }
    }

    private void verHistorial(Producto p) {
        List<HistorialPrecio> historial = servicio.historial(p.id());
        TableView<HistorialPrecio> tabla = new TableView<>();
        tabla.getItems().setAll(historial);
        TableColumn<HistorialPrecio, String> fecha = new TableColumn<>("Fecha");
        fecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().creadoEn() == null ? ""
                : c.getValue().creadoEn().format(FORMATO)));
        fecha.setPrefWidth(140);
        TableColumn<HistorialPrecio, String> tipo = new TableColumn<>("Tipo");
        tipo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().tipo()));
        tipo.setPrefWidth(90);
        TableColumn<HistorialPrecio, String> anterior = new TableColumn<>("Anterior");
        anterior.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().valorAnterior())));
        TableColumn<HistorialPrecio, String> nuevo = new TableColumn<>("Nuevo");
        nuevo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor(c.getValue().valorNuevo())));
        TableColumn<HistorialPrecio, String> usuario = new TableColumn<>("Usuario");
        usuario.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().usuarioNombre() == null ? "sistema" : c.getValue().usuarioNombre()));
        usuario.setPrefWidth(120);
        tabla.getColumns().addAll(fecha, tipo, anterior, nuevo, usuario);
        tabla.setPrefHeight(360);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Historial de precios y costos: " + p.nombre());
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        VBox caja = new VBox(8, tabla);
        caja.setPadding(new Insets(12));
        dialogo.getDialogPane().setContent(caja);
        dialogo.initOwner(ventana());
        dialogo.showAndWait();
    }

    private void etiquetas(Producto p) {
        TextInputDialog cantidad = new TextInputDialog("12");
        cantidad.setTitle("Etiquetas");
        cantidad.setHeaderText("Cuantas etiquetas de " + p.nombre() + "?");
        cantidad.initOwner(ventana());
        int copias = cantidad.showAndWait().map(t -> {
            try {
                return Math.max(1, Integer.parseInt(t.trim()));
            } catch (NumberFormatException e) {
                return 1;
            }
        }).orElse(0);
        if (copias == 0) {
            return;
        }
        File destino = guardarComo("PDF", "etiquetas.pdf");
        if (destino == null) {
            return;
        }
        try {
            ProductoCompleto completo = servicio.cargar(p.id());
            List<ProductoCompleto> lista = new ArrayList<>();
            for (int i = 0; i < copias; i++) {
                lista.add(completo);
            }
            etiquetas.generarPdfEtiquetas(lista, destino.toPath(), SesionActual.requerida().empresa().nombreParaMostrar());
            Alertas.info("Etiquetas", "PDF generado: " + destino.getAbsolutePath());
        } catch (Exception e) {
            Alertas.error("Etiquetas", "No se pudo generar: " + e.getMessage());
        }
    }

    private void importar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar catalogo CSV/Excel");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV o Excel", "*.csv", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File archivo = chooser.showOpenDialog(ventana());
        if (archivo == null) {
            return;
        }
        try {
            var resultado = csv.importar(archivo.toPath());
            String mensaje = "Creados: " + resultado.creados() + "\nOmitidos (ya existian): " + resultado.omitidos();
            if (!resultado.errores().isEmpty()) {
                mensaje += "\nErrores: " + resultado.errores().size() + "\n"
                        + String.join("\n", resultado.errores().subList(0, Math.min(10, resultado.errores().size())));
            }
            Alertas.info("Importacion", mensaje);
            refrescar();
        } catch (Exception e) {
            Alertas.error("Importacion", "No se pudo importar: " + e.getMessage());
        }
    }

    private void exportar() {
        File destino = guardarComo("CSV", "catalogo.csv");
        if (destino == null) {
            return;
        }
        try {
            csv.exportar(Path.of(destino.getAbsolutePath()));
            Alertas.info("Exportacion", "Archivo generado: " + destino.getAbsolutePath());
        } catch (Exception e) {
            Alertas.error("Exportacion", "No se pudo exportar: " + e.getMessage());
        }
    }

    private File guardarComo(String extension, String nombre) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar como");
        chooser.setInitialFileName(nombre);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension, "*." + extension.toLowerCase()));
        return chooser.showSaveDialog(ventana());
    }

    private String valor(java.math.BigDecimal v) {
        return v == null ? "-" : v.toPlainString();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
