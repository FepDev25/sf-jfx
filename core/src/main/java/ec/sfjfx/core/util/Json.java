package ec.sfjfx.core.util;

import java.util.Map;

/** Construccion minima de JSON (solo lo necesario para auditoria). */
public final class Json {

    private Json() {
    }

    public static String deMapa(Map<String, ?> mapa) {
        if (mapa == null || mapa.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, ?> e : mapa.entrySet()) {
            if (!primero) {
                sb.append(',');
            }
            primero = false;
            sb.append('"').append(escapar(e.getKey())).append("\":").append(valor(e.getValue()));
        }
        return sb.append('}').toString();
    }

    private static String valor(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return "\"" + escapar(v.toString()) + "\"";
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
