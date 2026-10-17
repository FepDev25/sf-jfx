package ec.sfjfx.core.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.repositorio.ModuloRepositorio;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.servicio.ServicioAuditoria;

/** Resuelve que modulos estan activos (feature flags) y visibles para un usuario. */
public final class GestorModulos {

    private final ModuloRepositorio repo;

    public GestorModulos(ModuloRepositorio repo) {
        this.repo = repo;
    }

    public GestorModulos() {
        this(new ModuloRepositorio());
    }

    public Set<String> codigosActivos(long empresaId) {
        return repo.activosPorEmpresa(empresaId);
    }

    public boolean activo(long empresaId, ModuloSistema modulo) {
        return repo.activosPorEmpresa(empresaId).contains(modulo.codigo());
    }

    /**
     * Modulos activos para la empresa que el usuario tiene permiso de ver,
     * en el orden de declaracion del enum.
     */
    public List<ModuloSistema> visibles(Sesion sesion, long empresaId) {
        Set<String> activos = repo.activosPorEmpresa(empresaId);
        List<ModuloSistema> resultado = new ArrayList<>();
        for (ModuloSistema m : ModuloSistema.values()) {
            if (activos.contains(m.codigo()) && sesion.tienePermiso(m.permisoVer())) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    /** Estado de todos los modulos del sistema (activados o no) para una empresa. */
    public Map<String, Boolean> estado(long empresaId) {
        return repo.estado(empresaId);
    }

    /** Activa o desactiva un modulo para la empresa y lo audita. */
    public void ajustar(long empresaId, ModuloSistema modulo, boolean activo) {
        repo.ajustar(empresaId, modulo.codigo(), activo);
        new ServicioAuditoria().registrar("CONFIGURACION", "empresa_modulo", null, "EDITAR",
                null, Map.of("modulo", modulo.codigo(), "activo", activo));
    }
}
