package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.repositorio.ListaPrecioRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioListaPrecio {

    private final ListaPrecioRepositorio repo = new ListaPrecioRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<ListaPrecio> listar() {
        return repo.listar(empresa());
    }

    public List<ListaPrecio> activas() {
        return repo.activas(empresa());
    }

    public ListaPrecio guardar(ListaPrecio l) {
        Validaciones.requerido(l.nombre(), "Nombre");
        if (repo.nombreExiste(empresa(), l.nombre(), l.id())) {
            throw new ValidacionExcepcion("Ya existe una lista con ese nombre");
        }
        if (l.id() == null) {
            long id = repo.crear(l);
            auditoria.registrar("LISTAS_PRECIO", "lista_precio", id, "CREAR", null, Map.of("nombre", l.nombre()));
            return new ListaPrecio(id, l.empresaId(), l.nombre(), l.tipo(), l.moneda(), l.incluyeIva(),
                    l.esDefault(), l.activo());
        }
        repo.actualizar(l);
        auditoria.registrar("LISTAS_PRECIO", "lista_precio", l.id(), "EDITAR", null, Map.of("nombre", l.nombre()));
        return l;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
