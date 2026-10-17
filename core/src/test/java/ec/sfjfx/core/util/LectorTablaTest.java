package ec.sfjfx.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorTablaTest {

    @TempDir
    Path temp;

    @Test
    void leeCsvConBom() throws Exception {
        Path archivo = temp.resolve("datos.csv");
        Files.writeString(archivo, "\uFEFFcodigo,nombre\nA1,Arroz\n");
        List<String[]> filas = LectorTabla.leer(archivo);
        assertEquals(2, filas.size());
        assertEquals("A1", filas.get(1)[0]);
    }

    @Test
    void leeXlsx() throws Exception {
        Path archivo = temp.resolve("datos.xlsx");
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Datos");
            Row cabecera = hoja.createRow(0);
            cabecera.createCell(0).setCellValue("codigo");
            cabecera.createCell(1).setCellValue("nombre");
            Row fila = hoja.createRow(1);
            fila.createCell(0).setCellValue("A1");
            fila.createCell(1).setCellValue("Arroz");
            try (var salida = Files.newOutputStream(archivo)) {
                libro.write(salida);
            }
        }
        List<String[]> filas = LectorTabla.leer(archivo);
        assertEquals(2, filas.size());
        assertEquals("Arroz", filas.get(1)[1]);
    }
}
