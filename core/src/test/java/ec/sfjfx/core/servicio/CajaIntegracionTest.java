package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.SesionCaja;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class CajaIntegracionTest {

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
    void aperturaCierreYArqueoSinDiferencia() {
        ServicioSesionCaja servicio = new ServicioSesionCaja();
        Caja caja = new CajaRepositorio().porEmpresa(empresa()).stream()
                .filter(c -> servicio.sesionAbierta(c.id()) == null)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(caja != null, "Todas las cajas tienen sesion abierta");

        long id = servicio.abrir(caja.id(), new BigDecimal("100.00"));
        try {
            SesionCaja abierta = servicio.porId(id);
            assertNotNull(abierta);
            assertEquals("ABIERTA", abierta.estado());

            SesionCaja cerrada = servicio.cerrar(id, Map.of(
                    new BigDecimal("50.00"), 2), "arqueo de prueba");
            assertFalse(cerrada.abierta());
            assertEquals(0, cerrada.montoInicial().compareTo(new BigDecimal("100.00")));
            assertEquals(0, cerrada.montoEsperado().compareTo(new BigDecimal("100.00")));
            assertEquals(0, cerrada.montoContado().compareTo(new BigDecimal("100.00")));
            assertEquals(0, cerrada.diferencia().compareTo(BigDecimal.ZERO));

            SesionCaja leida = servicio.porId(id);
            assertEquals("CERRADA", leida.estado());
            assertEquals(0, leida.diferencia().compareTo(BigDecimal.ZERO));
        } finally {
            Bd.transaccion(c -> {
                try (var ps = c.prepareStatement("DELETE FROM sesion_caja_arqueo WHERE sesion_caja_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                try (var ps = c.prepareStatement("DELETE FROM sesion_caja WHERE id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    @Test
    void cierreConFaltanteRegistraDiferenciaNegativa() {
        ServicioSesionCaja servicio = new ServicioSesionCaja();
        Caja caja = new CajaRepositorio().porEmpresa(empresa()).stream()
                .filter(c -> servicio.sesionAbierta(c.id()) == null)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(caja != null, "Todas las cajas tienen sesion abierta");

        long id = servicio.abrir(caja.id(), new BigDecimal("50.00"));
        try {
            SesionCaja cerrada = servicio.cerrar(id, Map.of(new BigDecimal("20.00"), 2), "faltante");
            assertEquals(0, cerrada.diferencia().compareTo(new BigDecimal("-10.0000")));
        } finally {
            Bd.transaccion(c -> {
                try (var ps = c.prepareStatement("DELETE FROM sesion_caja_arqueo WHERE sesion_caja_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                try (var ps = c.prepareStatement("DELETE FROM sesion_caja WHERE id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
