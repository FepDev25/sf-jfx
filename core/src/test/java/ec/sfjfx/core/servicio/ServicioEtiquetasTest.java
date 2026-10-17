package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;

class ServicioEtiquetasTest {

    @Test
    void generaPngDeCodigoDeBarras() throws Exception {
        byte[] png = new ServicioEtiquetas().png("7861000112233", BarcodeFormat.CODE_128, 260, 80);
        assertTrue(png.length > 100, "Debe producir bytes PNG");
        // Firma PNG
        assertTrue((png[0] & 0xFF) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G');
    }

    @Test
    void formatoEanFallbackACode128() throws Exception {
        // Un EAN-13 con digito verificador invalido debe caer a Code128 sin fallar.
        byte[] png = new ServicioEtiquetas().png("1234567890123", BarcodeFormat.EAN_13, 200, 80);
        assertTrue(png.length > 100);
    }
}
