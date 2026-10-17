package ec.sfjfx.core.reporte;

import java.util.List;

import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

/** Servicio de reporteria: catalogo, ejecucion, panel y configuraciones guardadas. */
public final class ServicioReportes {

    private static final String CLAVE_STOCK_BAJO = "reportes.stock_minimo";
    private static final int STOCK_BAJO_POR_DEFECTO = 5;

    private final RepositorioReportes repo = new RepositorioReportes();
    private final ReporteGuardadoRepositorio guardados = new ReporteGuardadoRepositorio();
    private final ConfiguracionRepositorio configuracion = new ConfiguracionRepositorio();

    public List<DefinicionReporte> catalogo() {
        return CatalogoReportes.todos();
    }

    public List<DefinicionReporte> porGrupo(GrupoReporte grupo) {
        return CatalogoReportes.porGrupo(grupo);
    }

    public ResultadoReporte ejecutar(String codigo, FiltroReporte filtro) {
        DefinicionReporte definicion = CatalogoReportes.porCodigo(codigo)
                .orElseThrow(() -> new ValidacionExcepcion("Reporte no encontrado: " + codigo));
        return repo.ejecutar(definicion, empresaActual(), filtro == null ? FiltroReporte.sinFiltros() : filtro,
                umbralStockBajo());
    }

    public PanelDashboard dashboard() {
        return repo.dashboard(empresaActual(), sucursalActual(), umbralStockBajo());
    }

    public List<ReporteGuardado> guardados() {
        return guardados.listar(empresaActual(), SesionActual.requerida().usuario().id());
    }

    public long guardar(String codigo, String nombre, FiltroReporte filtro) {
        Validaciones.requerido(codigo, "Reporte");
        Validaciones.requerido(nombre, "Nombre");
        CatalogoReportes.porCodigo(codigo)
                .orElseThrow(() -> new ValidacionExcepcion("Reporte no encontrado: " + codigo));
        ReporteGuardado r = new ReporteGuardado(null, empresaActual(),
                SesionActual.requerida().usuario().id(), codigo, nombre.trim(),
                filtro.desde(), filtro.hasta(), filtro.sucursalId(), filtro.categoriaId(), filtro.usuarioId());
        return guardados.insertar(r);
    }

    public void eliminarGuardado(long id) {
        guardados.eliminar(empresaActual(), id);
    }

    public int umbralStockBajo() {
        String valor = configuracion.valor(empresaActual(), CLAVE_STOCK_BAJO,
                String.valueOf(STOCK_BAJO_POR_DEFECTO));
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return STOCK_BAJO_POR_DEFECTO;
        }
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    private Long sucursalActual() {
        var sucursal = SesionActual.requerida().sucursal();
        return sucursal == null ? null : sucursal.id();
    }
}
