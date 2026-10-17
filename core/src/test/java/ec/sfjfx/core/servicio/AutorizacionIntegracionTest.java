package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

class AutorizacionIntegracionTest {

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
    void adminAutorizaConPinParaDescuentos() {
        ServicioAutorizacion.Autorizacion autorizacion =
                new ServicioAutorizacion().autorizar("admin", "1234", "pos.editar");
        assertEquals(SesionActual.requerida().usuario().id(), autorizacion.usuarioId());
    }

    @Test
    void pinIncorrectoEsRechazado() {
        assertThrows(ValidacionExcepcion.class,
                () -> new ServicioAutorizacion().autorizar("admin", "0000", "pos.editar"));
        assertThrows(ValidacionExcepcion.class,
                () -> new ServicioAutorizacion().autorizar("no-existe", "1234", "pos.editar"));
        assertThrows(ValidacionExcepcion.class,
                () -> new ServicioAutorizacion().autorizar("admin", "", "pos.editar"));
    }
}
