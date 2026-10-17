package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.TransferenciaDetalle;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class InventarioIntegracionTest {

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
    void entradasCalculanPppSalidasYTransferenciasMuevenStock() {
        ServicioProducto productos = new ServicioProducto();
        ServicioInventario inventario = new ServicioInventario();
        ServicioAjuste ajustes = new ServicioAjuste();
        ServicioTransferencia transferencias = new ServicioTransferencia();

        long marca = System.currentTimeMillis();
        long productoId = productos.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "INV" + marca, "Producto inventario", null, null, null, null,
                        TipoProducto.SIMPLE, null, BigDecimal.ZERO, BigDecimal.ZERO, false, false, false,
                        null, null, null, null, null, true),
                List.of(), List.of(), Set.of(), List.of(), Map.of(), List.of(), List.of()));

        List<Bodega> bodegas = new BodegaRepositorio().porEmpresa(empresa());
        Bodega origen = bodegas.get(0);
        Bodega destino = bodegas.size() > 1 ? bodegas.get(1) : bodegas.get(0);

        // Entradas: 10 @ 2.00 y 10 @ 3.00 => PPP 2.50
        long aj1 = ajustes.aplicar(origen.id(), "ENTRADA", "INICIAL", "carga",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("10"), new BigDecimal("2.00"))));
        long aj2 = ajustes.aplicar(origen.id(), "ENTRADA", "CORRECCION", "carga",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("10"), new BigDecimal("3.00"))));

        BigDecimal promedio = productos.cargar(productoId).producto().costoPromedio();
        assertTrue(promedio.compareTo(new BigDecimal("2.5")) == 0, "PPP debe ser 2.50, fue " + promedio);

        assertEquals(0, cantidadEn(inventario, productoId, origen.id()).compareTo(new BigDecimal("20")));

        // Salida de 5 => quedan 15
        long aj3 = ajustes.aplicar(origen.id(), "SALIDA", "MERMA", "salida",
                List.of(new AjusteDetalle(null, 0, productoId, null, null, new BigDecimal("5"), BigDecimal.ZERO)));
        assertEquals(0, cantidadEn(inventario, productoId, origen.id()).compareTo(new BigDecimal("15")));

        assertTrue(inventario.kardex(productoId, null, 50).size() >= 3, "El kardex debe registrar los movimientos");

        // Transferencia de 3 al destino
        long transferencia = transferencias.crear(origen.sucursalId(), origen.id(), destino.sucursalId(), destino.id(),
                "prueba", List.of(new TransferenciaDetalle(null, 0, productoId, null, null, new BigDecimal("3"), BigDecimal.ZERO)));
        transferencias.enviar(transferencia);
        assertEquals(0, cantidadEn(inventario, productoId, origen.id()).compareTo(new BigDecimal("12")));
        transferencias.recibir(transferencia);
        assertEquals(0, cantidadEn(inventario, productoId, destino.id()).compareTo(new BigDecimal("3")));

        // Limpieza
        Bd.transaccion(c -> {
            ejecutar(c, "DELETE FROM movimiento_inventario WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM stock WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM ajuste_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM transferencia_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM toma_inventario_detalle WHERE producto_id = ?", productoId);
            ejecutar(c, "DELETE FROM ajuste_inventario WHERE id IN (" + aj1 + "," + aj2 + "," + aj3 + ")", -1);
            ejecutar(c, "DELETE FROM transferencia WHERE id = ?", transferencia);
            ejecutar(c, "DELETE FROM producto WHERE id = ?", productoId);
            return null;
        });
    }

    private BigDecimal cantidadEn(ServicioInventario inventario, long productoId, long bodegaId) {
        return inventario.stockProducto(productoId).stream()
                .filter(s -> s.bodegaId() == bodegaId)
                .map(s -> s.cantidad())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ejecutar(java.sql.Connection c, String sql, long id) throws java.sql.SQLException {
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
