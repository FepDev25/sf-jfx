package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.AjusteDetalle;
import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.Cupon;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.ProgramaFidelidad;
import ec.sfjfx.core.modelo.Promocion;
import ec.sfjfx.core.modelo.PromocionAlcance;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.modelo.TipoImpuesto;
import ec.sfjfx.core.modelo.TipoListaPrecio;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.repositorio.MedioPagoRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class FidelidadPromocionesIntegracionTest {

    @BeforeAll
    static void preparar() throws Exception {
        Path config = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config), "Sin configuracion local");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");
        SesionActual.iniciar(new ServicioAutenticacion().iniciar("admin", "admin"));
    }

    @Test
    void fidelidadPromocionesCuponesYPrecioMasivo() {
        long marca = System.currentTimeMillis();
        ServicioProducto productos = new ServicioProducto();
        ServicioAjuste ajustes = new ServicioAjuste();
        ServicioVenta ventas = new ServicioVenta();
        ServicioSesionCaja sesiones = new ServicioSesionCaja();
        ServicioFidelidad fidelidad = new ServicioFidelidad();
        ServicioPromocion promociones = new ServicioPromocion();
        ServicioCupon cupones = new ServicioCupon();
        ServicioPrecioMasivo masivo = new ServicioPrecioMasivo();

        // Programa de fidelidad
        fidelidad.guardarPrograma(new ProgramaFidelidad(null, empresa(), "Puntos", true, BigDecimal.ONE,
                new BigDecimal("0.01"), null));

        long categoriaId = new ServicioCategoria().guardar(new Categoria(null, empresa(), null, "CAT" + marca,
                "Categoria promo", null, 0, true)).id();
        long listaId = new ServicioListaPrecio().guardar(new ListaPrecio(null, empresa(), "Lista" + marca,
                TipoListaPrecio.PUBLICO, "USD", false, false, true)).id();

        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "PRO" + marca, "Producto promo", null, categoriaId, null, null,
                        TipoProducto.SIMPLE, null, new BigDecimal("2.00"), BigDecimal.ZERO, false, false, false,
                        null, null, null, null, null, true),
                List.of(), List.of(), Set.of(), List.of(), Map.of(),
                List.of(new PrecioProducto(null, listaId, null, 0, new BigDecimal("10.00"), null, null, null)),
                List.of()));
        Bodega bodega = new ec.sfjfx.core.repositorio.BodegaRepositorio().porEmpresa(empresa()).get(0);
        ajustes.aplicar(bodega.id(), "ENTRADA", "INICIAL", "carga",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("20"), new BigDecimal("2.00"))));

        // Promocion 10% sobre la categoria
        promociones.guardar(new Promocion(null, empresa(), "Promo 10%", null, "PORCENTAJE", new BigDecimal("10"),
                null, null, 5, false, LocalDate.now(), LocalDate.now().plusDays(30), "ACTIVA", true,
                List.of(new PromocionAlcance(null, 0, "CATEGORIA", categoriaId)), List.of()));
        var oferta = promociones.evaluar(productoId, categoriaId, null, new BigDecimal("2"), new BigDecimal("10.00"), null);
        assertTrue(oferta.isPresent());
        assertEquals(0, oferta.get().descuento().compareTo(new BigDecimal("2.00")));

        // Cupon 10%
        long cuponId = cupones.guardar(new Cupon(null, empresa(), "CUP" + marca, "PORCENTAJE", new BigDecimal("10"),
                LocalDate.now(), LocalDate.now().plusDays(30), 100, 0, true)).id();
        assertEquals(0, cupones.validarYCalcular("CUP" + marca, new BigDecimal("50")).compareTo(new BigDecimal("5.00")));

        // Precio masivo +10%
        int afectados = masivo.aplicar(listaId, categoriaId, new BigDecimal("10"));
        assertTrue(afectados >= 1);
        assertEquals(0, productos.cargar(productoId).precios().get(0).precio().compareTo(new BigDecimal("11.0000")));

        // Venta acumula puntos; canje reduce
        long clienteId = new ServicioCliente().guardar(new Cliente(null, empresa(), "PERSONA_NATURAL", "CEDULA",
                "1710034065", "Cliente", "Puntos", null, null, null, null, false, BigDecimal.ZERO, 0,
                BigDecimal.ZERO, true)).id();
        Caja caja = new CajaRepositorio().porEmpresa(empresa()).get(0);
        SesionCaja sesion = sesiones.sesionAbierta(caja.id());
        boolean abrimos = false;
        if (sesion == null) {
            sesion = sesiones.porId(sesiones.abrir(caja.id(), BigDecimal.ZERO));
            abrimos = true;
        }
        Long ivaId = new ServicioImpuesto().deVenta().stream()
                .filter(i -> i.tipo() == TipoImpuesto.IVA && i.porcentaje().signum() > 0)
                .map(Impuesto::id).findFirst().orElseThrow();
        Long efectivoId = new MedioPagoRepositorio().listar().stream()
                .filter(m -> "EFECTIVO".equals(m.tipo())).map(MedioPagoRepositorio.MedioPago::id).findFirst().orElseThrow();

        long ventaId = ventas.cobrar(sesion, bodega.id(), null, clienteId, "puntos", false,
                List.of(new VentaDetalle(null, 0, productoId, "Producto", new BigDecimal("1"), new BigDecimal("10.00"),
                        BigDecimal.ZERO, ivaId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                List.of(new VentaPago(null, 0, efectivoId, "Efectivo", new BigDecimal("11.50"), null, BigDecimal.ZERO)),
                BigDecimal.ZERO, BigDecimal.ZERO, null);
        BigDecimal saldoPuntos = fidelidad.saldo(clienteId);
        assertEquals(0, saldoPuntos.compareTo(new BigDecimal("11.50")), "Debe acumular 11.50 puntos");

        fidelidad.canjear(clienteId, new BigDecimal("0.05")); // 5 puntos
        assertEquals(0, fidelidad.saldo(clienteId).compareTo(new BigDecimal("6.50")));

        // Limpieza
        Bd.transaccion(c -> {
            ejecutar(c, "DELETE FROM puntos_movimiento WHERE cliente_id = ?", clienteId);
            ejecutar(c, "DELETE FROM cliente_puntos WHERE cliente_id = ?", clienteId);
            ejecutar(c, "DELETE FROM promocion_alcance WHERE promocion_id IN (SELECT id FROM promocion WHERE empresa_id = " + empresa() + ")", -1);
            ejecutar(c, "DELETE FROM promocion_detalle WHERE promocion_id IN (SELECT id FROM promocion WHERE empresa_id = " + empresa() + ")", -1);
            ejecutar(c, "DELETE FROM promocion WHERE nombre = 'Promo 10%'", -1);
            ejecutar(c, "DELETE FROM cupon WHERE id = ?", cuponId);
            ejecutar(c, "DELETE FROM venta_pago WHERE venta_id = ?", ventaId);
            ejecutar(c, "DELETE FROM venta_detalle WHERE venta_id = ?", ventaId);
            ejecutar(c, "DELETE FROM venta WHERE id = ?", ventaId);
            ejecutar(c, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM historial_precio WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM ajuste_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM precio_producto WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM producto WHERE id = ?", productoId);
            ejecutar(c, "DELETE FROM categoria WHERE id = ?", categoriaId);
            ejecutar(c, "DELETE FROM lista_precio WHERE id = ?", listaId);
            ejecutar(c, "DELETE FROM cliente WHERE id = ?", clienteId);
            return null;
        });
        if (abrimos) {
            sesiones.cerrar(sesion.id(), Map.of(), "fin de prueba");
        }
    }

    private void ejecutar(Connection c, String sql, long id) throws SQLException {
        try (var ps = c.prepareStatement(sql)) {
            if (sql.contains("?")) {
                ps.setLong(1, id);
            }
            ps.executeUpdate();
        }
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
