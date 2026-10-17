package ec.sfjfx.core.modulos;

/**
 * Modulos del sistema que pueden activarse/desactivarse por empresa
 * (feature flags) y que se agrupan en el menu lateral.
 */
public enum ModuloSistema {

    POS("POS", "Ventas / POS", "Operacion"),
    CLIENTES("CLIENTES", "Clientes", "Operacion"),
    FIDELIZACION("FIDELIZACION", "Fidelizacion", "Operacion"),

    PRODUCTOS("PRODUCTOS", "Productos", "Catalogo e inventario"),
    INVENTARIO("INVENTARIO", "Inventario", "Catalogo e inventario"),
    BODEGAS("BODEGAS", "Bodegas", "Catalogo e inventario"),
    LISTAS_PRECIO("LISTAS_PRECIO", "Listas de precio", "Catalogo e inventario"),
    PROMOCIONES("PROMOCIONES", "Promociones", "Catalogo e inventario"),
    GRANEL("GRANEL", "Granel / PLU", "Catalogo e inventario"),

    COMPRAS("COMPRAS", "Compras", "Compras"),
    PROVEEDORES("PROVEEDORES", "Proveedores", "Compras"),

    FACTURACION("FACTURACION", "Facturacion", "Administracion"),
    REPORTES("REPORTES", "Reportes", "Administracion"),
    USUARIOS("USUARIOS", "Usuarios", "Administracion"),
    AUDITORIA("AUDITORIA", "Auditoria", "Administracion"),
    CONFIGURACION("CONFIGURACION", "Configuracion", "Administracion");

    private final String codigo;
    private final String etiqueta;
    private final String grupo;

    ModuloSistema(String codigo, String etiqueta, String grupo) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
        this.grupo = grupo;
    }

    public String codigo() {
        return codigo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String grupo() {
        return grupo;
    }

    /** Permiso minimo (VER) para que un usuario vea el modulo. */
    public String permisoVer() {
        return codigo.toLowerCase() + ".ver";
    }

    public static ModuloSistema porCodigo(String codigo) {
        for (ModuloSistema m : values()) {
            if (m.codigo.equalsIgnoreCase(codigo)) {
                return m;
            }
        }
        return null;
    }
}
