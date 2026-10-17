package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cupon de descuento con codigo y control de uso. */
public record Cupon(
        Long id,
        long empresaId,
        String codigo,
        String tipo,
        BigDecimal valor,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        Integer usosMaximos,
        int usosActuales,
        boolean activo) {

    @Override
    public String toString() {
        return codigo;
    }
}
