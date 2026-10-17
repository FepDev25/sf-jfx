package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Relacion producto-proveedor (multi-proveedor con historico de costos). */
public record ProductoProveedor(
        Long id,
        long productoId,
        String productoNombre,
        long proveedorId,
        String proveedorNombre,
        String codigoProveedor,
        BigDecimal costoUltimo,
        Integer plazoDias,
        boolean esPreferido) {
}
