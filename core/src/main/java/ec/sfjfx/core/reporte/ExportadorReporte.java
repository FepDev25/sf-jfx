package ec.sfjfx.core.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import ec.sfjfx.core.util.Csv;

/** Exportacion de reportes a CSV, XLSX y PDF. */
public final class ExportadorReporte {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ExportadorReporte() {
    }

    public static byte[] csv(ResultadoReporte resultado) {
        List<String[]> filas = new ArrayList<>();
        filas.add(resultado.columnas().stream().map(ColumnaReporte::titulo).toArray(String[]::new));
        for (List<Object> fila : resultado.filas()) {
            filas.add(filaTexto(resultado.columnas(), fila));
        }
        String[] totales = filaTotales(resultado);
        if (totales != null) {
            filas.add(totales);
        }
        StringWriter escritor = new StringWriter();
        try {
            Csv.escribir(escritor, filas);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el CSV", e);
        }
        byte[] contenido = escritor.toString().getBytes(StandardCharsets.UTF_8);
        byte[] conBom = new byte[contenido.length + 3];
        conBom[0] = (byte) 0xEF;
        conBom[1] = (byte) 0xBB;
        conBom[2] = (byte) 0xBF;
        System.arraycopy(contenido, 0, conBom, 3, contenido.length);
        return conBom;
    }

    public static byte[] xlsx(ResultadoReporte resultado) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(nombreHoja(resultado.definicion().nombre()));
            CreationHelper helper = libro.getCreationHelper();

