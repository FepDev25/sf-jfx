package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.repositorio.ImpuestoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioImpuesto {

    private final ImpuestoRepositorio repo = new ImpuestoRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Impuesto> listar() {
        return repo.listar(SesionActual.requerida().empresa().id());
    }

    public List<Impuesto> deVenta() {
        return listar().stream().filter(i -> !i.esRetencion() && i.activo()).toList();
    }

    public Impuesto guardar(Impuesto i) {
        Validaciones.requerido(i.nombre(), "Nombre");
        if (i.tipo() == null) {
            throw new ec.sfjfx.core.util.ValidacionExcepcion("Seleccione un tipo de impuesto");
        }
        if (i.id() == null) {
            long id = repo.crear(i);
            auditoria.registrar("PRODUCTOS", "impuesto", id, "CREAR", null, Map.of("nombre", i.nombre()));
            return new Impuesto(id, i.empresaId(), i.codigoSri(), i.nombre(), i.tipo(), i.porcentaje(),
                    i.vigenteDesde(), i.vigenteHasta(), i.esRetencion(), i.activo());
        }
        repo.actualizar(i);
        auditoria.registrar("PRODUCTOS", "impuesto", i.id(), "EDITAR", null, Map.of("nombre", i.nombre()));
        return i;
    }
}
