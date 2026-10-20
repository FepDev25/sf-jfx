package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.repositorio.Pagina;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;
import ec.sfjfx.core.modelo.ComprobantePago;
import ec.sfjfx.core.modelo.DevolucionVenta;
import ec.sfjfx.core.modelo.DevolucionVentaDetalle;
import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.modelo.Retencion;
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.repositorio.CertificadoRepositorio;
import ec.sfjfx.core.repositorio.ClienteRepositorio;
import ec.sfjfx.core.repositorio.ComprobanteRepositorio;
import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
import ec.sfjfx.core.repositorio.ProductoRepositorio;
import ec.sfjfx.core.repositorio.ProveedorRepositorio;
import ec.sfjfx.core.repositorio.SecuenciaDocumentoRepositorio;
import ec.sfjfx.core.repositorio.VentaRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.sri.CatalogoSri;
import ec.sfjfx.core.sri.ClaveAcceso;
import ec.sfjfx.core.sri.ConstructorXml;
import ec.sfjfx.core.sri.FirmadorXml;
import ec.sfjfx.core.sri.RidePdf;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Emision de comprobantes electronicos (simulado: no se envia al SRI). */
public final class ServicioComprobante {

    private static final String ESTAB = "001";
    private static final String PTO_EMI = "001";

    private final ServicioLicencia licencia = new ServicioLicencia();
    private final ComprobanteRepositorio repo = new ComprobanteRepositorio();
    private final SecuenciaDocumentoRepositorio secuencias = new SecuenciaDocumentoRepositorio();
    private final CertificadoRepositorio certificados = new CertificadoRepositorio();
    private final EmpresaRepositorio empresas = new EmpresaRepositorio();
    private final ConfiguracionRepositorio config = new ConfiguracionRepositorio();
    private final VentaRepositorio ventas = new VentaRepositorio();
    private final ClienteRepositorio clientes = new ClienteRepositorio();
    private final ProveedorRepositorio proveedores = new ProveedorRepositorio();
    private final ProductoRepositorio productos = new ProductoRepositorio();
    private final ServicioDevolucionVenta devoluciones = new ServicioDevolucionVenta();
    private final ServicioRetencion retenciones = new ServicioRetencion();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Comprobante> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Pagina<Comprobante> pagina(String filtro, int pagina, int tamano) {
        return repo.paginar(empresa(), filtro, pagina, tamano);
    }

