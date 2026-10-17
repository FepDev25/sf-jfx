package ec.sfjfx.ui.util;

/** Opcion de combo con etiqueta visible y valor asociado (puede ser nulo = "todos"). */
public record Opcion<T>(String etiqueta, T valor) {

    @Override
    public String toString() {
        return etiqueta;
    }
}
