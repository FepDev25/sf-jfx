package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.modelo.Respaldo;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
import ec.sfjfx.core.repositorio.RespaldoRepositorio;

/**
 * Respaldo desde consola: debe funcionar sin sesion de usuario, porque lo invoca la tarea
 * programada del sistema operativo con la aplicacion cerrada.
 */
class RespaldoConsolaIntegracionTest {

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
        esquema = "respaldo_" + System.nanoTime();
        directorio = Files.createTempDirectory("sfjfx-respaldo-");

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
                + "app.entorno=dev\n");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        // La prueba debe correr sin sesion de usuario: la cierro por si otra prueba la dejo abierta.
        ec.sfjfx.core.seguridad.SesionActual.cerrar();
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
    void generaRespaldoVerificadoSinSesionDeUsuario() throws Exception {
        // La instalacion debe existir para que el respaldo sea atribuible a una empresa.
        new ServicioInstalacion().instalar(datos());

        assertFalse(ec.sfjfx.core.seguridad.SesionActual.haySesion(),
                "La prueba debe correr sin sesion de usuario");

        ServicioRespaldo.RespaldoResultado resultado =
                new ServicioRespaldo().respaldarDesdeConsola();

        assertTrue(resultado.verificado(), resultado.detalle());
        assertTrue(resultado.tamanoBytes() > 0, "El archivo de respaldo no debe estar vacio");
        assertTrue(Files.isRegularFile(Path.of(resultado.ruta())), "Debe existir el archivo");

        // El respaldo queda registrado en el historial.
        List<Respaldo> historial = new RespaldoRepositorio().listar(
                new EmpresaRepositorio().primeraActiva().orElseThrow().id(), 5);
        assertTrue(historial.stream().anyMatch(r -> r.rutaArchivo().equals(resultado.ruta())),
                "Debe registrarse en el historial de respaldos");

        // Y se puede verificar de nuevo.
        assertTrue(new ServicioRespaldo().verificar(Path.of(resultado.ruta())).contains("verificado"));
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
                        "admin_respaldo", "Admin", "Respaldo", "admin@example.com", "0999999999",
                        "ClaveSegura123", "4321"));
    }
}
