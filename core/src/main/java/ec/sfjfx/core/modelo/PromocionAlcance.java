package ec.sfjfx.core.modelo;

/** Alcance de una promocion (producto, categoria, marca, sucursal o tipo de cliente). */
public record PromocionAlcance(
        Long id,
        long promocionId,
        String tipoAlcance,
        Long referenciaId) {
}
