package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.util.ValidacionExcepcion;

/**
 * Integracion del primer arranque sobre un esquema temporal aislado.
 *
 * <p>Verifica que el asistente cree empresa, sucursal, bodega, caja, administrador, modulos,
 * secuencias y configuracion sin cargar datos de demostracion.
 */
class InstalacionIntegracionTest {

    private static String url;
    private static String usuario;
    private static String password;
    private static String esquema;

    @BeforeAll
    static void preparar() throws Exception {
        Path local = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(local),
                "Sin configuracion local; se omite la prueba de integracion");
        System.setProperty("sfjfx.config", local.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");

        url = AppConfig.obtener("db.url");
        usuario = AppConfig.obtener("db.usuario");
        password = AppConfig.obtener("db.password");
        esquema = "fase1_" + System.nanoTime();

        try (Connection c = DriverManager.getConnection(url, usuario, password);
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + esquema);
        }

        Path config = Files.createTempFile("sfjfx-fase1-", ".properties");
        Files.writeString(config,
                "db.url=" + url + "\n"
                + "db.usuario=" + usuario + "\n"
                + "db.password=" + password + "\n"
                + "db.esquema=" + esquema + "\n"
                + "app.entorno=prod\n");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
    }

    @AfterAll
    static void limpiar() throws Exception {
        if (url != null && esquema != null) {
            try (Connection c = DriverManager.getConnection(url, usuario, password);
                 Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
            }
        }
        System.clearProperty("sfjfx.config");
        AppConfig.recargar();
    }

    @Test
    void instalaUnaBaseVaciaSinDatosDemo() throws Exception {
        ServicioInstalacion servicio = new ServicioInstalacion();

        assertTrue(servicio.requiereInstalacion(), "Una base vacia requiere instalacion");
        servicio.prepararEsquema();
        assertTrue(servicio.requiereInstalacion(), "Migrar no crea empresas ni usuarios");

        ServicioInstalacion.ResultadoInstalacion resultado = servicio.instalar(datos());
        assertTrue(resultado.empresaId() > 0);
        assertTrue(resultado.usuarioId() > 0);

        assertFalse(servicio.requiereInstalacion());
        assertEquals(1, contar("empresa"));
        assertEquals(1, contar("usuario"));
        assertEquals(1, contar("sucursal"));
        assertEquals(2, contar("bodega"), "Bodega de almacen y bodega de venta");
        assertEquals(1, contar("caja"));
        assertEquals(1, contar("instalacion"));
        assertEquals(1, contar("lista_precio"), "Debe crear una lista de precio publica por defecto");
        assertEquals(0, contar("producto"), "No debe cargar catalogo de demostracion");
        assertEquals(6, contar("secuencia_documento"));
        assertTrue(contar("empresa_modulo") > 0);

        Instalacion instalacion = new InstalacionRepositorio().obtener().orElseThrow();
        assertEquals("Instalacion de prueba", instalacion.nombre());
        assertEquals(Instalacion.MODO_UNICA_EMPRESA, instalacion.modo());
        assertTrue(instalacion.codigoUnico() != null && !instalacion.codigoUnico().isBlank(),
                "Debe tener un codigo unico para licencias");
        assertTrue(Integer.parseInt(instalacion.versionEsquema()) >= 8,
                "Debe registrar la version de esquema aplicada");
        assertFalse(servicio.permiteMultiempresa());

        var sesion = new ServicioAutenticacion().iniciar("admin_prueba", "ClaveSegura123");
        assertTrue(sesion.esSuperadmin());
        assertEquals("Empresa de prueba S.A.", sesion.empresa().razonSocial());

        assertThrows(ValidacionExcepcion.class, () -> servicio.instalar(datos()));
    }

    private static ServicioInstalacion.DatosInstalacion datos() {
        return new ServicioInstalacion.DatosInstalacion(
                "Instalacion de prueba",
                Instalacion.MODO_UNICA_EMPRESA,
                new ServicioInstalacion.DatosEmpresa(
                        "1790011678001",
                        "Empresa de prueba S.A.",
                        "Prueba",
                        "GENERAL",
                        "Supermercado",
                        "Av. Principal y Calle 1, Quito",
                        "022345678",
                        "empresa@example.com",
                        false,
                        true),
                new ServicioInstalacion.DatosOrganizacion(
                        "001",
                        "Matriz",
                        "Av. Principal y Calle 1, Quito",
                        "022345678",
                        "ALM",
                        "Almacen principal",
                        "CAJA01",
                        "Caja 1"),
                new ServicioInstalacion.DatosSri("PRUEBAS"),
                new ServicioInstalacion.DatosAdmin(
                        "admin_prueba",
                        "Administrador",
                        "de Prueba",
                        "admin@example.com",
                        "0999999999",
                        "ClaveSegura123",
                        "4321"));
    }

    private static long contar(String tabla) {
        return Bd.consulta(c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }
}
