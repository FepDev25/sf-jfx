package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Venta (documento comercial; el comprobante SRI se genera en el Hito 7). */
public record Venta(
        Long id,
        long empresaId,
        long sucursalId,
        long cajaId,
        Long sesionCajaId,
        String numero,
        Long clienteId,
        String clienteNombre,
        Long usuarioId,
        String tipo,
        String estado,
        LocalDateTime fecha,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal propina,
        BigDecimal total,
        BigDecimal costoTotal,
        BigDecimal utilidad,
        Long autorizadoPorId,
        String nota,
        List<VentaDetalle> detalles,
        List<VentaPago> pagos) {

    public boolean pagada() {
        return "PAGADA".equals(estado);
    }
}
