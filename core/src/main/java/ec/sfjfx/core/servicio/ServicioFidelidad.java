package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.ProgramaFidelidad;
import ec.sfjfx.core.modelo.PuntosMovimiento;
import ec.sfjfx.core.repositorio.FidelidadRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Programa de fidelidad: acumulacion y canje de puntos. */
public final class ServicioFidelidad {

    private final FidelidadRepositorio repo = new FidelidadRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public ProgramaFidelidad programa() {
        return repo.programa(empresa()).orElse(new ProgramaFidelidad(null, empresa(), "Programa de puntos", false,
                BigDecimal.ONE, new BigDecimal("0.01"), null));
    }

    public void guardarPrograma(ProgramaFidelidad p) {
        repo.guardarPrograma(p);
        auditoria.registrar("FIDELIZACION", "programa", p.id(), p.id() == null ? "CREAR" : "EDITAR", null,
                Map.of("activo", p.activo()));
    }

    public BigDecimal saldo(long clienteId) {
        return ec.sfjfx.core.db.Bd.consulta(c -> repo.saldo(c, clienteId));
    }

    public List<PuntosMovimiento> movimientos(long clienteId) {
        return repo.movimientos(clienteId, 200);
    }

    public List<Object[]> clientesConPuntos() {
        return repo.clientesConPuntos(empresa());
    }

    /** Acumula puntos por el monto de una venta. */
    public void acumularEn(Connection c, long clienteId, BigDecimal monto, Long ventaId) throws SQLException {
        ProgramaFidelidad p = programa();
        if (!p.activo()) {
            return;
        }
        BigDecimal puntos = monto.multiply(nz(p.puntosPorMonto())).setScale(2, RoundingMode.HALF_UP);
        if (puntos.signum() <= 0) {
            return;
        }
        BigDecimal nuevoSaldo = repo.saldo(c, clienteId).add(puntos);
        repo.actualizarSaldo(c, clienteId, nuevoSaldo);
        repo.registrarMovimiento(c, clienteId, "ACUMULA", puntos, "VENTA", ventaId, nuevoSaldo, "Acumulacion por venta");
    }

    /** Canjea puntos por un valor monetario (uso directo, p. ej. desde UI). */
    public void canjear(long clienteId, BigDecimal valorMonetario) {
        ec.sfjfx.core.db.Bd.transaccion(c -> {
            canjearEn(c, clienteId, valorMonetario);
            return null;
        });
    }

    /** Canjea puntos por un valor monetario (usado como medio de pago). */
    public void canjearEn(Connection c, long clienteId, BigDecimal valorMonetario) throws SQLException {
        ProgramaFidelidad p = programa();
        BigDecimal valorPunto = nz(p.valorPunto());
        if (valorPunto.signum() <= 0) {
            throw new ValidacionExcepcion("El programa de fidelidad no permite canje");
        }
        BigDecimal puntos = valorMonetario.divide(valorPunto, 2, RoundingMode.HALF_UP);
        BigDecimal saldo = repo.saldo(c, clienteId);
        if (puntos.compareTo(saldo) > 0) {
            throw new ValidacionExcepcion("El cliente no tiene puntos suficientes (" + saldo.toPlainString() + ")");
        }
        BigDecimal nuevoSaldo = saldo.subtract(puntos);
        repo.actualizarSaldo(c, clienteId, nuevoSaldo);
        repo.registrarMovimiento(c, clienteId, "CANJE", puntos.negate(), "VENTA", null, nuevoSaldo, "Canje en venta");
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
