package ec.sfjfx.core.modelo;

import java.time.LocalDate;

/**
 * Licencia perpetua con mantenimiento anual.
 *
 * <p>El archivo se firma offline con la clave privada del proveedor. La aplicacion valida la
 * firma con una clave publica y comprueba que el {@code idInstalacion} coincida con el codigo
 * unico de la instalacion.
 */
public record Licencia(
        int versionLicencia,
        String idInstalacion,
        String cliente,
        String ruc,
        String edicion,
        int maxSucursales,
        int maxCajas,
        LocalDate emitidaEn,
        LocalDate mantenimientoHasta,
        String firma) {

    /** Carga util canonica que se firma; no incluye la firma. */
    public String payload() {
        return "versionLicencia=" + versionLicencia + "\n"
                + "idInstalacion=" + valor(idInstalacion) + "\n"
                + "cliente=" + valor(cliente) + "\n"
                + "ruc=" + valor(ruc) + "\n"
                + "edicion=" + valor(edicion) + "\n"
                + "maxSucursales=" + maxSucursales + "\n"
                + "maxCajas=" + maxCajas + "\n"
                + "emitidaEn=" + (emitidaEn == null ? "" : emitidaEn) + "\n"
                + "mantenimientoHasta=" + (mantenimientoHasta == null ? "" : mantenimientoHasta);
    }

    public boolean mantenimientoVigente() {
        return mantenimientoHasta != null && !mantenimientoHasta.isBefore(LocalDate.now());
    }

    private static String valor(String valor) {
        return valor == null ? "" : valor;
    }
}
