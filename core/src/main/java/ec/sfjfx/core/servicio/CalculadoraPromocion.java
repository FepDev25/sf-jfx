package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ec.sfjfx.core.modelo.Promocion;

/** Calculo puro del descuento de una promocion sobre una linea. */
public final class CalculadoraPromocion {

    private CalculadoraPromocion() {
    }

    public static BigDecimal descuento(Promocion p, BigDecimal cantidad, BigDecimal precioUnitario) {
        if (p == null || cantidad == null || precioUnitario == null || cantidad.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = cantidad.multiply(precioUnitario);
        return switch (p.tipo()) {
            case "PORCENTAJE" -> base.multiply(nz(p.valor()))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case "MONTO" -> nz(p.valor()).min(base);
            case "PRECIO_FIJO" -> {
                BigDecimal diferencia = precioUnitario.subtract(nz(p.valor()));
                yield diferencia.signum() > 0 ? diferencia.multiply(cantidad) : BigDecimal.ZERO;
            }
            case "NXM" -> {
                int requerida = p.cantidadRequerida() == null ? 1 : p.cantidadRequerida();
                int pagada = p.cantidadPagada() == null ? requerida : p.cantidadPagada();
                int gratis = cantidad.intValue() / requerida * (requerida - pagada);
                yield precioUnitario.multiply(BigDecimal.valueOf(gratis));
            }
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
