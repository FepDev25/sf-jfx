package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioSucursal {

    private final SucursalRepositorio repo = new SucursalRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Sucursal> listar() {
        return repo.porEmpresa(empresaActual());
    }

    public long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    public Sucursal guardar(Sucursal s) {
        Validaciones.requerido(s.codigo(), "Codigo");
        Validaciones.requerido(s.nombre(), "Nombre");
        if (s.id() == null) {
            long id = repo.crear(s);
            auditoria.registrar("SUCURSALES", "sucursal", id, "CREAR", null, datos(s));
            return new Sucursal(id, s.empresaId(), s.codigo(), s.nombre(), s.direccion(),
                    s.telefono(), s.esMatriz(), s.activo());
        }
        repo.actualizar(s);
        auditoria.registrar("SUCURSALES", "sucursal", s.id(), "EDITAR", null, datos(s));
        return s;
    }

    public void validarCreacion(Sucursal s) {
        if (s.empresaId() <= 0) {
            throw new ValidacionExcepcion("Empresa invalida");
        }
    }

    private Map<String, Object> datos(Sucursal s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", s.codigo());
        m.put("nombre", s.nombre());
        m.put("esMatriz", s.esMatriz());
        m.put("activo", s.activo());
        return m;
    }
}
