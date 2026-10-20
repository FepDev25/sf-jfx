package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;

/**
 * Metadatos de la instalacion actual.
 *
 * <p>Una instalacion es single-tenant: una base PostgreSQL por operador. El modo
 * {@code UNICA_EMPRESA} o {@code MULTIEMPRESA} describe si esa instalacion admite una o varias
 * empresas legales.
 */
public record Instalacion(
        Long id,
        String nombre,
        String modo,
        String versionEsquema,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    public static final String MODO_UNICA_EMPRESA = "UNICA_EMPRESA";
    public static final String MODO_MULTIEMPRESA = "MULTIEMPRESA";

    public boolean esMultiempresa() {
        return MODO_MULTIEMPRESA.equalsIgnoreCase(modo);
    }
}
