package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.InventarioRepositorio;
import ec.sfjfx.core.repositorio.ClienteRepositorio;
import ec.sfjfx.core.repositorio.CxcRepositorio;
import ec.sfjfx.core.repositorio.PrecioRepositorio;
import ec.sfjfx.core.repositorio.VentaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Ventas de caja y punto de venta. */
public final class ServicioVenta {

    private final ServicioLicencia licencia = new ServicioLicencia();
    private final VentaRepositorio repo = new VentaRepositorio();
    private final InventarioRepositorio inv = new InventarioRepositorio();
    private final ClienteRepositorio clientes = new ClienteRepositorio();
    private final CxcRepositorio cxc = new CxcRepositorio();
    private final ServicioFidelidad fidelidad = new ServicioFidelidad();
    private final PrecioRepositorio precios = new PrecioRepositorio();
    private final ServicioInventario inventario = new ServicioInventario();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Venta> listar(String filtro) {
        return repo.listar(empresa(), filtro, 300);
    }

    public ec.sfjfx.core.repositorio.Pagina<Venta> pagina(String filtro, int pagina, int tamano) {
        return repo.paginar(empresa(), filtro, pagina, tamano);
    }

    public Venta porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Venta no encontrada"));
    }

    public List<Venta> aparcadas(long cajaId) {
        return repo.aparcadas(cajaId);
    }

    public Optional<ListaPrecio> listaPrecioDefault() {
        return precios.listaDefault(empresa());
    }

    public Map<Long, BigDecimal> mapaPrecios(long listaPrecioId) {
        return precios.mapa(listaPrecioId);
    }

    /** Cobra una venta (o reanuda una aparcada): registra pagos y da salida de stock. */
    public long cobrar(SesionCaja sesion, long bodegaId, Long ventaIdReanudar, Long clienteId, String nota,
                       boolean preciosIncluyenIva, List<VentaDetalle> lineas, List<VentaPago> pagos,
                       BigDecimal descuentoGlobal, BigDecimal propina, Long autorizadoPorId) {
        licencia.exigirOperacion("cobrar una venta");
        if (lineas == null || lineas.isEmpty()) {
            throw new ValidacionExcepcion("La venta no tiene items");
        }
        long usuarioId = usuario();
        long id = Bd.transaccion(c -> {
            Totales t = calcular(c, lineas, descuentoGlobal, propina, preciosIncluyenIva);
            BigDecimal pagado = pagos == null ? BigDecimal.ZERO
                    : pagos.stream().map(p -> nz(p.monto())).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (pagado.compareTo(t.total()) < 0) {
                throw new ValidacionExcepcion("El pago (" + pagado.toPlainString() + ") es menor al total ("
                        + t.total().toPlainString() + ")");
            }
            long sucursalId = inv.sucursalDeBodega(c, bodegaId);
            long ventaId;
            if (ventaIdReanudar != null) {
                Venta existente = repo.porId(ventaIdReanudar).orElseThrow(() -> new ValidacionExcepcion("Venta no existe"));
                Venta venta = new Venta(ventaIdReanudar, empresa(), sucursalId, sesion.cajaId(), sesion.id(),
                        existente.numero(), clienteId, null, usuarioId, "VENTA", "PAGADA", null, t.subtotal(),
                        t.descuentoTotal(), t.impuesto(), t.propina(), t.total(), t.costo(), t.utilidad(),
                        autorizadoPorId, nota, t.detalles(), pagos);
                repo.reemplazarDetallesYPagos(c, ventaIdReanudar, venta, usuarioId);
                ventaId = ventaIdReanudar;
            } else {
                Venta venta = new Venta(null, empresa(), sucursalId, sesion.cajaId(), sesion.id(), numero(), clienteId,
                        null, usuarioId, "VENTA", "PAGADA", null, t.subtotal(), t.descuentoTotal(), t.impuesto(),
                        t.propina(), t.total(), t.costo(), t.utilidad(), autorizadoPorId, nota, t.detalles(), pagos);
                ventaId = repo.insertar(c, venta, usuarioId);
            }
            for (VentaDetalle d : t.detalles()) {
                if (d.productoId() != null) {
                    inventario.salidaEn(c, bodegaId, d.productoId(), d.cantidad(), "VENTA", "VENTA", ventaId, null, nota);
                }
            }
            if (clienteId != null && pagos != null) {
                for (VentaPago p : pagos) {
                    String tipo = tipoMedio(c, p.medioPagoId());
                    if ("CREDITO".equals(tipo)) {
                        registrarCargoCredito(c, clienteId, ventaId, nz(p.monto()), p.medioPagoId());
                    } else if ("PUNTOS".equals(tipo)) {
                        fidelidad.canjearEn(c, clienteId, nz(p.monto()));
                    }
                }
            }
            if (clienteId != null) {
                fidelidad.acumularEn(c, clienteId, t.total(), ventaId);
            }
            return ventaId;
        });
        auditoria.registrar("POS", "venta", id, "CREAR", null, Map.of("items", lineas.size()));
        return id;
    }

    public long apartar(SesionCaja sesion, long bodegaId, Long clienteId, String nota, boolean preciosIncluyenIva,
                        List<VentaDetalle> lineas, BigDecimal descuentoGlobal) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ValidacionExcepcion("La venta no tiene items");
        }
        long usuarioId = usuario();
        long id = Bd.transaccion(c -> {
            Totales t = calcular(c, lineas, descuentoGlobal, BigDecimal.ZERO, preciosIncluyenIva);
            long sucursalId = inv.sucursalDeBodega(c, bodegaId);
            Venta venta = new Venta(null, empresa(), sucursalId, sesion.cajaId(), sesion.id(), numero(), clienteId, null,
                    usuarioId, "VENTA", "APARCADA", null, t.subtotal(), t.descuentoTotal(), t.impuesto(), t.propina(),
                    t.total(), t.costo(), t.utilidad(), null, nota, t.detalles(), List.of());
            return repo.insertar(c, venta, usuarioId);
        });
        auditoria.registrar("POS", "venta", id, "CREAR", null, Map.of("estado", "APARCADA"));
        return id;
    }

    /** Anula una venta: repone stock si estaba pagada. */
    public void anular(long ventaId, long bodegaId) {
        licencia.exigirOperacion("anular una venta");
        Venta v = porId(ventaId);
        if ("ANULADA".equals(v.estado())) {
            throw new ValidacionExcepcion("La venta ya esta anulada");
        }
        Bd.transaccion(c -> {
            if (v.pagada()) {
                for (VentaDetalle d : repo.detallesEn(c, ventaId)) {
                    if (d.productoId() != null) {
                        inventario.entradaEn(c, bodegaId, d.productoId(), d.cantidad(), null, "DEVOLUCION_VENTA",
                                "VENTA", ventaId, null, null, "Anulacion venta " + v.numero());
                    }
                }
            }
            repo.cambiarEstado(c, ventaId, "ANULADA");
            return null;
        });
        auditoria.registrar("POS", "venta", ventaId, "ANULAR", null, null);
    }

    // ------------------------------------------------------------------ calculo

    private Totales calcular(Connection c, List<VentaDetalle> lineas, BigDecimal descuentoGlobal, BigDecimal propina,
                             boolean preciosIncluyenIva) throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal descuentoLineas = BigDecimal.ZERO;
        BigDecimal totalLineas = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        List<VentaDetalle> norm = new ArrayList<>();
        for (VentaDetalle d : lineas) {
            BigDecimal cant = nz(d.cantidad());
            if (cant.signum() <= 0) {
                throw new ValidacionExcepcion("Cantidad invalida en la venta");
            }
            BigDecimal base = cant.multiply(nz(d.precioUnitario())).subtract(nz(d.descuento()));
            BigDecimal rate = porcentaje(c, d.impuestoId());
            BigDecimal impuestoLinea;
            BigDecimal neto;
            BigDecimal totalLinea;
            if (preciosIncluyenIva) {
                impuestoLinea = base.subtract(base.divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP));
                neto = base.subtract(impuestoLinea);
                totalLinea = base;
            } else {
                impuestoLinea = base.multiply(rate).setScale(4, RoundingMode.HALF_UP);
                neto = base;
                totalLinea = base.add(impuestoLinea);
            }
            BigDecimal costoProm = d.productoId() == null ? BigDecimal.ZERO : inv.costoPromedio(c, d.productoId());
            norm.add(new VentaDetalle(null, 0, d.productoId(), d.descripcion(), cant, nz(d.precioUnitario()),
                    nz(d.descuento()), d.impuestoId(), impuestoLinea, neto, totalLinea, costoProm));
            subtotal = subtotal.add(neto);
            impuesto = impuesto.add(impuestoLinea);
            descuentoLineas = descuentoLineas.add(nz(d.descuento()));
            totalLineas = totalLineas.add(totalLinea);
            costo = costo.add(cant.multiply(costoProm));
        }
        BigDecimal descGlobal = nz(descuentoGlobal);
        BigDecimal prop = nz(propina);
        BigDecimal total = totalLineas.subtract(descGlobal).add(prop);
        BigDecimal utilidad = total.subtract(impuesto).subtract(costo);
        return new Totales(norm, subtotal, descuentoLineas.add(descGlobal), impuesto, prop, total, costo, utilidad);
    }

    private record Totales(List<VentaDetalle> detalles, BigDecimal subtotal, BigDecimal descuentoTotal,
                           BigDecimal impuesto, BigDecimal propina, BigDecimal total, BigDecimal costo,
                           BigDecimal utilidad) {
    }

    private BigDecimal porcentaje(Connection c, Long impuestoId) throws SQLException {
        if (impuestoId == null) {
            return BigDecimal.ZERO;
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT porcentaje FROM impuesto WHERE id = ?")) {
            ps.setLong(1, impuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return nz(rs.getBigDecimal(1)).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private String tipoMedio(Connection c, long medioPagoId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT tipo FROM medio_pago WHERE id = ?")) {
            ps.setLong(1, medioPagoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    private void registrarCargoCredito(Connection c, long clienteId, long ventaId, BigDecimal monto, Long medioPagoId)
            throws SQLException {
        Cliente cl = clientes.porId(c, clienteId).orElseThrow(() -> new ValidacionExcepcion("Cliente no existe"));
        BigDecimal nuevoSaldo = nz(cl.saldo()).add(monto);
        if (nz(cl.limiteCredito()).signum() > 0 && nuevoSaldo.compareTo(cl.limiteCredito()) > 0) {
            throw new ValidacionExcepcion("La venta excede el limite de credito del cliente ("
                    + cl.limiteCredito().toPlainString() + ")");
        }
        clientes.actualizarSaldo(c, clienteId, nuevoSaldo);
        cxc.registrar(c, empresa(), clienteId, ventaId, "CARGO", monto, nuevoSaldo, medioPagoId, usuario(),
                "Venta a credito");
    }

    private String numero() {
        return "V-" + System.currentTimeMillis();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private long usuario() {
        return SesionActual.requerida().usuario().id();
    }
}
