package ec.sfjfx.core.servicio;

import java.util.EnumSet;
import java.util.Set;

import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.modulos.PerfilInstalacion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Aplica perfiles de instalacion (Supermercado, Minimercado, Tienda) sobre los feature flags. */
public final class ServicioPerfiles {

    public static final String CLAVE_PERFIL = "empresa.perfil";

    private final GestorModulos modulos = new GestorModulos();
    private final ServicioConfiguracion configuracion = new ServicioConfiguracion();

    public PerfilInstalacion perfilActual() {
        String valor = configuracion.valor(CLAVE_PERFIL, null);
        return valor == null || valor.isBlank() ? null : PerfilInstalacion.porEtiqueta(valor);
    }

    public Set<ModuloSistema> modulosDelPerfil(PerfilInstalacion perfil) {
        return EnumSet.copyOf(perfil.modulos());
    }

    public void aplicar(PerfilInstalacion perfil) {
        if (perfil == null) {
            throw new ValidacionExcepcion("Seleccione un perfil");
        }
        long empresaId = empresa();
        for (ModuloSistema modulo : ModuloSistema.values()) {
            modulos.ajustar(empresaId, modulo, perfil.incluye(modulo));
        }
        configuracion.guardar(CLAVE_PERFIL, perfil.etiqueta(), "TEXTO", "Empresa",
                "Perfil de instalacion aplicado");
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
