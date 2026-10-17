package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Empaque (caja, docena) con su factor de conversion a la unidad base. */
public record EmpaqueProducto(
        Long id,
        long productoId,
        String nombre,
        Long unidadMedidaId,
        BigDecimal factor,
        boolean esCompra,
        boolean esVenta,
        Long codigoBarraId,
        boolean activo) {
}
