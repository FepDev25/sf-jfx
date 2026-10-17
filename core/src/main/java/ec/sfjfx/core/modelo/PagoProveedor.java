package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pago a proveedor (puede cubrir varias facturas). */
public record PagoProveedor(
        Long id,
        long empresaId,
        long proveedorId,
        String proveedorNombre,
        String numero,
        LocalDateTime fecha,
        BigDecimal total,
        Long medioPagoId,
        String referencia,
        String nota,
        List<PagoFactura> aplicaciones) {

    /** Aplicacion de un pago a una factura. */
    public record PagoFactura(long facturaProveedorId, String facturaNumero, BigDecimal monto) {
    }
}
