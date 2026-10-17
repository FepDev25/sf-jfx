package ec.sfjfx.core.sri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ClaveAccesoTest {

    @Test
    void genera49Digitos() {
        String clave = ClaveAcceso.generar(LocalDate.of(2026, 10, 16), "01", "1790011678001", "1", "001", "001",
                "000000001", "1");
        assertEquals(49, clave.length());
        assertTrue(clave.matches("\\d{49}"));
    }

    @Test
    void elUltimoDigitoEsElVerificador() {
        String clave = ClaveAcceso.generar(LocalDate.of(2026, 10, 16), "01", "1790011678001", "2", "001", "002",
                "000000042", "1");
        String esperado = ClaveAcceso.digitoVerificador(clave.substring(0, 48));
        assertEquals(esperado, clave.substring(48));
    }

    @Test
    void digitoVerificadorConocido() {
        String base = "161020260117900116780011001001000000001123456781";
        assertEquals(48, base.length());
        assertEquals("5", ClaveAcceso.digitoVerificador(base));
    }

    @Test
    void estructuraDeLaClave() {
        String clave = ClaveAcceso.generar(LocalDate.of(2026, 10, 16), "04", "1790011678001", "2", "001", "002",
                "000000042", "1");
        assertEquals("16102026", clave.substring(0, 8));
        assertEquals("04", clave.substring(8, 10));
        assertEquals("1790011678001", clave.substring(10, 23));
        assertEquals("2", clave.substring(23, 24));
        assertEquals("001", clave.substring(24, 27));
        assertEquals("002", clave.substring(27, 30));
        assertEquals("000000042", clave.substring(30, 39));
        assertEquals("1", clave.substring(47, 48));
    }

    @Test
    void codigoPorcentajeIva() {
        assertEquals("4", CatalogoSri.codigoPorcentajeIva(new java.math.BigDecimal("15")));
        assertEquals("2", CatalogoSri.codigoPorcentajeIva(new java.math.BigDecimal("12")));
        assertEquals("0", CatalogoSri.codigoPorcentajeIva(java.math.BigDecimal.ZERO));
    }
}
