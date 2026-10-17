package ec.sfjfx.core.sri;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;

/** Genera el RIDE (representacion impresa) en PDF con iText y el codigo de barras de la clave de acceso. */
public final class RidePdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RidePdf() {
    }

    public static byte[] generar(Comprobante c) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
                 Document doc = new Document(pdf, PageSize.A4)) {
                doc.setFontSize(9);

                doc.add(new Paragraph(CatalogoSri.nombreTipo(CatalogoSri.codDoc(c.tipo())))
                        .setBold().setFontSize(13).setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph("RUC: " + nvl(c.emisorRuc())).setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph(nvl(c.emisorRazonSocial())).setBold().setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph("Direccion: " + nvl(c.emisorDireccion())).setTextAlignment(TextAlignment.CENTER));                doc.add(new Paragraph(" "));

                Table cabecera = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                cabecera.addCell(sinBorde("No.: " + c.establecimiento() + "-" + c.puntoEmision() + "-" + c.secuencial()
                        + "\nFecha emision: " + (c.fechaEmision() == null ? "" : c.fechaEmision().format(FECHA))
                        + "\nAmbiente: " + ("PRODUCCION".equals(c.ambiente()) ? "PRODUCCION" : "PRUEBAS")));
                cabecera.addCell(sinBorde("Razon social: " + nvl(c.receptorRazonSocial())
                        + "\nIdentificacion: " + nvl(c.receptorNumeroId())
                        + "\nEmail: " + nvl(c.receptorEmail())));
                doc.add(cabecera);
                doc.add(new Paragraph(" "));

                Table tabla = new Table(UnitValue.createPercentArray(new float[]{1.2f, 3f, 1f, 1.3f, 1f, 1.3f}))
                        .useAllAvailableWidth();
                encabezado(tabla, "Codigo");
                encabezado(tabla, "Descripcion");
                encabezado(tabla, "Cant.");
                encabezado(tabla, "P. unitario");
                encabezado(tabla, "Desc.");
                encabezado(tabla, "Total");
                for (ComprobanteDetalle d : c.detalles()) {
                    celda(tabla, nvl(d.codigoPrincipal()));
                    celda(tabla, nvl(d.descripcion()));
                    celda(tabla, num(d.cantidad()));
                    celda(tabla, num(d.precioUnitario()));
                    celda(tabla, num(d.descuento()));
                    celda(tabla, num(d.total()));
                }
                doc.add(tabla);
                doc.add(new Paragraph(" "));

                Table totales = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
                totales.addCell(sinBorde("SUBTOTAL"));
                totales.addCell(sinBordeDerecha(num(c.subtotal())));
                totales.addCell(sinBorde("DESCUENTO"));
                totales.addCell(sinBordeDerecha(num(c.descuento())));
                totales.addCell(sinBorde("IVA"));
                totales.addCell(sinBordeDerecha(num(c.iva())));
                if (nz(c.propina()).signum() > 0) {
                    totales.addCell(sinBorde("PROPINA"));
                    totales.addCell(sinBordeDerecha(num(c.propina())));
                }
                totales.addCell(sinBorde("TOTAL").setBold());
                totales.addCell(sinBordeDerecha(num(c.total())).setBold());
                doc.add(totales);
                doc.add(new Paragraph(" "));

                byte[] barras = codigoBarras(c.claveAcceso());
                Image img = new Image(ImageDataFactory.create(barras));
                img.setAutoScale(true);
                doc.add(img);
                doc.add(new Paragraph("Clave de acceso: " + nvl(c.claveAcceso()))
                        .setFontSize(7).setTextAlignment(TextAlignment.CENTER));
                if ("ANULADO".equals(c.estado())) {
                    doc.add(new Paragraph("*** COMPROBANTE ANULADO ***").setBold()
                            .setTextAlignment(TextAlignment.CENTER));
                }
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el RIDE: " + e.getMessage(), e);
        }
    }

    private static byte[] codigoBarras(String contenido) throws Exception {
        java.util.Map<EncodeHintType, Object> pistas = new java.util.HashMap<>();
        pistas.put(EncodeHintType.MARGIN, 1);
        BitMatrix matriz = new MultiFormatWriter().encode(contenido == null ? "" : contenido,
                BarcodeFormat.CODE_128, 500, 90, pistas);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matriz, "PNG", out);
        return out.toByteArray();
    }

    private static void encabezado(Table t, String texto) {
        t.addHeaderCell(new Cell().add(new Paragraph(texto).setBold()).setBackgroundColor(
                com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setBorder(Border.NO_BORDER));
    }

    private static void celda(Table t, String texto) {
        t.addCell(new Cell().add(new Paragraph(texto).setFontSize(8)).setBorder(Border.NO_BORDER));
    }

    private static Cell sinBorde(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(8)).setBorder(Border.NO_BORDER);
    }

    private static Cell sinBordeDerecha(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(8).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER);
    }

    private static String num(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
