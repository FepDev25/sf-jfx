package ec.sfjfx.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void escribeYLeeConComasYComillas() throws Exception {
        List<String[]> original = List.of(
                new String[]{"codigo", "nombre", "precio"},
                new String[]{"A1", "Arroz, premium", "5.00"},
                new String[]{"A2", "Con \"comillas\"", "3.50"});

        StringWriter escritor = new StringWriter();
        Csv.escribir(escritor, original);

        List<String[]> leido = Csv.leer(new StringReader(escritor.toString()));
        assertEquals(3, leido.size());
        assertEquals("Arroz, premium", leido.get(1)[1]);
        assertEquals("Con \"comillas\"", leido.get(2)[1]);
        assertEquals("3.50", leido.get(2)[2]);
    }
}
