package ec.sfjfx.core.util;

/** Validaciones de datos de negocio (Ecuador). */
public final class Validaciones {

    private static final int[] PESOS = {2, 1, 2, 1, 2, 1, 2, 1, 2};

    private Validaciones() {
    }

    public static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionExcepcion("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    public static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Valida una cedula (10 digitos) o RUC (13) de Ecuador con el modulo 11. */
    public static boolean identificacionEcuador(String numero) {
        if (numero == null) {
            return false;
        }
        String n = numero.trim();
        if (!n.matches("\\d{10}|\\d{13}")) {
            return false;
        }
        int digitoVerificador = Character.getNumericValue(n.charAt(9));
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int producto = Character.getNumericValue(n.charAt(i)) * PESOS[i];
            suma += producto > 9 ? producto - 9 : producto;
        }
        int residuo = suma % 10;
        int esperado = residuo == 0 ? 0 : 10 - residuo;
        return esperado == digitoVerificador;
    }

    public static boolean email(String valor) {
        return valor == null || valor.isBlank()
                || valor.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
    }
}
