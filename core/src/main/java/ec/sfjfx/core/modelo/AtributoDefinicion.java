package ec.sfjfx.core.modelo;

/** Definicion de un atributo personalizado (por empresa, opcionalmente por categoria). */
public record AtributoDefinicion(
        Long id,
        long empresaId,
        Long categoriaId,
        String codigo,
        String nombre,
        TipoDatoAtributo tipoDato,
        String opciones,
        boolean obligatorio,
        int orden,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
