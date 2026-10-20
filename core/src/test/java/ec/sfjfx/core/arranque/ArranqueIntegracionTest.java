package ec.sfjfx.core.arranque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;

/**
 * Verifica que el arranque solo aplica migraciones y no crea empresas ni usuarios.
 *
 * <p>Se ejecuta en un esquema temporal para no tocar la base de demostracion. Se omite si no
 * hay configuracion local o la base no esta disponible.
 */
class ArranqueIntegracionTest {

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
    void migracionesEnBaseVaciaNoCreanEmpresasNiUsuarios() throws Exception {
        String esquema = "fase0_" + System.nanoTime();

        try (Connection c = ConexionBD.abrir()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE SCHEMA " + esquema);
                st.execute("SET search_path TO " + esquema);
            }

            List<String> historial = Arranque.ejecutarMigraciones(c);

            assertFalse(historial.isEmpty(), "Debe aplicar migraciones");
            assertEquals(0, contar(c, "empresa"), "El arranque no debe crear empresas");
            assertEquals(0, contar(c, "usuario"), "El arranque no debe crear usuarios");
            assertTrue(contar(c, "permiso") > 0, "Los datos base de V2 deben aplicarse");
            assertTrue(contar(c, "rol") > 0, "Los roles base de V2 deben aplicarse");
        } finally {
            try (Connection c = ConexionBD.abrir(); Statement st = c.createStatement()) {
                st.execute("SET search_path TO public");
                st.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
            }
        }
    }

    private static long contar(Connection c, String tabla) throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
