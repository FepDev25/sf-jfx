package ec.sfjfx.core.sri;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/** Genera la clave de acceso del SRI (49 digitos) con su digito verificador (modulo 11). */
public final class ClaveAcceso {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final Random RANDOM = new Random();

    private ClaveAcceso() {
    }

    /**
     * @param fecha        fecha de emision
     * @param codDoc       tipo de comprobante (01, 04, ...)
     * @param ruc          13 digitos
     * @param ambiente     "1" pruebas, "2" produccion
     * @param estab        3 digitos
     * @param ptoEmi       3 digitos
     * @param secuencial   9 digitos
     * @param tipoEmision  "1" normal, "2" contingencia
     */
    public static String generar(LocalDate fecha, String codDoc, String ruc, String ambiente, String estab,
                                 String ptoEmi, String secuencial, String tipoEmision) {
        String codigoNumerico = String.format("%08d", RANDOM.nextInt(100_000_000));
        String base = fecha.format(FECHA)
                + codDoc
                + ruc
                + ambiente
                + estab + ptoEmi
                + secuencial
                + codigoNumerico
                + tipoEmision;
        return base + digitoVerificador(base);
    }

    /** Digito verificador modulo 11 (pesos 2..7 ciclicos de derecha a izquierda). */
    public static String digitoVerificador(String base48) {
        int[] pesos = {2, 3, 4, 5, 6, 7};
        int suma = 0;
        int indice = 0;
        for (int i = base48.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(base48.charAt(i)) * pesos[indice % 6];
            indice++;
        }
        int residuo = suma % 11;
        int digito = 11 - residuo;
        if (digito == 11) {
            digito = 0;
        } else if (digito == 10) {
            digito = 1;
        }
        return String.valueOf(digito);
    }
}
