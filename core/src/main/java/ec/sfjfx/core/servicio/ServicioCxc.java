package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.CxcMovimiento;
import ec.sfjfx.core.repositorio.ClienteRepositorio;
import ec.sfjfx.core.repositorio.CxcRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Cuentas por cobrar: consulta, abonos y validacion de limite de credito. */
public final class ServicioCxc {

    private final CxcRepositorio repo = new CxcRepositorio();
    private final ClienteRepositorio clientes = new ClienteRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<CxcMovimiento> listar(Long clienteId) {
        return repo.listar(empresa(), clienteId, 300);
    }

    public List<Cliente> conSaldo() {
        return clientes.conSaldo(empresa());
    }

    /** Registra un abono del cliente (reduce el saldo). */
    public void abonar(long clienteId, BigDecimal monto, Long medioPagoId, String nota) {
        if (monto == null || monto.signum() <= 0) {
            throw new ValidacionExcepcion("El monto del abono debe ser mayor a cero");
        }
        Bd.transaccion(c -> {
            Cliente cl = clientes.porId(c, clienteId).orElseThrow(() -> new ValidacionExcepcion("Cliente no existe"));
            BigDecimal nuevoSaldo = nz(cl.saldo()).subtract(monto);
            clientes.actualizarSaldo(c, clienteId, nuevoSaldo);
            repo.registrar(c, empresa(), clienteId, null, "ABONO", monto, nuevoSaldo, medioPagoId, usuario(), nota);
            return null;
        });
        auditoria.registrar("CLIENTES", "cxc", clienteId, "EDITAR", null, Map.of("abono", monto.toPlainString()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
