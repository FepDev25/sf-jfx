package ec.sfjfx.core.servicio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import ec.sfjfx.core.modelo.CodigoBarra;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoCodigoBarra;

/** Generacion de codigos de barras (ZXing) y etiquetas en PDF (iText). */
public final class ServicioEtiquetas {

    public byte[] png(String contenido, BarcodeFormat formato, int ancho, int alto) throws IOException {
        try {
            BitMatrix matriz = codificar(contenido, formato, ancho, alto);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException e) {
            throw new IOException("No se pudo generar el codigo de barras", e);
        }
    }

    private BitMatrix codificar(String contenido, BarcodeFormat formato, int ancho, int alto)
            throws WriterException {
        Map<EncodeHintType, Object> pistas = new HashMap<>();
        pistas.put(EncodeHintType.MARGIN, 1);
        try {
            return new MultiFormatWriter().encode(contenido, formato, ancho, alto, pistas);
        } catch (WriterException | IllegalArgumentException e) {
            // Si el formato estricto falla (p. ej. EAN-13 con digito verificador invalido), se usa Code128.
            if (formato != BarcodeFormat.CODE_128) {
                return new MultiFormatWriter().encode(contenido, BarcodeFormat.CODE_128, ancho, alto, pistas);
            }
            throw e;
        }
    }

    public BarcodeFormat formato(CodigoBarra cb) {
        if (cb == null) {
            return BarcodeFormat.CODE_128;
        }
        return switch (cb.tipo()) {
            case EAN13 -> BarcodeFormat.EAN_13;
            case UPC_A -> BarcodeFormat.UPC_A;
            case EAN8 -> BarcodeFormat.EAN_8;
            case DUN14, INTERNO, PLU -> BarcodeFormat.CODE_128;
        };
    }

    /** Genera un PDF de etiquetas (3 por fila) para los productos indicados. */
    public void generarPdfEtiquetas(List<ProductoCompleto> productos, Path destino, String empresa)
            throws IOException {
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(destino.toFile()));
             Document documento = new Document(pdf)) {
            Paragraph titulo = new Paragraph(empresa + " - Etiquetas")
                    .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(12);
            documento.add(titulo);

            Table tabla = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
            int enFila = 0;
            for (ProductoCompleto completo : productos) {
                tabla.addCell(celdaEtiqueta(completo));
                enFila++;
                if (enFila % 3 == 0) {
                    // fila completa
                }
            }
            documento.add(tabla);
        }
    }

    private Cell celdaEtiqueta(ProductoCompleto completo) throws IOException {
        Producto p = completo.producto();
        String contenido = codigoPrincipal(completo, p);
        BarcodeFormat formato = BarcodeFormat.CODE_128;
        CodigoBarra principal = completo.codigos().stream().filter(CodigoBarra::esPrincipal).findFirst()
                .orElse(completo.codigos().isEmpty() ? null : completo.codigos().get(0));
        if (principal != null) {
            formato = formato(principal);
        }

        Cell celda = new Cell().setPadding(6);
        celda.add(new Paragraph(p.nombre()).setFontSize(8).setBold());
        celda.add(new Paragraph(p.codigo()).setFontSize(7));
        celda.add(new Paragraph(precioPublico(completo)).setFontSize(11).setBold());
        try {
            byte[] imagen = png(contenido, formato, 260, 70);
            Image img = new Image(ImageDataFactory.create(imagen));
            img.setAutoScale(true);
            celda.add(img);
        } catch (IOException e) {
            celda.add(new Paragraph("(sin codigo)").setFontSize(6));
        }
        return celda;
    }

    private String codigoPrincipal(ProductoCompleto completo, Producto p) {
        return completo.codigos().stream().filter(CodigoBarra::esPrincipal).map(CodigoBarra::codigo)
                .findFirst().orElse(completo.codigos().isEmpty() ? p.codigo() : completo.codigos().get(0).codigo());
    }

    private String precioPublico(ProductoCompleto completo) {
        return completo.precios().stream()
                .filter(pr -> pr.listaPrecioNombre() != null && pr.listaPrecioNombre().toLowerCase().contains("public"))
                .map(PrecioProducto::precio).filter(java.util.Objects::nonNull)
                .map(java.math.BigDecimal::toPlainString).findFirst()
                .map(v -> "$ " + v).orElse("");
    }

    public boolean esTipoPlu(TipoCodigoBarra tipo) {
        return tipo == TipoCodigoBarra.PLU;
    }
}
