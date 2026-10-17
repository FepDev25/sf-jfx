package ec.sfjfx.core.modelo;

/** Empresa (raiz de la organizacion). */
public record Empresa(
        Long id,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionMatriz,
        String telefono,
        String email,
        String logoPath,
        boolean contribuyenteEspecial,
        boolean obligadoContabilidad,
        String regimen,
        String moneda,
        boolean activo) {

    public String nombreParaMostrar() {
        return nombreComercial != null && !nombreComercial.isBlank() ? nombreComercial : razonSocial;
    }
}
