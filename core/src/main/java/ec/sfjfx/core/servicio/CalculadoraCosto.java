package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculos de costeo de inventario. */
public final class CalculadoraCosto {

    private CalculadoraCosto() {
    }

    /**
     * Costo promedio ponderado (PPP) al ingresar mercaderia.
     *
     * @param totalAnterior existencias previas
     * @param promedio      costo promedio previo
     * @param entrada       cantidad que ingresa
     * @param costoEntrada  costo unitario de la entrada
     */
    public static BigDecimal promedioPonderado(BigDecimal totalAnterior, BigDecimal promedio,
                                                BigDecimal entrada, BigDecimal costoEntrada) {
        BigDecimal totalNuevo = nz(totalAnterior).add(nz(entrada));
        if (totalNuevo.signum() <= 0) {
            return nz(costoEntrada);
        }
        BigDecimal valorAnterior = nz(totalAnterior).multiply(nz(promedio));
        BigDecimal valorNuevo = nz(entrada).multiply(nz(costoEntrada));
        return valorAnterior.add(valorNuevo).divide(totalNuevo, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
