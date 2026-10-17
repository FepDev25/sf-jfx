package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioEmpresa {

    private final EmpresaRepositorio repo = new EmpresaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Empresa> listar() {
        return repo.listarTodas();
    }

    public Empresa guardar(Empresa e) {
        Validaciones.requerido(e.ruc(), "RUC");
        Validaciones.requerido(e.razonSocial(), "Razon social");
        if (!Validaciones.identificacionEcuador(e.ruc())) {
            throw new ec.sfjfx.core.util.ValidacionExcepcion("El RUC no es valido");
        }
        if (!Validaciones.email(e.email())) {
            throw new ec.sfjfx.core.util.ValidacionExcepcion("El email no es valido");
        }
        if (e.id() == null) {
            long id = repo.crear(e);
            auditoria.registrar("EMPRESA", "empresa", id, "CREAR", null, datos(e));
            return new Empresa(id, e.ruc(), e.razonSocial(), e.nombreComercial(), e.direccionMatriz(),
                    e.telefono(), e.email(), e.logoPath(), e.contribuyenteEspecial(),
                    e.obligadoContabilidad(), e.regimen(), e.moneda(), e.activo());
        }
        repo.actualizar(e);
        auditoria.registrar("EMPRESA", "empresa", e.id(), "EDITAR", null, datos(e));
        return e;
    }

    private Map<String, Object> datos(Empresa e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruc", e.ruc());
        m.put("razonSocial", e.razonSocial());
        m.put("nombreComercial", e.nombreComercial());
        m.put("regimen", e.regimen());
        m.put("activo", e.activo());
        return m;
    }
}
