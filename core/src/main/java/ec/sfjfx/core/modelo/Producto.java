package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Producto. */
public record Producto(
        Long id,
        long empresaId,
        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        Long marcaId,
        Long unidadMedidaId,
        TipoProducto tipo,
        Long productoPadreId,
        BigDecimal costo,
        BigDecimal costoPromedio,
        boolean controlaLote,
        boolean controlaSerie,
        boolean esGranel,
        String codigoPlu,
        BigDecimal pesoNeto,
        BigDecimal volumen,
        String ubicacion,
        Long imagenPrincipalId,
        boolean activo) {

    public String nombreParaMostrar() {
        return nombre;
    }
}
