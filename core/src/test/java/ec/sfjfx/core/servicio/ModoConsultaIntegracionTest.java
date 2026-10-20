package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;
import ec.sfjfx.core.servicio.ServicioLicencia.EstadoLicencia;
import ec.sfjfx.core.util.ValidacionExcepcion;

/**
 * Modo consulta: se activa cuando no hay licencia valida y termina el periodo de prueba, o cuando
 * la licencia esta vencida y se acaba el periodo de gracia.
 */
class ModoConsultaIntegracionTest {

    private static String url;
    private static String usuario;
    private static String password;
    private static String esquema;
    private static Path directorio;

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
        esquema = "consulta_" + System.nanoTime();
        directorio = Files.createTempDirectory("sfjfx-consulta-");

        try (Connection c = DriverManager.getConnection(url, usuario, password);
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + esquema);
        }

        Path config = directorio.resolve("sfjfx.properties");
        Files.writeString(config,
                "db.url=" + url + "\n"
                + "db.usuario=" + usuario + "\n"
                + "db.password=" + password + "\n"
                + "db.esquema=" + esquema + "\n"
                + "app.entorno=prod\n"
                + "licencia.prueba.dias=30\n"
                + "licencia.gracia.dias=30\n");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();

        new ServicioInstalacion().instalar(datos());
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
    void sinLicenciaEnPeriodoDePruebaOperaYLuegoPasaAModoConsulta() {
        ServicioLicencia servicio = new ServicioLicencia();
        servicio.invalidarCache();

        EstadoLicencia enPrueba = servicio.estado();
        assertFalse(enPrueba.valida(), "Sin archivo de licencia no debe ser valida");
        assertFalse(enPrueba.modoConsulta(), "Durante el periodo de prueba no debe bloquear");

        // Envejecemos la instalacion mas alla del periodo de prueba.
        envejecerInstalacion(60);

        servicio.invalidarCache();
        EstadoLicencia vencida = servicio.estado();
        assertTrue(vencida.modoConsulta(), "Pasado el periodo de prueba debe entrar en modo consulta");
        assertTrue(vencida.motivo() != null && !vencida.motivo().isBlank());

        ValidacionExcepcion error = org.junit.jupiter.api.Assertions.assertThrows(
                ValidacionExcepcion.class, () -> servicio.exigirOperacion("cobrar una venta"));
        assertTrue(error.getMessage().contains("modo consulta"));
    }

    @Test
    void saludReportaInstalacionRegistrada() {
        ServicioSalud.Estado estado = new ServicioSalud().comprobar();
        assertTrue(estado.version() != null && !estado.version().isBlank());
        assertTrue(estado.instalacion().contains("Instalacion de prueba"),
                "Debe reportar la instalacion: " + estado.instalacion());
    }

    private void envejecerInstalacion(int dias) {
        try (Connection c = DriverManager.getConnection(url, usuario, password);
             Statement st = c.createStatement()) {
            st.execute("SET search_path TO " + esquema);
            st.execute("UPDATE instalacion SET creado_en = creado_en - INTERVAL '" + dias + " days'");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo envejecer la instalacion: " + e.getMessage(), e);
        }
    }

    private static ec.sfjfx.core.servicio.ServicioInstalacion.DatosInstalacion datos() {
        return new ec.sfjfx.core.servicio.ServicioInstalacion.DatosInstalacion(
                "Instalacion de prueba",
                Instalacion.MODO_UNICA_EMPRESA,
                new ec.sfjfx.core.servicio.ServicioInstalacion.DatosEmpresa(
                        "1790011678001", "Empresa de prueba S.A.", "Prueba", "GENERAL",
                        "Supermercado", "Quito", "022345678", "empresa@example.com", false, true),
                new ec.sfjfx.core.servicio.ServicioInstalacion.DatosOrganizacion(
                        "001", "Matriz", "Quito", "022345678", "ALM", "Almacen", "CAJA01", "Caja 1"),
                new ec.sfjfx.core.servicio.ServicioInstalacion.DatosSri("PRUEBAS"),
                new ec.sfjfx.core.servicio.ServicioInstalacion.DatosAdmin(
                        "admin_consulta", "Admin", "Consulta", "admin@example.com", "0999999999",
                        "ClaveSegura123", "4321"));
    }
}
