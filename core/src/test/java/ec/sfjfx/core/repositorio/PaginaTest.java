package ec.sfjfx.core.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaginaTest {

    @Test
    void calculaPaginasYRangos() {
        Pagina<String> pagina = new Pagina<>(List.of("a", "b", "c"), 7, 2, 3);
        assertEquals(3, pagina.totalPaginas());
        assertTrue(pagina.hayAnterior());
        assertTrue(pagina.haySiguiente());
        assertEquals(4, pagina.desde());
        assertEquals(6, pagina.hasta());
    }

    @Test
    void ultimaPaginaNoTieneSiguiente() {
        Pagina<String> pagina = new Pagina<>(List.of("g"), 7, 3, 3);
        assertTrue(pagina.hayAnterior());
        assertFalse(pagina.haySiguiente());
        assertEquals(7, pagina.hasta());
    }

    @Test
    void sinResultados() {
        Pagina<String> pagina = new Pagina<>(List.of(), 0, 1, 50);
        assertEquals(1, pagina.totalPaginas());
        assertEquals(0, pagina.desde());
        assertEquals(0, pagina.hasta());
        assertFalse(pagina.hayAnterior());
        assertFalse(pagina.haySiguiente());
    }
}
