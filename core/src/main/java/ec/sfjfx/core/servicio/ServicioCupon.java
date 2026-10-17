package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Cupon;
import ec.sfjfx.core.repositorio.CuponRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioCupon {

    private final CuponRepositorio repo = new CuponRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Cupon> listar() {
        return repo.listar(empresa());
    }

    public Cupon guardar(Cupon c) {
        Validaciones.requerido(c.codigo(), "Codigo");
        if (repo.codigoExiste(empresa(), c.codigo(), c.id())) {
            throw new ValidacionExcepcion("Ya existe un cupon con ese codigo");
        }
        if (c.id() == null) {
            long id = repo.crear(c);
            auditoria.registrar("PROMOCIONES", "cupon", id, "CREAR", null, Map.of("codigo", c.codigo()));
            return new Cupon(id, c.empresaId(), c.codigo(), c.tipo(), c.valor(), c.vigenteDesde(), c.vigenteHasta(),
                    c.usosMaximos(), 0, c.activo());
        }
        repo.actualizar(c);
        auditoria.registrar("PROMOCIONES", "cupon", c.id(), "EDITAR", null, Map.of("codigo", c.codigo()));
        return c;
    }

    /** Valida el cupon y devuelve el descuento sobre la base dada. */
    public BigDecimal validarYCalcular(String codigo, BigDecimal base) {
        Cupon c = repo.porCodigo(empresa(), codigo)
                .orElseThrow(() -> new ValidacionExcepcion("Cupon no encontrado"));
        if (!c.activo()) {
            throw new ValidacionExcepcion("El cupon no esta activo");
        }
        LocalDate hoy = LocalDate.now();
        if (c.vigenteDesde() != null && hoy.isBefore(c.vigenteDesde())) {
            throw new ValidacionExcepcion("El cupon aun no esta vigente");
        }
        if (c.vigenteHasta() != null && hoy.isAfter(c.vigenteHasta())) {
            throw new ValidacionExcepcion("El cupon esta vencido");
        }
        if (c.usosMaximos() != null && c.usosActuales() >= c.usosMaximos()) {
            throw new ValidacionExcepcion("El cupon alcanzo su limite de usos");
        }
        BigDecimal descuento = "PORCENTAJE".equals(c.tipo())
                ? base.multiply(c.valor()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : c.valor();
        return descuento.min(base);
    }

    public Cupon porCodigo(String codigo) {
        return repo.porCodigo(empresa(), codigo).orElseThrow(() -> new ValidacionExcepcion("Cupon no encontrado"));
    }

    public void usar(long cuponId) {
        repo.incrementarUso(cuponId);
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
