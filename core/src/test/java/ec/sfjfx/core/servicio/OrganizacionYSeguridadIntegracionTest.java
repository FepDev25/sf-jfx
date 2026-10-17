package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.modelo.Usuario;
import ec.sfjfx.core.repositorio.SucursalRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.Sesion;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

class OrganizacionYSeguridadIntegracionTest {

    @BeforeAll
    static void prepararSesion() throws Exception {
        Path config = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config), "Sin configuracion local");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");

        Sesion sesion = new ServicioAutenticacion().iniciar("admin", "admin");
        SesionActual.iniciar(sesion);
    }

    @Test
    void cicloDeVidaDeUnRol() {
        ServicioRol servicio = new ServicioRol();
        String nombre = "PRUEBA_" + System.currentTimeMillis();

        Rol creado = servicio.guardar(
                new Rol(null, SesionActual.requerida().empresa().id(), nombre, "temporal", false, true),
                Set.of("pos.ver", "clientes.ver"));
        assertNotNull(creado.id());

        Set<String> permisos = servicio.permisosDe(creado.id());
        assertTrue(permisos.contains("pos.ver"), "Debe tener el permiso asignado");

        servicio.eliminar(creado);
    }

    @Test
    void empresaConRucInvalidoNoSeGuarda() {
        Empresa invalida = new Empresa(null, "123", "Prueba S.A.", null, null, null, null, null,
                false, true, "GENERAL", "USD", true);
        assertThrows(ValidacionExcepcion.class, () -> new ServicioEmpresa().guardar(invalida));
    }

    @Test
    void usuarioConNombreDuplicadoNoSeCrea() {
        Usuario duplicado = new Usuario(null, SesionActual.requerida().empresa().id(), "admin",
                "Duplicado", null, null, null, false, true);
        assertThrows(ValidacionExcepcion.class, () -> new ServicioUsuario().crear(duplicado, "clave1234"));
    }

    @Test
    void parametroSeGuardaYLee() {
        ServicioConfiguracion servicio = new ServicioConfiguracion();
        String clave = "prueba." + System.currentTimeMillis();
        servicio.guardar(clave, "valor-ok", "TEXTO", "prueba", "parametro de prueba");
        assertTrue("valor-ok".equals(servicio.valor(clave, "no")), "Debe leer el valor guardado");
        assertTrue(servicio.listar().stream().anyMatch(c -> c.valor().equals("valor-ok")));
    }

    @Test
    void registrarEnAuditoriaNoFalla() {
        new ServicioAuditoria().registrar("PRUEBA", "entidad_prueba", 1L, "EDITAR",
                Map.of("a", 1), Map.of("a", 2));
        assertTrue(!new ServicioAuditoria().listar(null, null, 5).isEmpty());
    }

    @Test
    void alcancePorSucursalSeRespeta() {
        long empresaId = SesionActual.requerida().empresa().id();
        long marca = System.currentTimeMillis();

        Sucursal sucursal = new ServicioSucursal().guardar(new Sucursal(
                null, empresaId, "TST" + (marca % 100000), "Sucursal prueba", null, null, false, true));
        Usuario usuario = new ServicioUsuario().crear(new Usuario(
                null, empresaId, "usr" + marca, "Usuario", "Prueba", null, null, false, true), "clave1234");
        new ServicioUsuario().asignarSucursales(usuario.id(), Set.of(sucursal.id()));

        var vistas = new SucursalRepositorio().porUsuario(usuario.id(), empresaId);
        assertTrue(vistas.size() == 1 && vistas.get(0).id() == sucursal.id(),
                "El usuario solo debe ver la sucursal asignada");

        // Limpieza
        Bd.transaccion(c -> {
            try (var ps = c.prepareStatement("DELETE FROM usuario_sucursal WHERE usuario_id = ?")) {
                ps.setLong(1, usuario.id());
                ps.executeUpdate();
            }
            try (var ps = c.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
                ps.setLong(1, usuario.id());
                ps.executeUpdate();
            }
            try (var ps = c.prepareStatement("DELETE FROM sucursal WHERE id = ?")) {
                ps.setLong(1, sucursal.id());
                ps.executeUpdate();
            }
            return null;
        });
    }
}
