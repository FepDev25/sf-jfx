package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Comprobante electronico (documento SRI). */
public record Comprobante(
        Long id,
        long empresaId,
        long sucursalId,
        String tipo,
        String establecimiento,
        String puntoEmision,
        String secuencial,
        String numeroCompleto,
        String claveAcceso,
        LocalDate fechaEmision,
        String ambiente,
        String tipoEmision,
        String emisorRuc,
        String emisorRazonSocial,
        String emisorNombreComercial,
        String emisorDireccion,
        String obligadoContabilidad,
        String regimen,
        String receptorTipoId,
        String receptorNumeroId,
        String receptorRazonSocial,
        String receptorEmail,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal iva,
        BigDecimal ice,
        BigDecimal irbpnr,
        BigDecimal propina,
        BigDecimal total,
        String estado,
        Long ventaId,
        String ventaNumero,
        Long facturaProveedorId,
        String motivo,
        String xmlPath,
        String xmlFirmadoPath,
        String pdfPath,
        String comprobanteRelacionadoClave,
        List<ComprobanteDetalle> detalles,
        List<ComprobantePago> pagos) {

    public String numeroCompletoCalculado() {
        return establecimiento + "-" + puntoEmision + "-" + secuencial;
    }
}
