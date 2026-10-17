package ec.sfjfx.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Lectura/escritura CSV simple (separador coma, comillas dobles, escape ""). */
public final class Csv {

    private Csv() {
    }

    public static List<String[]> leer(Reader reader) throws IOException {
        List<String[]> filas = new ArrayList<>();
        BufferedReader br = new BufferedReader(reader);
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;
        int ch;
        boolean filaConDatos = false;
        while ((ch = br.read()) != -1) {
            char c = (char) ch;
            if (enComillas) {
                if (c == '"') {
                    br.mark(1);
                    int siguiente = br.read();
                    if (siguiente == '"') {
                        actual.append('"');
                    } else {
                        enComillas = false;
                        if (siguiente != -1) {
                            br.reset();
                        }
                    }
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                enComillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
                filaConDatos = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r') {
                    br.mark(1);
                    if (br.read() != '\n') {
                        br.reset();
                    }
                }
                campos.add(actual.toString());
                actual.setLength(0);
                filas.add(campos.toArray(new String[0]));
                campos = new ArrayList<>();
                filaConDatos = false;
            } else {
                actual.append(c);
                filaConDatos = true;
            }
        }
        if (actual.length() > 0 || !campos.isEmpty() || filaConDatos) {
            campos.add(actual.toString());
            filas.add(campos.toArray(new String[0]));
        }
        return filas;
    }

    public static void escribir(Writer writer, List<String[]> filas) throws IOException {
        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) {
                    writer.write(',');
                }
                writer.write(escapar(fila[i]));
            }
            writer.write('\n');
        }
    }

    private static String escapar(String valor) {
        String v = valor == null ? "" : valor;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
