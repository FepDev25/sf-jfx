package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Factura (o documento) de proveedor para cuentas por pagar. */
public record FacturaProveedor(
        Long id,
        long empresaId,
        long proveedorId,
        String proveedorNombre,
        Long ordenCompraId,
        Long recepcionId,
        String tipoDocumento,
        String numero,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal iva,
        BigDecimal total,
        BigDecimal saldo,
        String estadoPago) {

    public boolean conSaldo() {
        return saldo != null && saldo.signum() > 0;
    }
}
