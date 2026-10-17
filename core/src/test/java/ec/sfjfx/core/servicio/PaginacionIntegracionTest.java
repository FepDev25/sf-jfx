package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.repositorio.Pagina;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class PaginacionIntegracionTest {

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
    void productosPaginanEnElServidor() {
        ServicioProducto servicio = new ServicioProducto();
        String marca = String.valueOf(System.currentTimeMillis());
        String prefijo = "PAG" + marca;
        List<Long> ids = new ArrayList<>();
        try {
            for (int i = 1; i <= 7; i++) {
                long id = servicio.guardar(new ProductoCompleto(
                        new Producto(null, empresa(), prefijo + "-" + i, "Producto paginacion " + i, null,
                                null, null, null, TipoProducto.SIMPLE, null, BigDecimal.ZERO, BigDecimal.ZERO,
                                false, false, false, null, null, null, null, null, true),
                        List.of(), List.of(), Set.of(), List.of(), Map.of(), List.of(), List.of()));
                ids.add(id);
            }

            Pagina<Producto> primera = servicio.pagina(prefijo, 1, 3);
            assertEquals(7, primera.total());
            assertEquals(3, primera.items().size());
            assertEquals(3, primera.totalPaginas());
            assertEquals(1, primera.pagina());
            assertTrue(primera.haySiguiente());
            assertFalse(primera.hayAnterior());

            Pagina<Producto> segunda = servicio.pagina(prefijo, 2, 3);
            assertEquals(3, segunda.items().size());
            assertEquals(4, segunda.desde());
            assertEquals(6, segunda.hasta());

            Pagina<Producto> tercera = servicio.pagina(prefijo, 3, 3);
            assertEquals(1, tercera.items().size());
            assertFalse(tercera.haySiguiente());
            assertEquals(7, tercera.hasta());
        } finally {
            Bd.transaccion(c -> {
                for (Long id : ids) {
                    try (var ps = c.prepareStatement("DELETE FROM historial_precio WHERE producto_id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                    try (var ps = c.prepareStatement("DELETE FROM producto WHERE id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
                return null;
            });
        }
    }

    @Test
    void listadosGrandesRespondenPaginados() {
        Pagina<?> clientes = new ServicioCliente().pagina(null, 1, 2);
        assertNotNull(clientes);
        assertEquals(1, clientes.pagina());
        assertTrue(clientes.items().size() <= 2);
        assertTrue(clientes.total() >= clientes.items().size());

        Pagina<?> proveedores = new ServicioProveedor().pagina(null, 1, 2);
        assertNotNull(proveedores);
        assertTrue(proveedores.items().size() <= 2);

        Pagina<?> ventas = new ServicioVenta().pagina(null, 1, 2);
        assertNotNull(ventas);
        assertTrue(ventas.items().size() <= 2);

        Pagina<?> comprobantes = new ServicioComprobante().pagina(null, 1, 2);
        assertNotNull(comprobantes);
        assertTrue(comprobantes.items().size() <= 2);
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
