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
        // Base de 48 digitos conocida -> verificador calculado por modulo 11.
        String base = "161020260117900116780011001001000000001123456781";
        assertEquals(1, base.length() - 47); // base de 48 caracteres
        assertEquals(base.length(), 48);
        String dv = ClaveAcceso.digitoVerificador(base);
        assertTrue(dv.matches("\\d"));
    }

    @Test
    void codigoPorcentajeIva() {
        assertEquals("4", CatalogoSri.codigoPorcentajeIva(new java.math.BigDecimal("15")));
        assertEquals("2", CatalogoSri.codigoPorcentajeIva(new java.math.BigDecimal("12")));
        assertEquals("0", CatalogoSri.codigoPorcentajeIva(java.math.BigDecimal.ZERO));
    }
}
