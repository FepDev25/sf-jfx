package ec.sfjfx.ui.vistas.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Dispositivo;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.servicio.ServicioDispositivo;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Opcion;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public final class VistaDispositivos extends VistaLista<Dispositivo> {

    private final ServicioDispositivo servicio = new ServicioDispositivo();
    private final SucursalRepositorio sucursales = new SucursalRepositorio();
    private final CajaRepositorio cajas = new CajaRepositorio();
    private final Map<Long, String> nombreSucursal = new HashMap<>();
    private final Map<Long, String> nombreCaja = new HashMap<>();

    public VistaDispositivos() {
        super("Dispositivos");
        columna("Nombre", 200, Dispositivo::nombre);
        columna("Tipo", 150, Dispositivo::tipo);
        columna("Sucursal", 150, d -> nombreSucursal.getOrDefault(d.sucursalId(), "Todas"));
        columna("Caja", 140, d -> nombreCaja.getOrDefault(d.cajaId(), "-"));
        columna("Configuracion", 260, Dispositivo::configuracion);
        columna("Activo", 70, d -> Fx.siNo(d.activo()));
        buscarPor(d -> d.nombre() + " " + d.tipo() + " " + nombreSucursal.getOrDefault(d.sucursalId(), ""));
        proveedor(servicio::listar);

        if (Perm.tiene("dispositivos.crear")) {
            accion("Nuevo", true, v -> abrirFormulario(null));
        }
        if (Perm.tiene("dispositivos.editar")) {
            accion("Editar", false, v -> v.seleccion().ifPresent(this::abrirFormulario));
        }
        if (Perm.tiene("dispositivos.eliminar")) {
            accion("Eliminar", false, v -> v.seleccion().ifPresent(this::eliminar));
        }
        onDobleClic(d -> {
            if (Perm.tiene("dispositivos.editar")) {
                abrirFormulario(d);
            }
        });
        recargar();
    }

    private void recargar() {
        long empresaId = SesionActual.requerida().empresa().id();
        nombreSucursal.clear();
        nombreCaja.clear();
        for (Sucursal s : sucursales.porEmpresa(empresaId)) {
            nombreSucursal.put(s.id(), s.nombre());
        }
        for (Caja c : cajas.porEmpresa(empresaId)) {
            nombreCaja.put(c.id(), c.nombre());
        }
        refrescar();
    }

    private void abrirFormulario(Dispositivo original) {
        boolean nuevo = original == null;
        Dispositivo base = nuevo
                ? new Dispositivo(null, empresa(), null, null, "IMPRESORA_TICKET", "", "{}", true)
                : original;

        long empresaId = empresa();
        List<Sucursal> disponibles = sucursales.porEmpresa(empresaId);
        List<Caja> cajasDisponibles = cajas.porEmpresa(empresaId);

        ComboBox<String> tipo = Fx.combo(ServicioDispositivo.TIPOS.toArray(String[]::new));
        tipo.setValue(base.tipo());

        TextField nombre = Fx.campo(base.nombre());

        ComboBox<Opcion<Sucursal>> sucursal = new ComboBox<>();
        sucursal.getStyleClass().add("selector");
        sucursal.setMaxWidth(Double.MAX_VALUE);
        sucursal.getItems().add(new Opcion<>("Todas", null));
        for (Sucursal s : disponibles) {
            sucursal.getItems().add(new Opcion<>(s.nombre(), s));
        }
        seleccionar(sucursal, base.sucursalId());

        ComboBox<Opcion<Caja>> caja = new ComboBox<>();
        caja.getStyleClass().add("selector");
        caja.setMaxWidth(Double.MAX_VALUE);
        Runnable cargarCajas = () -> {
            Opcion<Caja> actual = caja.getValue();
            caja.getItems().clear();
            caja.getItems().add(new Opcion<>("Sin caja", null));
            Sucursal seleccionada = sucursal.getValue() == null ? null : sucursal.getValue().valor();
            for (Caja c : cajasDisponibles) {
                if (seleccionada == null || c.sucursalId() == seleccionada.id()) {
                    caja.getItems().add(new Opcion<>(c.nombre(), c));
                }
            }
            if (actual != null) {
                caja.getItems().stream().filter(o -> igual(o.valor() == null ? null : o.valor().id(),
                        actual.valor() == null ? null : actual.valor().id())).findFirst()
                        .ifPresentOrElse(caja::setValue, () -> caja.getSelectionModel().selectFirst());
            } else {
                caja.getItems().stream().filter(o -> o.valor() != null
                        && igual(o.valor().id(), base.cajaId())).findFirst()
                        .ifPresentOrElse(caja::setValue, () -> caja.getSelectionModel().selectFirst());
            }
        };
        cargarCajas.run();
        sucursal.valueProperty().addListener((o, a, b) -> cargarCajas.run());

        TextArea configuracion = new TextArea(base.configuracion() == null ? "{}" : base.configuracion());
        configuracion.getStyleClass().add("campo");
        configuracion.setPrefRowCount(7);
        configuracion.setWrapText(false);
        tipo.valueProperty().addListener((o, anterior, nuevoTipo) -> {
            String texto = configuracion.getText();
            if (texto == null || texto.isBlank() || "{}".equals(texto.trim())
                    || texto.equals(servicio.configuracionSugerida(anterior))) {
                configuracion.setText(servicio.configuracionSugerida(nuevoTipo));
            }
        });
        CheckBox activo = Fx.check("Activo", base.activo());

        new DialogoFormulario(nuevo ? "Nuevo dispositivo" : "Editar dispositivo", ventana())
                .agregar("Tipo", tipo)
                .agregar("Nombre", nombre)
                .agregar("Sucursal", sucursal)
                .agregar("Caja", caja)
                .agregar("Configuracion JSON", configuracion)
                .agregarAnchoCompleto(activo)
                .alGuardar(() -> {
                    Sucursal s = sucursal.getValue() == null ? null : sucursal.getValue().valor();
                    Caja c = caja.getValue() == null ? null : caja.getValue().valor();
                    servicio.guardar(new Dispositivo(base.id(), empresa(),
                            s == null ? null : s.id(), c == null ? null : c.id(),
                            tipo.getValue(), Fx.texto(nombre), configuracion.getText(), activo.isSelected()));
                    recargar();
                })
                .mostrar();
    }

    private void eliminar(Dispositivo d) {
        if (!Alertas.confirmar("Eliminar dispositivo", "¿Eliminar \"" + d.nombre() + "\"?")) {
            return;
        }
        try {
            servicio.eliminar(d.id());
            recargar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo eliminar", e.getMessage());
        }
    }

    private static <T> void seleccionar(ComboBox<Opcion<T>> combo, Long id) {
        for (Opcion<T> opcion : combo.getItems()) {
            T valor = opcion.valor();
            Long valorId = valor instanceof Sucursal s ? s.id() : valor instanceof Caja c ? c.id() : null;
            if (igual(valorId, id)) {
                combo.setValue(opcion);
                return;
            }
        }
        combo.getSelectionModel().selectFirst();
    }

    private static boolean igual(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
