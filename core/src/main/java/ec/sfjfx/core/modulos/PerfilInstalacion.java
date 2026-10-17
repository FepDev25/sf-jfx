package ec.sfjfx.core.modulos;

import java.util.EnumSet;
import java.util.Set;

/** Perfiles de instalacion que activan un conjunto de modulos. */
public enum PerfilInstalacion {

    SUPERMERCADO("Supermercado", EnumSet.allOf(ModuloSistema.class)),
    MINIMERCADO("Minimercado", EnumSet.of(
            ModuloSistema.POS,
            ModuloSistema.CLIENTES,
            ModuloSistema.PRODUCTOS,
            ModuloSistema.INVENTARIO,
            ModuloSistema.LISTAS_PRECIO,
            ModuloSistema.COMPRAS,
            ModuloSistema.PROVEEDORES,
            ModuloSistema.FACTURACION,
            ModuloSistema.REPORTES,
            ModuloSistema.USUARIOS,
            ModuloSistema.AUDITORIA,
            ModuloSistema.CONFIGURACION)),
    TIENDA("Tienda", EnumSet.of(
            ModuloSistema.POS,
            ModuloSistema.CLIENTES,
            ModuloSistema.PRODUCTOS,
            ModuloSistema.INVENTARIO,
            ModuloSistema.COMPRAS,
            ModuloSistema.PROVEEDORES,
            ModuloSistema.FACTURACION,
            ModuloSistema.REPORTES,
            ModuloSistema.USUARIOS,
            ModuloSistema.AUDITORIA,
            ModuloSistema.CONFIGURACION));

    private final String etiqueta;
    private final Set<ModuloSistema> modulos;

    PerfilInstalacion(String etiqueta, Set<ModuloSistema> modulos) {
        this.etiqueta = etiqueta;
        this.modulos = modulos;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public Set<ModuloSistema> modulos() {
        return modulos;
    }

    public boolean incluye(ModuloSistema modulo) {
        return modulos.contains(modulo);
    }

    public static PerfilInstalacion porEtiqueta(String etiqueta) {
        for (PerfilInstalacion p : values()) {
            if (p.etiqueta.equalsIgnoreCase(etiqueta) || p.name().equalsIgnoreCase(etiqueta)) {
                return p;
            }
        }
        return SUPERMERCADO;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
