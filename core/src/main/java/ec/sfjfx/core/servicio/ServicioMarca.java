package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Marca;
import ec.sfjfx.core.repositorio.MarcaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioMarca {

    private final MarcaRepositorio repo = new MarcaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Marca> listar() {
        return repo.listar(empresa());
    }

    public Marca guardar(Marca m) {
        Validaciones.requerido(m.nombre(), "Nombre");
        if (repo.nombreExiste(empresa(), m.nombre(), m.id())) {
            throw new ValidacionExcepcion("Ya existe una marca con ese nombre");
        }
        if (m.id() == null) {
            long id = repo.crear(m);
            auditoria.registrar("PRODUCTOS", "marca", id, "CREAR", null, Map.of("nombre", m.nombre()));
            return new Marca(id, m.empresaId(), m.nombre(), m.descripcion(), m.logoPath(), m.activo());
        }
        repo.actualizar(m);
        auditoria.registrar("PRODUCTOS", "marca", m.id(), "EDITAR", null, Map.of("nombre", m.nombre()));
        return m;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
