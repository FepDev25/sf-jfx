package ec.sfjfx.core.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.servicio.GestorModulos;

/**
 * Prueba de integracion: requiere la base de datos y la semilla de desarrollo.
 * Se omite (no falla) si no hay configuracion local o la BD no esta disponible.
 */
class AutenticacionIntegracionTest {

    @BeforeAll
    static void preparar() {
        Path config = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config),
                "Sin configuracion local; se omite la prueba de integracion");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");
    }

    @Test
    void adminIniciaSesionConEmpresaYPermisos() throws Exception {
        Sesion sesion = new ServicioAutenticacion().iniciar("admin", "admin");

        assertNotNull(sesion.empresa(), "Debe resolver una empresa");
        assertTrue(sesion.esSuperadmin(), "La semilla crea al admin como superadmin");
        assertFalse(sesion.permisos().isEmpty(), "Debe cargar permisos");
        assertTrue(sesion.tienePermiso("pos.ver"), "El admin puede ver POS");
    }

    @Test
    void claveIncorrectaNoAutentica() {
        assertThrows(AutenticacionExcepcion.class,
                () -> new ServicioAutenticacion().iniciar("admin", "clave-mala"));
    }

    @Test
    void losModulosVisiblesDependenDeFlagsYPermisos() throws Exception {
        Sesion sesion = new ServicioAutenticacion().iniciar("admin", "admin");
        var visibles = new GestorModulos().visibles(sesion, sesion.empresa().id());

        assertTrue(visibles.contains(ModuloSistema.POS), "POS debe estar activo y visible");
        assertTrue(visibles.size() >= 10, "Se esperan varios modulos activos");
    }
}
