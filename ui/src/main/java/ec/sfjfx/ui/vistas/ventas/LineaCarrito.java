package ec.sfjfx.ui.vistas.ventas;

import java.math.BigDecimal;

/** Linea del carrito de venta (mutable mientras se arma la venta). */
public final class LineaCarrito {

    private final Long productoId;
    private final String descripcion;
    private BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final Long impuestoId;
    private BigDecimal descuento;

    public LineaCarrito(Long productoId, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario,
                        Long impuestoId, BigDecimal descuento) {
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.impuestoId = impuestoId;
        this.descuento = descuento;
    }

    public Long productoId() {
        return productoId;
    }

    public String descripcion() {
        return descripcion;
    }

    public BigDecimal cantidad() {
        return cantidad;
    }

    public void cantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal precioUnitario() {
        return precioUnitario;
    }

    public Long impuestoId() {
        return impuestoId;
    }

    public BigDecimal descuento() {
        return descuento == null ? BigDecimal.ZERO : descuento;
    }

    public void descuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal totalLinea() {
        return cantidad.multiply(precioUnitario).subtract(descuento());
    }
}
