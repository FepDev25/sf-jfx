package ec.sfjfx.core.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formato de valores de reportes (tabla, PDF y CSV). */
public final class FormatoReporte {

    private static final Locale LOCALE = Locale.forLanguageTag("es-EC");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatoReporte() {
    }

    public static String valor(Object valor, TipoColumna tipo) {
        if (valor == null) {
            return "";
        }
        return switch (tipo) {
            case TEXTO -> valor.toString();
            case FECHA -> valor instanceof LocalDate fecha ? fecha.format(FECHA)
                    : valor instanceof java.sql.Date fecha ? fecha.toLocalDate().format(FECHA)
                    : valor.toString();
            case ENTERO -> entero(valor);
            case DECIMAL -> decimal(valor);
            case MONEDA -> moneda(valor);
            case PORCENTAJE -> porcentaje(valor);
        };
    }

    public static String entero(Object valor) {
        return formato("#,##0").format(new BigDecimal(valor.toString()));
    }

    public static String decimal(Object valor) {
        return formato("#,##0.00").format(new BigDecimal(valor.toString()));
    }

    public static String moneda(Object valor) {
        return "$" + decimal(valor);
    }

    public static String porcentaje(Object valor) {
        return formato("#,##0.00").format(new BigDecimal(valor.toString())) + " %";
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static DecimalFormat formato(String patron) {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(LOCALE);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat(patron, simbolos);
    }
}
