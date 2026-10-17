package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Configuracion;
import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioConfiguracion {

    private final ConfiguracionRepositorio repo = new ConfiguracionRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Configuracion> listar() {
        return repo.porEmpresa(empresaActual());
    }

    public String valor(String clave, String porDefecto) {
        return repo.valor(empresaActual(), clave, porDefecto);
    }

    public void guardar(String clave, String valor, String tipo, String grupo, String descripcion) {
        Validaciones.requerido(clave, "Clave");
        repo.guardar(empresaActual(), clave.trim(), valor, tipo, grupo, descripcion);
        auditoria.registrar("CONFIGURACION", "configuracion", null, "EDITAR", null, Map.of(clave, valor == null ? "" : valor));
    }

    public void eliminar(long id) {
        repo.eliminar(id);
        auditoria.registrar("CONFIGURACION", "configuracion", id, "ELIMINAR", null, null);
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }
}
