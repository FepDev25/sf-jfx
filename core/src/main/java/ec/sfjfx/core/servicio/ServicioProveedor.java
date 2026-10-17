package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.ProductoProveedor;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.repositorio.ProveedorRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioProveedor {

    private final ProveedorRepositorio repo = new ProveedorRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Proveedor> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Proveedor porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Proveedor no encontrado"));
    }

    public List<ProductoProveedor> productosDe(long proveedorId) {
        return repo.productosDe(proveedorId);
    }

    public Proveedor guardar(Proveedor p) {
        Validaciones.requerido(p.numeroIdentificacion(), "Identificacion");
        Validaciones.requerido(p.razonSocial(), "Razon social");
        if (!"PASAPORTE".equals(p.tipoIdentificacion()) && !Validaciones.identificacionEcuador(p.numeroIdentificacion())) {
            throw new ValidacionExcepcion("La identificacion no es valida");
        }
        if (repo.identificacionExiste(empresa(), p.numeroIdentificacion(), p.id())) {
            throw new ValidacionExcepcion("Ya existe un proveedor con esa identificacion");
        }
        if (p.id() == null) {
            long id = repo.crear(p);
            auditoria.registrar("PROVEEDORES", "proveedor", id, "CREAR", null, datos(p));
            return new Proveedor(id, p.empresaId(), p.tipoIdentificacion(), p.numeroIdentificacion(), p.razonSocial(),
                    p.nombreComercial(), p.direccion(), p.telefono(), p.email(), p.tipoContribuyente(), p.diasCredito(),
                    p.aplicaRetencionIva(), p.aplicaRetencionRenta(), p.activo(), p.nota());
        }
        repo.actualizar(p);
        auditoria.registrar("PROVEEDORES", "proveedor", p.id(), "EDITAR", null, datos(p));
        return p;
    }

    public void guardarProducto(ProductoProveedor pp) {
        repo.guardarProductoProveedor(pp);
        auditoria.registrar("PROVEEDORES", "producto_proveedor", null, "EDITAR", null,
                Map.of("producto", pp.productoId(), "proveedor", pp.proveedorId()));
    }

    public void eliminarProducto(long id) {
        repo.eliminarProductoProveedor(id);
    }

    private Map<String, Object> datos(Proveedor p) {
        return Map.of("razonSocial", p.razonSocial(), "identificacion", p.numeroIdentificacion());
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