            CellStyle encabezado = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            encabezado.setFont(fuente);
            encabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setBorderBottom(BorderStyle.THIN);
            encabezado.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(helper.createDataFormat().getFormat("$#,##0.00"));
            CellStyle decimal = libro.createCellStyle();
            decimal.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));
            CellStyle entero = libro.createCellStyle();
            entero.setDataFormat(helper.createDataFormat().getFormat("#,##0"));
            CellStyle porcentaje = libro.createCellStyle();
            porcentaje.setDataFormat(helper.createDataFormat().getFormat("0.00\" %\""));
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle titulo = libro.createCellStyle();
            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            titulo.setFont(fuenteTitulo);

            Row filaTitulo = hoja.createRow(0);
            org.apache.poi.ss.usermodel.Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue(resultado.definicion().nombre());
            celdaTitulo.setCellStyle(titulo);
            Row filaFiltro = hoja.createRow(1);
            filaFiltro.createCell(0).setCellValue(resultado.filtro().descripcion());

            Row filaCabecera = hoja.createRow(3);
            for (int i = 0; i < resultado.columnas().size(); i++) {
                org.apache.poi.ss.usermodel.Cell celda = filaCabecera.createCell(i);
                celda.setCellValue(resultado.columnas().get(i).titulo());
                celda.setCellStyle(encabezado);
                hoja.setColumnWidth(i, Math.min(12000, Math.max(3200, resultado.columnas().get(i).titulo().length() * 320)));
            }

            int numeroFila = 4;
            for (List<Object> fila : resultado.filas()) {
                Row row = hoja.createRow(numeroFila++);
                for (int i = 0; i < resultado.columnas().size(); i++) {
                    Object valor = i < fila.size() ? fila.get(i) : null;
                    org.apache.poi.ss.usermodel.Cell celda = row.createCell(i);
                    escribirCelda(celda, valor, resultado.columnas().get(i).tipo(),
                            moneda, decimal, entero, porcentaje, fecha);
                }
            }

            if (resultado.totales().stream().anyMatch(t -> t != null)) {
                Row row = hoja.createRow(numeroFila);
                for (int i = 0; i < resultado.columnas().size(); i++) {
                    org.apache.poi.ss.usermodel.Cell celda = row.createCell(i);
                    if (i == 0) {
                        celda.setCellValue("TOTALES");
                    }
                    java.math.BigDecimal total = resultado.totales().get(i);
                    if (total != null) {
                        celda.setCellValue(total.doubleValue());
                        aplicarEstilo(celda, resultado.columnas().get(i).tipo(),
                                moneda, decimal, entero, porcentaje);
                    }
                }
            }

            hoja.createFreezePane(0, 4);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el XLSX", e);
        }
    }

    public static byte[] pdf(ResultadoReporte resultado) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
             Document doc = new Document(pdf, PageSize.A4.rotate())) {
            doc.setFontSize(8);
            doc.add(new Paragraph("sf-jfx · " + resultado.definicion().nombre())
                    .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(resultado.definicion().descripcion())
                    .setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(resultado.filtro().descripcion())
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));

            float[] anchos = new float[resultado.columnas().size()];
            for (int i = 0; i < anchos.length; i++) {
                anchos[i] = resultado.columnas().get(i).tipo().esNumerica() ? 0.9f : 1.6f;
            }
            Table tabla = new Table(UnitValue.createPercentArray(anchos)).useAllAvailableWidth();
            for (ColumnaReporte columna : resultado.columnas()) {
                tabla.addHeaderCell(new Cell().add(new Paragraph(columna.titulo()).setBold())
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
            }
            for (List<Object> fila : resultado.filas()) {
                for (int i = 0; i < resultado.columnas().size(); i++) {
                    Object valor = i < fila.size() ? fila.get(i) : null;
                    ColumnaReporte columna = resultado.columnas().get(i);
                    String texto = FormatoReporte.valor(valor, columna.tipo());
                    Paragraph parrafo = new Paragraph(texto);
                    if (columna.tipo().esNumerica()) {
                        parrafo.setTextAlignment(TextAlignment.RIGHT);
                    }
                    tabla.addCell(new Cell().add(parrafo));
                }
            }
            if (resultado.totales().stream().anyMatch(t -> t != null)) {
                for (int i = 0; i < resultado.columnas().size(); i++) {
                    ColumnaReporte columna = resultado.columnas().get(i);
                    String texto = i == 0 ? "TOTALES"
                            : FormatoReporte.valor(resultado.totales().get(i), columna.tipo());
                    Paragraph parrafo = new Paragraph(texto).setBold();
                    if (columna.tipo().esNumerica()) {
                        parrafo.setTextAlignment(TextAlignment.RIGHT);
                    }
                    tabla.addCell(new Cell().add(parrafo)
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                }
            }
            doc.add(tabla);
            doc.add(new Paragraph("Generado: " + java.time.LocalDateTime.now().format(FECHA))
                    .setFontSize(7).setTextAlignment(TextAlignment.RIGHT));
        }
        return salida.toByteArray();
    }

    private static void escribirCelda(org.apache.poi.ss.usermodel.Cell celda, Object valor, TipoColumna tipo, CellStyle moneda,
                                      CellStyle decimal, CellStyle entero, CellStyle porcentaje,
                                      CellStyle fecha) {
        if (valor == null) {
            return;
        }
        switch (tipo) {
            case TEXTO -> celda.setCellValue(valor.toString());
            case FECHA -> {
                LocalDate f = valor instanceof LocalDate ld ? ld
                        : valor instanceof java.sql.Date sd ? sd.toLocalDate() : null;
                if (f != null) {
                    celda.setCellValue(f);
                    celda.setCellStyle(fecha);
                }
            }
            case ENTERO, DECIMAL, MONEDA, PORCENTAJE -> {
                if (valor instanceof Number n) {
                    celda.setCellValue(n.doubleValue());
                    aplicarEstilo(celda, tipo, moneda, decimal, entero, porcentaje);
                }
            }
        }
    }

    private static void aplicarEstilo(org.apache.poi.ss.usermodel.Cell celda, TipoColumna tipo, CellStyle moneda,
                                      CellStyle decimal, CellStyle entero, CellStyle porcentaje) {
        switch (tipo) {
            case MONEDA -> celda.setCellStyle(moneda);
            case DECIMAL -> celda.setCellStyle(decimal);
            case ENTERO -> celda.setCellStyle(entero);
            case PORCENTAJE -> celda.setCellStyle(porcentaje);
            default -> {
            }
        }
    }

    private static String[] filaTexto(List<ColumnaReporte> columnas, List<Object> fila) {
        String[] texto = new String[columnas.size()];
        for (int i = 0; i < columnas.size(); i++) {
            Object valor = i < fila.size() ? fila.get(i) : null;
            texto[i] = FormatoReporte.valor(valor, columnas.get(i).tipo());
        }
        return texto;
    }

    private static String[] filaTotales(ResultadoReporte resultado) {
        if (resultado.totales().stream().noneMatch(t -> t != null)) {
            return null;
        }
        String[] fila = new String[resultado.columnas().size()];
        for (int i = 0; i < fila.length; i++) {
            if (i == 0) {
                fila[i] = "TOTALES";
            } else {
                fila[i] = FormatoReporte.valor(resultado.totales().get(i), resultado.columnas().get(i).tipo());
            }
        }
        return fila;
    }

    private static String nombreHoja(String nombre) {
        String limpio = nombre.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }
}
