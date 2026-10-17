package ec.sfjfx.core.modelo;

/** Parametro de configuracion (clave/valor) por empresa. */
public record Configuracion(
        Long id,
        long empresaId,
        String clave,
        String valor,
        String tipo,
        String grupo,
        String descripcion) {
}
