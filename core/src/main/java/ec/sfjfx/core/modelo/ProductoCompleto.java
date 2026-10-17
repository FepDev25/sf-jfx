package ec.sfjfx.core.modelo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Agregado completo de un producto (para carga y guardado transaccional). */
public record ProductoCompleto(
        Producto producto,
        List<CodigoBarra> codigos,
        List<EmpaqueProducto> empaques,
        Set<Long> impuestoIds,
        List<ImagenProducto> imagenes,
        Map<Long, String> atributos,
        List<PrecioProducto> precios,
        List<ComponenteKit> componentes) {
}
