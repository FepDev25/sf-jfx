package ec.sfjfx.core.modelo;

import java.time.LocalDate;

/** Certificado de firma electronica (.p12). */
public record CertificadoFirma(
        Long id,
        long empresaId,
        String alias,
        String rutaP12,
        String passwordCifrada,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        boolean activo) {

    @Override
    public String toString() {
        return alias;
    }
}
