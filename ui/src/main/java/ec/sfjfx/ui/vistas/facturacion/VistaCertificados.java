package ec.sfjfx.ui.vistas.facturacion;

import ec.sfjfx.core.modelo.CertificadoFirma;
import ec.sfjfx.core.servicio.ServicioCertificado;
import ec.sfjfx.ui.componentes.DialogoFormulario;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Fx;
import ec.sfjfx.ui.util.Perm;

import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class VistaCertificados extends VistaLista<CertificadoFirma> {

    private final ServicioCertificado servicio = new ServicioCertificado();

    public VistaCertificados() {
        super("Certificados de firma (.p12)");
        columna("Alias", 200, CertificadoFirma::alias);
        columna("Archivo", 320, CertificadoFirma::rutaP12);
        columna("Vigente desde", 130, c -> c.fechaDesde() == null ? "-" : c.fechaDesde().toString());
        columna("Vigente hasta", 130, c -> c.fechaHasta() == null ? "-" : c.fechaHasta().toString());
        columna("Activo", 70, c -> Fx.siNo(c.activo()));
        buscarPor(CertificadoFirma::alias);
        proveedor(servicio::listar);

        if (Perm.tiene("facturacion.crear")) {
            accion("Cargar", true, v -> cargar());
            accion("Activar", false, v -> v.seleccion().ifPresent(this::activar));
            accion("Eliminar", false, v -> v.seleccion().ifPresent(this::eliminar));
        }
        refrescar();
    }

    private void cargar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar certificado .p12");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12", "*.p12", "*.pfx"));
        java.io.File archivo = chooser.showOpenDialog(ventana());
        if (archivo == null) {
            return;
        }
        TextField alias = Fx.campo(archivo.getName());
        TextField password = Fx.clave();
        new DialogoFormulario("Cargar certificado", ventana())
                .agregar("Alias", alias)
                .agregar("Contrasena", password)
                .alGuardar(() -> {
                    servicio.guardar(Fx.texto(alias), archivo.getAbsolutePath(), Fx.texto(password));
                    refrescar();
                })
                .mostrar();
    }

    private void activar(CertificadoFirma cf) {
        servicio.activar(cf.id());
        refrescar();
    }

    private void eliminar(CertificadoFirma cf) {
        try {
            servicio.eliminar(cf.id());
            refrescar();
        } catch (RuntimeException e) {
            Alertas.error("No se pudo eliminar", e.getMessage());
        }
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
