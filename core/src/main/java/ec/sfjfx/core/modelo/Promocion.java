package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Promocion (porcentaje, monto, NxM, precio fijo o combo). */
public record Promocion(
        Long id,
        long empresaId,
        String nombre,
        String descripcion,
        String tipo,
        BigDecimal valor,
        Integer cantidadRequerida,
        Integer cantidadPagada,
        int prioridad,
        boolean combinable,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String estado,
        boolean activo,
        List<PromocionAlcance> alcances,
        List<PromocionDetalle> componentes) {

    @Override
    public String toString() {
        return nombre;
    }
}
