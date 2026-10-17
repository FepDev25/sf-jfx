package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Cliente (consumidor final, persona natural o empresa). */
public record Cliente(
        Long id,
        long empresaId,
        String tipo,
        String tipoIdentificacion,
        String numeroIdentificacion,
        String nombres,
        String apellidos,
        String razonSocial,
        String email,
        String telefono,
        String direccion,
        boolean esCredito,
        BigDecimal limiteCredito,
        int diasCredito,
        BigDecimal saldo,
        boolean activo) {

    public String nombreParaMostrar() {
        if (razonSocial != null && !razonSocial.isBlank()) {
            return razonSocial;
        }
        String n = nombres == null ? "" : nombres;
        String a = apellidos == null ? "" : apellidos;
        String completo = (n + " " + a).trim();
        return completo.isBlank() ? "Consumidor final" : completo;
    }

    @Override
    public String toString() {
        return nombreParaMostrar();
    }
}
