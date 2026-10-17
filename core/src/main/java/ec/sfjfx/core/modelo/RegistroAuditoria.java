package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;

/** Registro de auditoria (fila de la bitacora). */
public record RegistroAuditoria(
        long id,
        Long usuarioId,
        String usuarioNombre,
        String terminal,
        String modulo,
        String entidad,
        Long entidadId,
        String accion,
        String valoresAnteriores,
        String valoresNuevos,
        LocalDateTime creadoEn) {
}
