package ec.sfjfx.ui.vistas.catalogo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioCategoria;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

public final class VistaCategorias extends BorderPane {

    private final ServicioCategoria servicio = new ServicioCategoria();
    private final TreeView<Categoria> arbol = new TreeView<>();

    public VistaCategorias() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));

        Label titulo = new Label("Categorias");
        titulo.getStyleClass().add("titulo-seccion");

        Button nueva = new Button("Nueva categoria");
        nueva.getStyleClass().add("boton-primario");
        nueva.setOnAction(e -> abrirFormulario(null, null));

        Button sub = new Button("Nueva subcategoria");
        sub.getStyleClass().add("boton-secundario");
        sub.setOnAction(e -> {
            Categoria padre = seleccion();
            if (padre != null) {
                abrirFormulario(null, padre);
            }
        });

        Button editar = new Button("Editar");
        editar.getStyleClass().add("boton-secundario");
        editar.setOnAction(e -> {
            Categoria sel = seleccion();
            if (sel != null) {
                abrirFormulario(sel, null);
            }
        });

        boolean puedeEditar = Perm.tiene("productos.crear") || Perm.tiene("productos.editar");
        nueva.setDisable(!puedeEditar);
        sub.setDisable(!puedeEditar);
        editar.setDisable(!puedeEditar);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(8, titulo, espacio, nueva, sub, editar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(0, 0, 10, 0));

        arbol.setShowRoot(false);
        arbol.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(Categoria item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : item.nombre() + (item.activo() ? "" : "  (inactiva)"));
            }
        });
        arbol.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && seleccion() != null && Perm.tiene("productos.editar")) {
                abrirFormulario(seleccion(), null);
            }
        });

        setTop(barra);
        setCenter(arbol);
        refrescar();
    }

    private Categoria seleccion() {
        TreeItem<Categoria> item = arbol.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    private void refrescar() {
        arbol.setRoot(construir());
    }

    private TreeItem<Categoria> construir() {
        TreeItem<Categoria> raiz = new TreeItem<>();
        Map<Long, TreeItem<Categoria>> items = new HashMap<>();
        List<Categoria> categorias = servicio.listar();
        for (Categoria c : categorias) {
            items.put(c.id(), new TreeItem<>(c));
        }
        for (Categoria c : categorias) {
            TreeItem<Categoria> item = items.get(c.id());
            if (c.padreId() == null) {
                raiz.getChildren().add(item);
            } else {
                TreeItem<Categoria> padre = items.get(c.padreId());
                if (padre != null) {
                    padre.getChildren().add(item);
                } else {
                    raiz.getChildren().add(item);
                }
            }
        }
        return raiz;
    }

    private void abrirFormulario(Categoria original, Categoria padre) {
        boolean nuevo = original == null;
        long empresa = SesionActual.requerida().empresa().id();
        Categoria base = nuevo
                ? new Categoria(null, empresa, padre == null ? null : padre.id(), "", "", null, 0, true)
                : original;

        TextField codigo = Fx.campo(base.codigo());
        TextField nombre = Fx.campo(base.nombre());
        TextArea descripcion = new TextArea(base.descripcion() == null ? "" : base.descripcion());
        descripcion.getStyleClass().add("campo");
        descripcion.setPrefRowCount(3);
        TextField orden = Fx.campo(String.valueOf(base.orden()));
        CheckBox activo = Fx.check("Activo", base.activo());

        DialogoFormulario dialogo = new DialogoFormulario(
                nuevo ? (padre == null ? "Nueva categoria" : "Nueva subcategoria de " + padre.nombre())
                        : "Editar categoria", ventana());
        dialogo.agregar("Codigo", codigo)
                .agregar("Nombre", nombre)
                .agregar("Descripcion", descripcion)
                .agregar("Orden", orden)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    boolean esNueva = base.id() == null;
                    servicio.guardar(new Categoria(esNueva ? null : base.id(), base.empresaId(), base.padreId(),
                            Fx.nuloSiVacio(Fx.texto(codigo)), Fx.texto(nombre), Fx.nuloSiVacio(descripcion.getText()),
                            entero(Fx.texto(orden)), activo.isSelected()));
                    refrescar();
                })
                .mostrar();
    }

    private int entero(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
