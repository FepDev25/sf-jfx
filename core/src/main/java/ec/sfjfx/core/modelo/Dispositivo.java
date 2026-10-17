package ec.sfjfx.core.modelo;

/** Dispositivo configurado para una sucursal o caja (impresora, lector, balanza, gaveta). */
public record Dispositivo(
        Long id,
        long empresaId,
        Long sucursalId,
        Long cajaId,
        String tipo,
        String nombre,
        String configuracion,
        boolean activo) {

    public boolean esImpresora() {
        return "IMPRESORA_TICKET".equals(tipo) || "IMPRESORA_A4".equals(tipo);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
