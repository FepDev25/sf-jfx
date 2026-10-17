package ec.sfjfx.ui.navegacion;

import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.ui.vistas.admin.VistaAuditoria;
import ec.sfjfx.ui.vistas.admin.VistaConfiguracion;
import ec.sfjfx.ui.vistas.admin.VistaSeguridad;
import ec.sfjfx.ui.vistas.catalogo.VistaCatalogo;
import ec.sfjfx.ui.vistas.catalogo.VistaListasPrecio;
import ec.sfjfx.ui.vistas.clientes.VistaClientesModulo;
import ec.sfjfx.ui.vistas.compras.VistaCompras;
import ec.sfjfx.ui.vistas.compras.VistaProveedores;
import ec.sfjfx.ui.vistas.facturacion.VistaFacturacion;
import ec.sfjfx.ui.vistas.inventario.VistaInventario;
import ec.sfjfx.ui.vistas.promociones.VistaFidelidad;
import ec.sfjfx.ui.vistas.promociones.VistaPromocionesModulo;
import ec.sfjfx.ui.vistas.ventas.VistaVentas;
import ec.sfjfx.ui.vistas.VistaReportes;

import javafx.scene.Node;

/** Mapea cada modulo del sistema a su vista. Devuelve {@code null} si aun no tiene vista. */
public final class RegistroVistas {

    private RegistroVistas() {
    }

    public static Node para(ModuloSistema modulo) {
        return switch (modulo) {
            case CONFIGURACION -> new VistaConfiguracion();
            case USUARIOS -> new VistaSeguridad();
            case AUDITORIA -> new VistaAuditoria();
            case PRODUCTOS -> new VistaCatalogo();
            case LISTAS_PRECIO -> new VistaListasPrecio();
            case INVENTARIO -> new VistaInventario();
            case POS -> new VistaVentas();
            case CLIENTES -> new VistaClientesModulo();
            case FACTURACION -> new VistaFacturacion();
            case FIDELIZACION -> new VistaFidelidad();
            case PROMOCIONES -> new VistaPromocionesModulo();
            case COMPRAS -> new VistaCompras();
            case PROVEEDORES -> new VistaProveedores();
            case REPORTES -> new VistaReportes();
            default -> null;
        };
    }
}
