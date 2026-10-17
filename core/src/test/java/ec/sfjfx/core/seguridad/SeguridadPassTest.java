package ec.sfjfx.core.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeguridadPassTest {

    @Test
    void hashYVerificacionFuncionan() {
        String hash = SeguridadPass.hashear("admin");
        assertTrue(SeguridadPass.verificar("admin", hash));
        assertFalse(SeguridadPass.verificar("otra", hash));
    }

    @Test
    void dosHashesDeLaMismaClaveDifieren() {
        assertNotEquals(SeguridadPass.hashear("admin"), SeguridadPass.hashear("admin"));
    }

    @Test
    void hashMalformadoNoVerifica() {
        assertFalse(SeguridadPass.verificar("admin", "texto-plano"));
        assertFalse(SeguridadPass.verificar("admin", null));
        assertFalse(SeguridadPass.verificar(null, "x"));
    }

    @Test
    void contrasenaVaciaNoSeHashea() {
        assertThrows(IllegalArgumentException.class, () -> SeguridadPass.hashear(""));
    }
}
