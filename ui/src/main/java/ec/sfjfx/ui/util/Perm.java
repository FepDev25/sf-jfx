package ec.sfjfx.ui.util;

import ec.sfjfx.core.seguridad.SesionActual;

/** Atajos de chequeo de permisos para la UI. */
public final class Perm {

    private Perm() {
    }

    public static boolean tiene(String codigo) {
        return SesionActual.requerida().tienePermiso(codigo);
    }
}
