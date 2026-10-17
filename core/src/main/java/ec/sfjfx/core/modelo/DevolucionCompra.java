package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Devolucion de mercaderia a proveedor. */
public record DevolucionCompra(
        Long id,
        long empresaId,
        long proveedorId,
        String proveedorNombre,
        Long facturaProveedorId,
        Long recepcionId,
        long bodegaId,
        String bodega,
        String numero,
        LocalDateTime fecha,
        String motivo,
        BigDecimal total,
        String estado,
        String nota,
        List<DevolucionCompraDetalle> detalles) {
}
