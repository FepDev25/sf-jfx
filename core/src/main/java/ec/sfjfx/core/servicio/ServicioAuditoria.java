package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.RegistroAuditoria;
import ec.sfjfx.core.repositorio.AuditoriaRepositorio;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Json;

/** Registra y consulta la bitacora de auditoria. */
public final class ServicioAuditoria {

    private final AuditoriaRepositorio repo;

    public ServicioAuditoria(AuditoriaRepositorio repo) {
        this.repo = repo;
    }

    public ServicioAuditoria() {
        this(new AuditoriaRepositorio());
    }

    public void registrar(String modulo, String entidad, Long entidadId, String accion,
                          Map<String, ?> anterior, Map<String, ?> nuevo) {
        Sesion s = SesionActual.requerida();
        repo.registrar(
                s.empresa().id(),
                s.sucursal() != null ? s.sucursal().id() : null,
                s.usuario().id(),
                null,
                modulo,
                entidad,
                entidadId,
                accion,
                anterior == null ? null : Json.deMapa(anterior),
                nuevo == null ? null : Json.deMapa(nuevo));
    }

    public List<RegistroAuditoria> listar(String modulo, String entidad, int limite) {
        Sesion s = SesionActual.requerida();
        return repo.listar(s.empresa().id(), modulo, entidad, limite);
    }

    public List<String> modulos() {
        return repo.modulosDistintos(SesionActual.requerida().empresa().id());
    }
}
