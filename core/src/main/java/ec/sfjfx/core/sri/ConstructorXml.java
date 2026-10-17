package ec.sfjfx.core.sri;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;
import ec.sfjfx.core.modelo.ComprobantePago;
import ec.sfjfx.core.modelo.RetencionDetalle;

/** Construye el XML de los comprobantes electronicos segun la estructura del SRI. */
public final class ConstructorXml {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ConstructorXml() {
    }

    public static String factura(Comprobante c) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<factura id=\"comprobante\" version=\"1.1.0\">");
        infoTributaria(x, c);
        x.append("<infoFactura>")
                .append(el("fechaEmision", c.fechaEmision().format(FECHA)))
                .append(el("dirEstablecimiento", nvl(c.emisorDireccion())))
                .append(el("obligadoContabilidad", "SI".equalsIgnoreCase(c.obligadoContabilidad()) ? "SI" : "NO"))
                .append(el("tipoIdentificacionComprador", nvl(c.receptorTipoId())))
                .append(el("razonSocialComprador", nvl(c.receptorRazonSocial())))
                .append(el("identificacionComprador", nvl(c.receptorNumeroId())))
                .append(el("totalSinImpuestos", din(c.subtotal())))
                .append(el("totalDescuento", din(c.descuento())));
        totalConImpuestos(x, c);
        x.append(el("propina", din(c.propina())))
                .append(el("importeTotal", din(c.total())))
                .append(el("moneda", "DOLAR"));
        pagos(x, c.pagos());
        x.append("</infoFactura>");
        detalles(x, c.detalles());
        infoAdicional(x, c);
        x.append("</factura>");
        return x.toString();
    }

    public static String notaCredito(Comprobante c) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<notaCredito id=\"comprobante\" version=\"1.1.0\">");
        infoTributaria(x, c);
        x.append("<infoNotaCredito>")
                .append(el("fechaEmision", c.fechaEmision().format(FECHA)))
                .append(el("tipoIdentificacionComprador", nvl(c.receptorTipoId())))
                .append(el("razonSocialComprador", nvl(c.receptorRazonSocial())))
                .append(el("identificacionComprador", nvl(c.receptorNumeroId())))
                .append(el("obligadoContabilidad", "SI".equalsIgnoreCase(c.obligadoContabilidad()) ? "SI" : "NO"))
                .append(el("codDocModificado", "01"))
                .append(el("numDocModificado", nvl(c.comprobanteRelacionadoClave())))
                .append(el("fechaEmisionDocSustento", c.fechaEmision().format(FECHA)))
                .append(el("totalSinImpuestos", din(c.subtotal())))
                .append(el("valorModificacion", din(c.total())))
                .append(el("moneda", "DOLAR"));
        totalConImpuestos(x, c);
        x.append("</infoNotaCredito>");
        detalles(x, c.detalles());
        x.append("</notaCredito>");
        return x.toString();
    }

    public static String notaDebito(Comprobante c) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<notaDebito id=\"comprobante\" version=\"1.0.0\">");
        infoTributaria(x, c);
        x.append("<infoNotaDebito>")
                .append(el("fechaEmision", c.fechaEmision().format(FECHA)))
                .append(el("tipoIdentificacionComprador", nvl(c.receptorTipoId())))
                .append(el("razonSocialComprador", nvl(c.receptorRazonSocial())))
                .append(el("identificacionComprador", nvl(c.receptorNumeroId())))
                .append(el("obligadoContabilidad", "SI".equalsIgnoreCase(c.obligadoContabilidad()) ? "SI" : "NO"))
                .append(el("codDocModificado", "01"))
                .append(el("numDocModificado", nvl(c.comprobanteRelacionadoClave())))
                .append(el("fechaEmisionDocSustento", c.fechaEmision().format(FECHA)))
                .append(el("totalSinImpuestos", din(c.subtotal())))
                .append(el("impuestos", ""));
        totalConImpuestos(x, c);
        x.append(el("valorTotal", din(c.total()))).append("</infoNotaDebito>");
        x.append("</notaDebito>");
        return x.toString();
    }

    public static String retencion(Comprobante c, List<RetencionDetalle> lineas) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<comprobanteRetencion id=\"comprobante\" version=\"1.0.0\">");
        infoTributaria(x, c);
        x.append("<infoCompRetencion>")
                .append(el("fechaEmision", c.fechaEmision().format(FECHA)))
                .append(el("obligadoContabilidad", "SI".equalsIgnoreCase(c.obligadoContabilidad()) ? "SI" : "NO"))
                .append(el("tipoIdentificacionSujetoRetenido", nvl(c.receptorTipoId())))
                .append(el("razonSocialSujetoRetenido", nvl(c.receptorRazonSocial())))
                .append(el("identificacionSujetoRetenido", nvl(c.receptorNumeroId())))
                .append(el("periodoFiscal", c.fechaEmision().format(DateTimeFormatter.ofPattern("MM/yyyy"))))
                .append("</infoCompRetencion>");
        x.append("<impuestos>");
        for (RetencionDetalle d : lineas) {
            x.append("<impuesto>")
                    .append(el("codigo", "IVA".equals(d.tipoImpuesto()) ? "2" : "1"))
                    .append(el("codigoRetencion", nvl(d.codigoRetencion())))
                    .append(el("baseImponible", din(d.baseImponible())))
                    .append(el("porcentajeRetener", din(d.porcentaje())))
                    .append(el("valorRetenido", din(d.valorRetenido())))
                    .append(el("codDocSustento", "01"))
                    .append(el("numDocSustento", "001001000000000"))
                    .append(el("fechaEmisionDocSustento", c.fechaEmision().format(FECHA)))
                    .append("</impuesto>");
        }
        x.append("</impuestos>");
        x.append("</comprobanteRetencion>");
        return x.toString();
    }

    public static String guiaRemision(Comprobante c) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<guiaRemision id=\"comprobante\" version=\"1.1.0\">");
        infoTributaria(x, c);
        x.append("<infoGuiaRemision>")
                .append(el("dirEstablecimiento", nvl(c.emisorDireccion())))
                .append(el("razonSocialTransportista", ""))
                .append(el("tipoIdentificacionTransportista", "04"))
                .append(el("rucTransportista", ""))
                .append(el("fechaIniTransporte", c.fechaEmision().format(FECHA)))
                .append(el("fechaFinTransporte", c.fechaEmision().format(FECHA)))
                .append(el("placa", ""))
                .append("</infoGuiaRemision>");
        x.append("</guiaRemision>");
        return x.toString();
    }

    public static String liquidacionCompra(Comprobante c) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.append("<liquidacionCompra id=\"comprobante\" version=\"1.1.0\">");
        infoTributaria(x, c);
        x.append("<infoLiquidacionCompra>")
                .append(el("fechaEmision", c.fechaEmision().format(FECHA)))
                .append(el("obligadoContabilidad", "SI".equalsIgnoreCase(c.obligadoContabilidad()) ? "SI" : "NO"))
                .append(el("tipoIdentificacionProveedor", nvl(c.receptorTipoId())))
                .append(el("razonSocialProveedor", nvl(c.receptorRazonSocial())))
                .append(el("identificacionProveedor", nvl(c.receptorNumeroId())))
                .append(el("totalSinImpuestos", din(c.subtotal())))
                .append(el("totalDescuento", din(c.descuento())));
        totalConImpuestos(x, c);
        x.append(el("importeTotal", din(c.total()))).append(el("moneda", "DOLAR")).append("</infoLiquidacionCompra>");
        detalles(x, c.detalles());
        x.append("</liquidacionCompra>");
        return x.toString();
    }

    // ------------------------------------------------------------------ bloques

    private static void infoTributaria(StringBuilder x, Comprobante c) {
        x.append("<infoTributaria>")
                .append(el("ambiente", CatalogoSri.ambiente(c.ambiente())))
                .append(el("tipoEmision", CatalogoSri.tipoEmision(c.tipoEmision())))
                .append(el("razonSocial", nvl(c.emisorRazonSocial())))
                .append(el("nombreComercial", nvl(c.emisorNombreComercial())))
                .append(el("ruc", nvl(c.emisorRuc())))
                .append(el("claveAcceso", nvl(c.claveAcceso())))
                .append(el("codDoc", CatalogoSri.codDoc(c.tipo())))
                .append(el("estab", nvl(c.establecimiento())))
                .append(el("ptoEmi", nvl(c.puntoEmision())))
                .append(el("secuencial", nvl(c.secuencial())))
                .append(el("dirMatriz", nvl(c.emisorDireccion())))
                .append("</infoTributaria>");
    }

    private static void totalConImpuestos(StringBuilder x, Comprobante c) {
        x.append("<totalConImpuestos>")
                .append("<totalImpuesto>")
                .append(el("codigo", "2"))
                .append(el("codigoPorcentaje", codigoIvaPredominante(c)))
                .append(el("baseImponible", din(c.subtotal())))
                .append(el("valor", din(c.iva())))
                .append("</totalImpuesto>")
                .append("</totalConImpuestos>");
    }

    private static String codigoIvaPredominante(Comprobante c) {
        BigDecimal tarifa = c.detalles().stream().map(ComprobanteDetalle::tarifa).filter(t -> t != null)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return CatalogoSri.codigoPorcentajeIva(tarifa);
    }

    private static void pagos(StringBuilder x, List<ComprobantePago> pagos) {
        x.append("<pagos>");
        if (pagos == null || pagos.isEmpty()) {
            x.append("<pago>").append(el("formaPago", "01")).append(el("total", "0.00")).append("</pago>");
        } else {
            for (ComprobantePago p : pagos) {
                x.append("<pago>")
                        .append(el("formaPago", p.formaPagoCodigo() == null ? "01" : p.formaPagoCodigo()))
                        .append(el("total", din(p.total())))
                        .append("</pago>");
            }
        }
        x.append("</pagos>");
    }

    private static void detalles(StringBuilder x, List<ComprobanteDetalle> detalles) {
        x.append("<detalles>");
        for (ComprobanteDetalle d : detalles) {
            x.append("<detalle>")
                    .append(el("codigoPrincipal", nvl(d.codigoPrincipal())))
                    .append(el("descripcion", nvl(d.descripcion())))
                    .append(el("cantidad", cant(d.cantidad())))
                    .append(el("precioUnitario", cant(d.precioUnitario())))
                    .append(el("descuento", din(d.descuento())))
                    .append(el("precioTotalSinImpuesto", din(d.subtotal())))
                    .append("<impuestos><impuesto>")
                    .append(el("codigo", "2"))
                    .append(el("codigoPorcentaje", CatalogoSri.codigoPorcentajeIva(d.tarifa())))
                    .append(el("tarifa", din(d.tarifa())))
                    .append(el("baseImponible", din(d.subtotal())))
                    .append(el("valor", din(d.impuestoValor())))
                    .append("</impuesto></impuestos>")
                    .append("</detalle>");
        }
        x.append("</detalles>");
    }

    private static void infoAdicional(StringBuilder x, Comprobante c) {
        x.append("<infoAdicional>");
        if (c.receptorEmail() != null && !c.receptorEmail().isBlank()) {
            x.append("<campoAdicional nombre=\"Email\">").append(escapar(c.receptorEmail())).append("</campoAdicional>");
        }
        if (c.regimen() != null && !c.regimen().isBlank()) {
            x.append("<campoAdicional nombre=\"Regimen\">").append(escapar(c.regimen())).append("</campoAdicional>");
        }
        x.append("</infoAdicional>");
    }

    // ------------------------------------------------------------------ helpers

    private static String el(String nombre, String valor) {
        return "<" + nombre + ">" + escapar(valor == null ? "" : valor) + "</" + nombre + ">";
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static String din(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String cant(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
