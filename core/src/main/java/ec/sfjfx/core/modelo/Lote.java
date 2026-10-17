package ec.sfjfx.core.modelo;

import java.time.LocalDate;

/** Lote de un producto (opcional segun flag controlaLote). */
public record Lote(
        Long id,
        long productoId,
        String productoNombre,
        String codigo,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        boolean activo) {

    @Override
    public String toString() {
        return codigo;
    }
}
