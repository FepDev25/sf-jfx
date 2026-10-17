package ec.sfjfx.core.sri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CatalogoSriTest {

    @Test
    void codigosDeComprobante() {
        assertEquals("01", CatalogoSri.codDoc("FACTURA"));
        assertEquals("03", CatalogoSri.codDoc("LIQUIDACION_COMPRA"));
        assertEquals("04", CatalogoSri.codDoc("NOTA_CREDITO"));
        assertEquals("05", CatalogoSri.codDoc("NOTA_DEBITO"));
        assertEquals("06", CatalogoSri.codDoc("GUIA_REMISION"));
        assertEquals("07", CatalogoSri.codDoc("RETENCION"));
        assertEquals("00", CatalogoSri.codDoc("OTRO"));
    }

    @Test
    void porcentajesDeIva() {
        assertEquals("0", CatalogoSri.codigoPorcentajeIva(BigDecimal.ZERO));
        assertEquals("5", CatalogoSri.codigoPorcentajeIva(new BigDecimal("5")));
        assertEquals("2", CatalogoSri.codigoPorcentajeIva(new BigDecimal("12.00")));
        assertEquals("3", CatalogoSri.codigoPorcentajeIva(new BigDecimal("14")));
        assertEquals("4", CatalogoSri.codigoPorcentajeIva(new BigDecimal("15")));
        assertEquals("0", CatalogoSri.codigoPorcentajeIva(null));
    }

    @Test
    void tiposDeIdentificacionYAmbiente() {
        assertEquals("04", CatalogoSri.tipoIdentificacion("RUC"));
        assertEquals("05", CatalogoSri.tipoIdentificacion("CEDULA"));
        assertEquals("06", CatalogoSri.tipoIdentificacion("PASAPORTE"));
        assertEquals("07", CatalogoSri.tipoIdentificacion("OTRO"));
        assertEquals("1", CatalogoSri.ambiente("PRUEBAS"));
        assertEquals("2", CatalogoSri.ambiente("PRODUCCION"));
        assertEquals("1", CatalogoSri.tipoEmision("NORMAL"));
        assertEquals("2", CatalogoSri.tipoEmision("CONTINGENCIA"));
    }
}
