package ec.sfjfx.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    void cedulaValida() {
        assertTrue(Validaciones.identificacionEcuador("1710034065"));
    }

    @Test
    void rucValido() {
        assertTrue(Validaciones.identificacionEcuador("1790011678001"));
    }

    @Test
    void identificacionesInvalidas() {
        assertFalse(Validaciones.identificacionEcuador("1710034064"));
        assertFalse(Validaciones.identificacionEcuador("123"));
        assertFalse(Validaciones.identificacionEcuador("abcdefghij"));
        assertFalse(Validaciones.identificacionEcuador(null));
    }

    @Test
    void emails() {
        assertTrue(Validaciones.email("a@b.com"));
        assertTrue(Validaciones.email(null));
        assertFalse(Validaciones.email("sin-arroba"));
    }
}
