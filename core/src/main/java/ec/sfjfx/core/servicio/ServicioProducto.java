package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.CodigoBarra;
import ec.sfjfx.core.modelo.HistorialPrecio;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.repositorio.HistorialPrecioRepositorio;
import ec.sfjfx.core.repositorio.ProductoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioProducto {

    private final ProductoRepositorio repo = new ProductoRepositorio();
    private final HistorialPrecioRepositorio historialRepo = new HistorialPrecioRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Producto> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public ProductoCompleto cargar(long id) {
        return repo.cargarCompleto(id);
    }

    public List<HistorialPrecio> historial(long productoId) {
        return historialRepo.porProducto(productoId, 100);
    }

    public long guardar(ProductoCompleto completo) {
        Producto p = completo.producto();
        Validaciones.requerido(p.codigo(), "Codigo");
        Validaciones.requerido(p.nombre(), "Nombre");
        if (p.tipo() == null) {
            throw new ValidacionExcepcion("Seleccione un tipo de producto");
        }
        if (repo.codigoExiste(p.empresaId(), p.codigo(), p.id())) {
            throw new ValidacionExcepcion("Ya existe un producto con ese codigo");
        }
        for (CodigoBarra cb : completo.codigos()) {
            if (cb.codigo() != null && !cb.codigo().isBlank()
                    && repo.codigoBarraExiste(cb.codigo().trim(), p.id())) {
                throw new ValidacionExcepcion("El codigo de barras ya esta en uso: " + cb.codigo());
            }
        }

        boolean nuevo = p.id() == null;
        ProductoCompleto anterior = nuevo ? null : repo.cargarCompleto(p.id());
        long usuarioId = SesionActual.requerida().usuario().id();

        long id = Bd.transaccion(c -> {
            long pid;
            if (nuevo) {
                pid = repo.insertar(c, p);
            } else {
                repo.actualizar(c, p);
                pid = p.id();
            }
            repo.guardarHijos(c, pid, completo);
            registrarHistorial(c, pid, p.empresaId(), anterior, completo, usuarioId);
            return pid;
        });

        auditoria.registrar("PRODUCTOS", "producto", id, nuevo ? "CREAR" : "EDITAR", null, datos(p));
        return id;
    }

    private void registrarHistorial(Connection c, long productoId, long empresaId, ProductoCompleto anterior,
                                    ProductoCompleto nuevo, long usuarioId) throws SQLException {
        BigDecimal costoAnterior = anterior == null ? null : anterior.producto().costo();
        BigDecimal costoNuevo = nuevo.producto().costo();
        if (!iguales(costoAnterior, costoNuevo)) {
            historialRepo.registrar(c, empresaId, productoId, null, "COSTO", costoAnterior, costoNuevo, null, usuarioId);
        }

        Map<Long, BigDecimal> preciosAnteriores = new HashMap<>();
        if (anterior != null) {
            for (PrecioProducto pp : anterior.precios()) {
                preciosAnteriores.put(pp.listaPrecioId(), pp.precio());
            }
        }
        for (PrecioProducto pp : nuevo.precios()) {
            if (pp.precio() == null) {
                continue;
            }
            BigDecimal previo = preciosAnteriores.get(pp.listaPrecioId());
            if (!iguales(previo, pp.precio())) {
                historialRepo.registrar(c, empresaId, productoId, pp.listaPrecioId(), "PRECIO",
                        previo, pp.precio(), pp.sucursalId(), usuarioId);
            }
        }
    }

    private boolean iguales(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private Map<String, Object> datos(Producto p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("codigo", p.codigo());
        m.put("nombre", p.nombre());
        m.put("tipo", p.tipo() == null ? "" : p.tipo().name());
        m.put("costo", p.costo() == null ? "0" : p.costo().toPlainString());
        m.put("activo", p.activo());
        return m;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
