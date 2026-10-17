package ec.sfjfx.core.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Lee hojas tabulares desde CSV o Excel (XLSX/XLS). */
public final class LectorTabla {

    private LectorTabla() {
    }

    public static List<String[]> leer(Path archivo) throws IOException {
        String nombre = archivo.getFileName().toString().toLowerCase();
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return leerExcel(archivo);
        }
        try (Reader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return Csv.leer(reader);
        }
    }

    private static List<String[]> leerExcel(Path archivo) throws IOException {
        List<String[]> filas = new ArrayList<>();
        try (Workbook libro = WorkbookFactory.create(archivo.toFile())) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = libro.getCreationHelper().createFormulaEvaluator();
            for (Row row : hoja) {
                int ultima = Math.max(0, row.getLastCellNum());
                String[] fila = new String[ultima];
                for (int i = 0; i < ultima; i++) {
                    if (row.getCell(i) == null) {
                        fila[i] = "";
                    } else {
                        fila[i] = formatter.formatCellValue(row.getCell(i), evaluator).trim();
                    }
                }
                filas.add(fila);
            }
        } catch (RuntimeException e) {
            throw new IOException("No se pudo leer el archivo Excel", e);
        }
        return filas;
    }
}
