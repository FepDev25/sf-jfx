package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.repositorio.SesionCajaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Apertura, arqueo y cierre de sesiones (turnos) de caja. */
public final class ServicioSesionCaja {

    private final SesionCajaRepositorio repo = new SesionCajaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public SesionCaja sesionAbierta(long cajaId) {
        return repo.sesionAbierta(cajaId).orElse(null);
    }

    public List<SesionCaja> listar() {
        return repo.listar(empresa(), 200);
    }

    public SesionCaja porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Sesion no encontrada"));
    }

    public long abrir(long cajaId, BigDecimal montoInicial) {
        if (repo.sesionAbierta(cajaId).isPresent()) {
            throw new ValidacionExcepcion("La caja ya tiene una sesion abierta");
        }
        long id = repo.abrir(empresa(), cajaId, usuario(), montoInicial == null ? BigDecimal.ZERO : montoInicial);
        auditoria.registrar("POS", "sesion_caja", id, "CREAR", null, Map.of("caja", cajaId));
        return id;
    }

    public BigDecimal esperadoEfectivo(long sesionId) {
        return repo.montoEsperadoEfectivo(sesionId);
    }

    /** Cierra la sesion con el arqueo por denominacion. */
    public SesionCaja cerrar(long sesionId, Map<BigDecimal, Integer> arqueo, String observacion) {
        SesionCaja sesion = porId(sesionId);
        if (!sesion.abierta()) {
            throw new ValidacionExcepcion("La sesion ya esta cerrada");
        }
        BigDecimal esperado = repo.montoEsperadoEfectivo(sesionId);
        BigDecimal contado = arqueo.entrySet().stream()
                .map(e -> e.getKey().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferencia = contado.subtract(esperado);

        ec.sfjfx.core.db.Bd.transaccion(c -> {
            for (Map.Entry<BigDecimal, Integer> e : arqueo.entrySet()) {
                BigDecimal subtotal = e.getKey().multiply(BigDecimal.valueOf(e.getValue()));
                repo.insertarArqueo(c, sesionId, e.getKey(), e.getValue(), subtotal);
            }
            return null;
        });
        repo.cerrar(sesionId, usuario(), esperado, contado, diferencia, observacion);
        auditoria.registrar("POS", "sesion_caja", sesionId, "EDITAR", null,
                Map.of("diferencia", diferencia.toPlainString()));
        return porId(sesionId);
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
