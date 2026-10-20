package ec.sfjfx.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.modelo.Licencia;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;
import ec.sfjfx.core.servicio.ServicioInstalacion;
import ec.sfjfx.core.servicio.ServicioLicencia;

/** Generacion y validacion offline de licencias sobre una instalacion temporal. */
class LicenciaIntegracionTest {

    private static String url;
    private static String usuario;
    private static String password;
    private static String esquema;
    private static Path directorio;
    private static String idInstalacion;

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
        esquema = "licencia_" + System.nanoTime();
        directorio = Files.createTempDirectory("sfjfx-licencia-");

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
                + "app.entorno=prod\n");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();

        new ServicioInstalacion().instalar(datosInstalacion());
        idInstalacion = new InstalacionRepositorio().obtener()
                .map(Instalacion::codigoUnico)
                .orElseThrow();
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
    void validaFirmaInstalacionYMantenimiento() throws Exception {
        Path publica = directorio.resolve("publica.pem");
        Path privada = directorio.resolve("privada.pem");
        LicenciaEmisor.generarClaves(publica, privada);

        Path valida = directorio.resolve("licencia-valida.properties");
        LicenciaEmisor.emitir(licencia(idInstalacion, LocalDate.now().plusYears(1)), privada, valida);
        configurarLicencia(publica, valida);

        ServicioLicencia servicio = new ServicioLicencia();
        ServicioLicencia.EstadoLicencia estado = servicio.estado();
        assertTrue(estado.valida(), estado.mensaje());
        assertTrue(estado.mantenimientoVigente());
        assertEquals("Cliente de prueba", estado.cliente());
        assertEquals("SUPERMERCADO", estado.edicion());
        assertEquals(2, estado.maxSucursales());
        assertEquals(4, estado.maxCajas());

        Path vencida = directorio.resolve("licencia-vencida.properties");
        LicenciaEmisor.emitir(licencia(idInstalacion, LocalDate.now().minusDays(1)), privada, vencida);
        configurarLicencia(publica, vencida);
        ServicioLicencia.EstadoLicencia vencidaEstado = servicio.estado();
        assertTrue(vencidaEstado.valida(), vencidaEstado.mensaje());
        assertFalse(vencidaEstado.mantenimientoVigente());
        assertTrue(vencidaEstado.mensaje().contains("vencido"));

        Path otraInstalacion = directorio.resolve("licencia-otra.properties");
        LicenciaEmisor.emitir(licencia("otra-instalacion", LocalDate.now().plusYears(1)), privada,
                otraInstalacion);
        configurarLicencia(publica, otraInstalacion);
        assertFalse(servicio.estado().valida());

        Path alterada = directorio.resolve("licencia-alterada.properties");
        Files.writeString(alterada,
                Files.readString(valida).replace("Cliente de prueba", "Cliente alterado"));
        configurarLicencia(publica, alterada);
        assertFalse(servicio.estado().valida());
    }

    private static Licencia licencia(String idInstalacion, LocalDate mantenimiento) {
        return new Licencia(1, idInstalacion, "Cliente de prueba", "1790011678001",
                "SUPERMERCADO", 2, 4, LocalDate.now(), mantenimiento, "");
    }

    private static void configurarLicencia(Path publica, Path archivo) {
        AppConfig.guardar(Map.of(
                "licencia.clave_publica", publica.toString(),
                "licencia.archivo", archivo.toString()));
    }

    private static ServicioInstalacion.DatosInstalacion datosInstalacion() {
        return new ServicioInstalacion.DatosInstalacion(
                "Instalacion licencia",
                Instalacion.MODO_UNICA_EMPRESA,
                new ServicioInstalacion.DatosEmpresa(
                        "1790011678001", "Empresa licencia S.A.", "Licencia", "GENERAL",
                        "Supermercado", "Quito", "022345678", "licencia@example.com", false, true),
                new ServicioInstalacion.DatosOrganizacion(
                        "001", "Matriz", "Quito", "022345678", "ALM", "Almacen", "CAJA01", "Caja 1"),
                new ServicioInstalacion.DatosSri("PRUEBAS"),
                new ServicioInstalacion.DatosAdmin(
                        "admin_licencia", "Admin", "Licencia", "admin@example.com", "0999999999",
                        "ClaveSegura123", "4321"));
    }
}
