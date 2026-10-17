package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;

/** Cabecera de transferencia entre bodegas/sucursales. */
public record Transferencia(
        Long id,
        long empresaId,
        String numero,
        long sucursalOrigenId,
        long bodegaOrigenId,
        String bodegaOrigen,
        long sucursalDestinoId,
        long bodegaDestinoId,
        String bodegaDestino,
        String estado,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaRecepcion,
        String nota) {

    public boolean editable() {
        return "BORRADOR".equals(estado);
    }
}
