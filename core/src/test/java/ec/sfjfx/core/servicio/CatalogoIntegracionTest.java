package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.CodigoBarra;
import ec.sfjfx.core.modelo.HistorialPrecio;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoCodigoBarra;
import ec.sfjfx.core.modelo.TipoListaPrecio;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.UnidadMedida;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class CatalogoIntegracionTest {

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
    void cicloDeProductoConPrecioImpuestoYHistorial() {
        long marca = System.currentTimeMillis();
        ServicioProducto servicioProducto = new ServicioProducto();

        Categoria categoria = new ServicioCategoria().guardar(new Categoria(null, empresa(), null,
                "CAT" + marca, "Categoria prueba", null, 0, true));
        ListaPrecio lista = listaPublico();
        Long impuestoIva = new ServicioImpuesto().deVenta().stream()
                .filter(i -> i.tipo() == ec.sfjfx.core.modelo.TipoImpuesto.IVA && i.porcentaje().signum() > 0)
                .map(ec.sfjfx.core.modelo.Impuesto::id).findFirst().orElse(null);

        long productoId = servicioProducto.guardar(new ProductoCompleto(
                new Producto(null, empresa(), "SKU" + marca, "Producto prueba", null, categoria.id(), null,
                        unidadId(), TipoProducto.SIMPLE, null, new BigDecimal("2.00"), BigDecimal.ZERO,
                        false, false, false, null, null, null, null, null, true),
                List.of(new CodigoBarra(null, 0, "7861" + (marca % 100000000), TipoCodigoBarra.EAN13, true, null)),
                List.of(),
                impuestoIva == null ? Set.of() : Set.of(impuestoIva),
                List.of(),
                Map.of(),
                List.of(new PrecioProducto(null, lista.id(), lista.nombre(), 0, new BigDecimal("3.50"), null, null, null)),
                List.of()));

        ProductoCompleto cargado = servicioProducto.cargar(productoId);
        assertEquals(1, cargado.codigos().size());
        assertEquals(1, cargado.precios().size());
        assertTrue(cargado.precios().get(0).precio().compareTo(new BigDecimal("3.50")) == 0);

        // Cambia costo y precio -> debe registrar historial
        servicioProducto.guardar(new ProductoCompleto(
                new Producto(productoId, empresa(), "SKU" + marca, "Producto prueba", null, categoria.id(), null,
                        unidadId(), TipoProducto.SIMPLE, null, new BigDecimal("2.50"), BigDecimal.ZERO,
                        false, false, false, null, null, null, null, null, true),
                cargado.codigos(), List.of(), cargado.impuestoIds(), List.of(), Map.of(),
                List.of(new PrecioProducto(null, lista.id(), lista.nombre(), 0, new BigDecimal("3.95"), null, null, null)),
                List.of()));

        List<HistorialPrecio> historial = servicioProducto.historial(productoId);
        assertTrue(historial.stream().anyMatch(h -> h.tipo().equals("COSTO")), "Debe registrar cambio de costo");
        assertTrue(historial.stream().anyMatch(h -> h.tipo().equals("PRECIO")), "Debe registrar cambio de precio");

        // Limpieza
        Bd.transaccion(c -> {
            try (var ps = c.prepareStatement("DELETE FROM producto WHERE id = ?")) {
                ps.setLong(1, productoId);
                ps.executeUpdate();
            }
            try (var ps = c.prepareStatement("DELETE FROM categoria WHERE id = ?")) {
                ps.setLong(1, categoria.id());
                ps.executeUpdate();
            }
            return null;
        });
        assertFalse(servicioProducto.listar(null).stream().anyMatch(p -> p.id() == productoId));
    }

    private ListaPrecio listaPublico() {
        return new ServicioListaPrecio().activas().stream()
                .filter(l -> l.tipo() == TipoListaPrecio.PUBLICO).findFirst()
                .orElseGet(() -> new ServicioListaPrecio().guardar(new ListaPrecio(null, empresa(), "Publico",
                        TipoListaPrecio.PUBLICO, "USD", false, true, true)));
    }

    private Long unidadId() {
        return new ServicioUnidadMedida().listar().stream().map(UnidadMedida::id).findFirst().orElse(null);
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
