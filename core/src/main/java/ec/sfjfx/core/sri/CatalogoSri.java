package ec.sfjfx.core.sri;

import java.math.BigDecimal;

/** Catalogos del SRI (Ecuador). */
public final class CatalogoSri {

    private CatalogoSri() {
    }

    public static String codDoc(String tipo) {
        return switch (tipo) {
            case "FACTURA" -> "01";
            case "LIQUIDACION_COMPRA" -> "03";
            case "NOTA_CREDITO" -> "04";
            case "NOTA_DEBITO" -> "05";
            case "GUIA_REMISION" -> "06";
            case "RETENCION" -> "07";
            default -> "00";
        };
    }

    /** Codigo de porcentaje de IVA (tabla 16). */
    public static String codigoPorcentajeIva(BigDecimal porcentaje) {
        if (porcentaje == null) {
            return "0";
        }
        return switch (porcentaje.stripTrailingZeros().toPlainString()) {
            case "0" -> "0";
            case "5" -> "5";
            case "12" -> "2";
            case "14" -> "3";
            case "15" -> "4";
            default -> "0";
        };
    }

    public static String tipoIdentificacion(String tipoIdentificacion) {
        if (tipoIdentificacion == null) {
            return "07";
        }
        return switch (tipoIdentificacion) {
            case "RUC" -> "04";
            case "CEDULA" -> "05";
            case "PASAPORTE" -> "06";
            default -> "07";
        };
    }

    public static String ambiente(String ambiente) {
        return "PRODUCCION".equals(ambiente) ? "2" : "1";
    }

    public static String tipoEmision(String tipoEmision) {
        return "CONTINGENCIA".equals(tipoEmision) ? "2" : "1";
    }

    public static String nombreTipo(String codDoc) {
        return switch (codDoc) {
            case "01" -> "FACTURA";
            case "03" -> "LIQUIDACION DE COMPRA";
            case "04" -> "NOTA DE CREDITO";
            case "05" -> "NOTA DE DEBITO";
            case "06" -> "GUIA DE REMISION";
            case "07" -> "COMPROBANTE DE RETENCION";
            default -> codDoc;
        };
    }
}
