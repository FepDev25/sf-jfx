package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.repositorio.CategoriaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioCategoria {

    private final CategoriaRepositorio repo = new CategoriaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Categoria> listar() {
        return repo.listar(empresa());
    }

    public Categoria guardar(Categoria cat) {
        Validaciones.requerido(cat.nombre(), "Nombre");
        if (cat.codigo() != null && !cat.codigo().isBlank()
                && repo.codigoExiste(empresa(), cat.codigo(), cat.id())) {
            throw new ValidacionExcepcion("Ya existe una categoria con ese codigo");
        }
        if (cat.id() != null && cat.id().equals(cat.padreId())) {
            throw new ValidacionExcepcion("Una categoria no puede ser su propio padre");
        }
        if (cat.id() == null) {
            long id = repo.crear(cat);
            auditoria.registrar("PRODUCTOS", "categoria", id, "CREAR", null, datos(cat));
            return new Categoria(id, cat.empresaId(), cat.padreId(), cat.codigo(), cat.nombre(),
                    cat.descripcion(), cat.orden(), cat.activo());
        }
        repo.actualizar(cat);
        auditoria.registrar("PRODUCTOS", "categoria", cat.id(), "EDITAR", null, datos(cat));
        return cat;
    }

    private Map<String, Object> datos(Categoria c) {
        return Map.of("nombre", c.nombre(), "padre", c.padreId() == null ? 0L : c.padreId(), "activo", c.activo());
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
