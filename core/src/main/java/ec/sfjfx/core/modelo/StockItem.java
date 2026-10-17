package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Fila de existencias (stock) para mostrar. */
public record StockItem(
        long productoId,
        String producto,
        String codigo,
        Long bodegaId,
        String bodega,
        Long loteId,
        String lote,
        BigDecimal cantidad,
        BigDecimal reservada,
        BigDecimal costoPromedio) {

    public BigDecimal disponible() {
        BigDecimal c = cantidad == null ? BigDecimal.ZERO : cantidad;
        BigDecimal r = reservada == null ? BigDecimal.ZERO : reservada;
        return c.subtract(r);
    }

    public BigDecimal valor() {
        return (cantidad == null ? BigDecimal.ZERO : cantidad)
                .multiply(costoPromedio == null ? BigDecimal.ZERO : costoPromedio);
    }
}
