package ec.sfjfx.core.modelo;

/** Sucursal de una empresa. */
public record Sucursal(
        Long id,
        long empresaId,
        String codigo,
        String nombre,
        String direccion,
        String telefono,
        boolean esMatriz,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