    public Comprobante porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Comprobante no encontrado"));
    }

    public long emitirFacturaDesdeVenta(long ventaId) {
        licencia.exigirOperacion("emitir una factura");
        Venta venta = ventas.porId(ventaId).orElseThrow(() -> new ValidacionExcepcion("Venta no existe"));
        if (repo.facturaDeVenta(ventaId).isPresent()) {
            throw new ValidacionExcepcion("La venta ya tiene una factura emitida");
        }
        Empresa empresa = empresas.porId(venta.empresaId()).orElseThrow();
        Map<Long, BigDecimal> tarifas = tarifasIva();

        List<ComprobanteDetalle> detalles = new ArrayList<>();
        BigDecimal iva = BigDecimal.ZERO;
        for (VentaDetalle d : venta.detalles()) {
            BigDecimal tarifa = d.impuestoId() == null ? BigDecimal.ZERO : tarifas.getOrDefault(d.impuestoId(), BigDecimal.ZERO);
            String codigo = d.productoId() == null ? "S/N" : productos.porId(d.productoId()).map(Producto::codigo).orElse("S/N");
            detalles.add(new ComprobanteDetalle(null, 0, codigo, null, d.descripcion(), d.cantidad(),
                    d.precioUnitario(), d.descuento(), nz(d.subtotal()), tarifa, nz(d.impuestoValor()), nz(d.total())));
            iva = iva.add(nz(d.impuestoValor()));
        }
        Cliente cliente = venta.clienteId() == null ? null : clientes.porId(venta.clienteId()).orElse(null);
        List<ComprobantePago> pagos = new ArrayList<>();
        venta.pagos().forEach(p -> pagos.add(new ComprobantePago(codigoFormaPago(p.medioPagoId()), p.monto())));

        Comprobante cab = construir(empresa, venta.sucursalId(), "FACTURA", LocalDate.now(), cliente, detalles,
                nz(venta.subtotal()), nz(venta.descuentoTotal()), iva, nz(venta.propina()), nz(venta.total()), pagos,
                venta.id(), null, null);
        return emitir(cab, null, null);
    }

    public long emitirNotaCreditoDevolucion(long devolucionId) {
        licencia.exigirOperacion("emitir una nota de credito");
        DevolucionVenta dev = devoluciones.porId(devolucionId);
        Venta venta = ventas.porId(dev.ventaId()).orElseThrow();
        Empresa empresa = empresas.porId(venta.empresaId()).orElseThrow();
        String numDocModificado = repo.facturaDeVenta(venta.id()).map(Comprobante::numeroCompleto).orElse("");
        Cliente cliente = venta.clienteId() == null ? null : clientes.porId(venta.clienteId()).orElse(null);

        List<ComprobanteDetalle> detalles = new ArrayList<>();
        for (DevolucionVentaDetalle d : dev.detalles()) {
            BigDecimal sub = nz(d.cantidad()).multiply(nz(d.precioUnitario()));
            detalles.add(new ComprobanteDetalle(null, 0, d.productoId() <= 0 ? "S/N" : String.valueOf(d.productoId()),
                    null, d.productoNombre(), d.cantidad(), d.precioUnitario(), BigDecimal.ZERO, sub,
                    BigDecimal.ZERO, BigDecimal.ZERO, sub));
        }
        Comprobante cab = construir(empresa, venta.sucursalId(), "NOTA_CREDITO", LocalDate.now(), cliente, detalles,
                nz(dev.total()), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, nz(dev.total()), List.of(),
                venta.id(), null, numDocModificado);
        return emitir(cab, null, null);
    }

    public long emitirRetencion(long retencionId) {
        Retencion ret = retenciones.porId(retencionId);
        Empresa empresa = empresas.porId(empresa()).orElseThrow();
        Proveedor proveedor = proveedores.porId(ret.proveedorId()).orElseThrow();
        Comprobante cab = construir(empresa, sucursal(), "RETENCION", LocalDate.now(), null, List.of(),
                nz(ret.baseIva()).add(nz(ret.baseRenta())), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                nz(ret.totalRetenido()), List.of(), null, null, null);
        // receptor = proveedor
        cab = new Comprobante(cab.id(), cab.empresaId(), cab.sucursalId(), cab.tipo(), cab.establecimiento(),
                cab.puntoEmision(), cab.secuencial(), cab.numeroCompleto(), cab.claveAcceso(), cab.fechaEmision(),
                cab.ambiente(), cab.tipoEmision(), cab.emisorRuc(), cab.emisorRazonSocial(), cab.emisorNombreComercial(),
                cab.emisorDireccion(), cab.obligadoContabilidad(), cab.regimen(),
                CatalogoSri.tipoIdentificacion(proveedor.tipoIdentificacion()), proveedor.numeroIdentificacion(),
                proveedor.razonSocial(), proveedor.email(), cab.subtotal(), cab.descuento(), cab.iva(), cab.ice(),
                cab.irbpnr(), cab.propina(), cab.total(), cab.estado(), cab.ventaId(), cab.ventaNumero(),
                cab.facturaProveedorId(), cab.motivo(), cab.xmlPath(), cab.xmlFirmadoPath(), cab.pdfPath(),
                cab.comprobanteRelacionadoClave(), cab.detalles(), cab.pagos());
        return emitir(cab, ret.detalles(), null);
    }

    public void anular(long comprobanteId, String motivo) {
        Comprobante cb = porId(comprobanteId);
        if ("ANULADO".equals(cb.estado())) {
            throw new ValidacionExcepcion("El comprobante ya esta anulado");
        }
        Bd.transaccion(c -> {
            repo.cambiarEstado(c, comprobanteId, "ANULADO", motivo);
            return null;
        });
        reimprimirRide(comprobanteId);
        auditoria.registrar("FACTURACION", "comprobante", comprobanteId, "ANULAR", null, Map.of("motivo", nvl(motivo)));
    }

    /** Regenera el RIDE (PDF) y devuelve la ruta. */
    public String reimprimirRide(long comprobanteId) {
        Comprobante cb = porId(comprobanteId);
        byte[] pdf = RidePdf.generar(cb);
        Path destino = dir().resolve(cb.claveAcceso() + ".pdf");
        escribir(destino, pdf);
        Bd.transaccion(c -> {
            repo.actualizarArchivos(c, comprobanteId, cb.xmlPath(), cb.xmlFirmadoPath(), destino.toString());
            return null;
        });
        return destino.toString();
    }

    // ------------------------------------------------------------------ interno

    private long emitir(Comprobante cab, List<ec.sfjfx.core.modelo.RetencionDetalle> retencionLineas, Void ignora) {
        long id = Bd.transaccion(c -> repo.insertar(c, cab, usuario()));
        Comprobante persistido = porId(id);

        String xml = switch (persistido.tipo()) {
            case "FACTURA" -> ConstructorXml.factura(persistido);
            case "NOTA_CREDITO" -> ConstructorXml.notaCredito(persistido);
            case "NOTA_DEBITO" -> ConstructorXml.notaDebito(persistido);
            case "RETENCION" -> ConstructorXml.retencion(persistido, retencionLineas == null ? List.of() : retencionLineas);
            case "GUIA_REMISION" -> ConstructorXml.guiaRemision(persistido);
            case "LIQUIDACION_COMPRA" -> ConstructorXml.liquidacionCompra(persistido);
            default -> throw new ValidacionExcepcion("Tipo de comprobante no soportado");
        };
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        Path xmlPath = dir().resolve(persistido.claveAcceso() + ".xml");
        escribir(xmlPath, xmlBytes);

        String firmadoPath = null;
        var certificado = certificados.activo(empresa()).orElse(null);
        if (certificado != null) {
            try {
                byte[] firmado = FirmadorXml.firmar(xmlBytes, Path.of(certificado.rutaP12()),
                        ServicioCertificado.descifrar(certificado.passwordCifrada()));
                Path destino = dir().resolve(persistido.claveAcceso() + ".firmado.xml");
                escribir(destino, firmado);
                firmadoPath = destino.toString();
            } catch (RuntimeException e) {
                firmadoPath = null; // firma opcional en la etapa simulada
            }
        }

        byte[] pdf = RidePdf.generar(persistido);
        Path pdfPath = dir().resolve(persistido.claveAcceso() + ".pdf");
        escribir(pdfPath, pdf);

        String firmadoFinal = firmadoPath;
        Bd.transaccion(c -> {
            repo.actualizarArchivos(c, id, xmlPath.toString(), firmadoFinal, pdfPath.toString());
            repo.cambiarEstado(c, id, "EMITIDO", null);
            return null;
        });
        auditoria.registrar("FACTURACION", "comprobante", id, "CREAR", null,
                Map.of("tipo", persistido.tipo(), "clave", persistido.claveAcceso()));
        return id;
    }

    private Comprobante construir(Empresa empresa, long sucursalId, String tipo, LocalDate fecha, Cliente cliente,
                                  List<ComprobanteDetalle> detalles, BigDecimal subtotal, BigDecimal descuento,
                                  BigDecimal iva, BigDecimal propina, BigDecimal total, List<ComprobantePago> pagos,
                                  Long ventaId, Long facturaProveedorId, String numDocModificado) {
        String ambiente = config.valor(empresa.id(), "emisor.ambiente", "PRUEBAS");
        boolean obligado = Boolean.parseBoolean(config.valor(empresa.id(), "emisor.obligado_contabilidad", "true"));
        String secuencial = secuencias.siguiente(empresa.id(), sucursalId, tipo, ESTAB, PTO_EMI);
        String clave = ClaveAcceso.generar(fecha, CatalogoSri.codDoc(tipo), empresa.ruc(),
                CatalogoSri.ambiente(ambiente), ESTAB, PTO_EMI, secuencial, "1");
        String receptorTipoId = "07";
        String receptorNumero = "9999999999";
        String receptorRazon = "CONSUMIDOR FINAL";
        String receptorEmail = null;
        if (cliente != null) {
            receptorTipoId = CatalogoSri.tipoIdentificacion(cliente.tipoIdentificacion());
            receptorNumero = cliente.numeroIdentificacion();
            receptorRazon = cliente.nombreParaMostrar();
            receptorEmail = cliente.email();
        }
        return new Comprobante(null, empresa.id(), sucursalId, tipo, ESTAB, PTO_EMI, secuencial,
                ESTAB + "-" + PTO_EMI + "-" + secuencial, clave, fecha, ambiente, "NORMAL",
                empresa.ruc(), empresa.razonSocial(), empresa.nombreComercial(), empresa.direccionMatriz(),
                obligado ? "SI" : "NO", empresa.regimen(), receptorTipoId, receptorNumero, receptorRazon, receptorEmail,
                subtotal, descuento, iva, BigDecimal.ZERO, BigDecimal.ZERO, propina, total, "BORRADOR", ventaId, null,
                facturaProveedorId, null, null, null, null, numDocModificado, detalles, pagos);
    }

    private String codigoFormaPago(Long medioPagoId) {
        if (medioPagoId == null) {
            return "01";
        }
        return Bd.consulta(c -> {
            try (var ps = c.prepareStatement("SELECT codigo_sri_forma_pago FROM medio_pago WHERE id = ?")) {
                ps.setLong(1, medioPagoId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) {
                        return rs.getString(1);
                    }
                }
            }
            return "01";
        });
    }

    private Map<Long, BigDecimal> tarifasIva() {
        Map<Long, BigDecimal> mapa = new java.util.HashMap<>();
        for (Impuesto i : new ServicioImpuesto().listar()) {
            mapa.put(i.id(), i.porcentaje());
        }
        return mapa;
    }

    private Path dir() {
        Path dir = Path.of(System.getProperty("user.home"), ".sfjfx", "comprobantes");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear el directorio de comprobantes", e);
        }
        return dir;
    }

    private void escribir(Path destino, byte[] contenido) {
        try {
            Files.write(destino, contenido);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo escribir " + destino, e);
        }
    }

    private long sucursal() {
        var s = SesionActual.requerida().sucursal();
        return s == null ? 0 : s.id();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }

    private Long usuario() {
        return SesionActual.requerida().usuario().id();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String nvl(String v) {
        return v == null ? "" : v;
    }
}
