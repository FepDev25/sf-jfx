package ec.sfjfx.core.sri;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;
import ec.sfjfx.core.modelo.ComprobantePago;

class RidePdfTest {

    @Test
    void generaPdfYXmlDeFactura() {
        Comprobante c = new Comprobante(1L, 1L, 1L, "FACTURA", "001", "001", "000000001", "001-001-000000001",
                ClaveAcceso.generar(LocalDate.now(), "01", "1790011678001", "1", "001", "001", "000000001", "1"),
                LocalDate.now(), "1", "1", "1790011678001", "Supermercado Demo S.A.", "Supermercado Demo",
                "Av. Principal", "SI", "GENERAL", "07", "9999999999", "CONSUMIDOR FINAL", null,
                new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("11.50"), "EMITIDO", null, null, null, null, null, null, null, null,
                List.of(new ComprobanteDetalle(null, 0, "P1", null, "Producto", new BigDecimal("2"),
                        new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("10.00"), new BigDecimal("15"),
                        new BigDecimal("1.50"), new BigDecimal("11.50"))),
                List.of(new ComprobantePago("01", new BigDecimal("11.50"))));

        byte[] pdf = RidePdf.generar(c);
        assertTrue(pdf.length > 500);
        assertTrue((pdf[0] & 0xFF) == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');

        String xml = ConstructorXml.factura(c);
        assertTrue(xml.contains("<factura"));
        assertTrue(xml.contains("<claveAcceso>" + c.claveAcceso() + "</claveAcceso>"));
        assertTrue(xml.contains("<importeTotal>11.50</importeTotal>"));
    }
}
