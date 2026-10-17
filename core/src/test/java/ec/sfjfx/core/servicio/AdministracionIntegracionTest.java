package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigracionInfo;
import ec.sfjfx.core.modelo.Configuracion;
import ec.sfjfx.core.modelo.Dispositivo;
import ec.sfjfx.core.modelo.Respaldo;
import ec.sfjfx.core.modulos.ModuloSistema;
import ec.sfjfx.core.modulos.PerfilInstalacion;
import ec.sfjfx.core.repositorio.ConfiguracionRepositorio;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class AdministracionIntegracionTest {

    @TempDir
    Path temp;

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
    void dispositivoSeCreaActualizaYElimina() {
        ServicioDispositivo servicio = new ServicioDispositivo();
        String nombre = "Impresora prueba " + System.currentTimeMillis();
        Dispositivo creado = servicio.guardar(new Dispositivo(null, empresa(), null, null,
                "IMPRESORA_TICKET", nombre, "{\"ancho_mm\":80}", true));
        try {
            assertNotNull(creado.id());
            assertTrue(servicio.listar().stream().anyMatch(d -> d.id().equals(creado.id())));
            assertEquals("IMPRESORA_TICKET", servicio.porId(creado.id()).tipo());

            servicio.guardar(new Dispositivo(creado.id(), empresa(), null, null,
                    "IMPRESORA_TICKET", nombre + " editada", "{\"ancho_mm\":58}", false));
            Dispositivo actualizado = servicio.porId(creado.id());
            assertEquals(nombre + " editada", actualizado.nombre());
            assertFalse(actualizado.activo());
            assertTrue(actualizado.configuracion().contains("58"));
        } finally {
            servicio.eliminar(creado.id());
        }
        assertTrue(servicio.listar().stream().noneMatch(d -> d.id().equals(creado.id())));
    }

    @Test
    void importaClientesYProveedoresConVistaPrevia() throws IOException {
        ServicioImportacion servicio = new ServicioImportacion();
        Path clientesCsv = temp.resolve("clientes.csv");
        Files.writeString(clientesCsv, String.join("\n",
                String.join(",", TipoImportacion.CLIENTES.cabecera()),
                "PERSONA_NATURAL,CEDULA,1710034073,Ana,Importada,,ana.import@demo.ec,0999999999,Calle 1,NO,0,0",
                "PERSONA_NATURAL,CEDULA,1710034073,Duplicada,Importada,,otra@demo.ec,,,NO,0,0",
                "PERSONA_NATURAL,CEDULA,,,,,,,NO,0,0") + "\n", StandardCharsets.UTF_8);

        ServicioImportacion.Analisis analisis = servicio.analizar(TipoImportacion.CLIENTES, clientesCsv);
        assertEquals(3, analisis.filas().size());
        assertTrue(analisis.errores().isEmpty());

        ServicioImportacion.Resultado resultado = servicio.importar(TipoImportacion.CLIENTES, clientesCsv);
        assertEquals(1, resultado.creados(), "Debe crear un cliente");
        assertEquals(1, resultado.omitidos(), "Debe omitir la identificacion duplicada");
        assertEquals(1, resultado.errores().size(), "La fila sin identificacion debe fallar");

        Path proveedoresCsv = temp.resolve("proveedores.csv");
        Files.writeString(proveedoresCsv, String.join("\n",
                String.join(",", TipoImportacion.PROVEEDORES.cabecera()),
                "CEDULA,1710034073,Proveedor Importado,Proveedor,Av. Demo,0999999999,prov@demo.ec,"
                        + "CONTRIBUYENTE_GENERAL,30,SI,NO,") + "\n", StandardCharsets.UTF_8);
        ServicioImportacion.Resultado proveedores = servicio.importar(TipoImportacion.PROVEEDORES, proveedoresCsv);
        assertEquals(1, proveedores.creados());
        try {
            assertNotNull(new ServicioCliente().listar("Importada").stream()
                    .filter(c -> "1710034073".equals(c.numeroIdentificacion())).findFirst().orElse(null));
            assertNotNull(new ServicioProveedor().listar("Proveedor Importado").stream()
                    .filter(p -> "1710034073".equals(p.numeroIdentificacion())).findFirst().orElse(null));
        } finally {
            Bd.transaccion(c -> {
                try (var ps = c.prepareStatement("DELETE FROM cliente WHERE numero_identificacion = ?")) {
                    ps.setString(1, "1710034073");
                    ps.executeUpdate();
                }
                try (var ps = c.prepareStatement("DELETE FROM proveedor WHERE numero_identificacion = ?")) {
                    ps.setString(1, "1710034073");
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    @Test
    void perfilAplicaModulosYSeRestaura() {
        long empresaId = empresa();
        GestorModulos gestor = new GestorModulos();
        Map<String, Boolean> original = gestor.estado(empresaId);
        ConfiguracionRepositorio configRepo = new ConfiguracionRepositorio();
        Optional<Configuracion> perfilOriginal = configRepo.porClave(empresaId, ServicioPerfiles.CLAVE_PERFIL);
        try {
            new ServicioPerfiles().aplicar(PerfilInstalacion.MINIMERCADO);
            assertTrue(gestor.activo(empresaId, ModuloSistema.POS));
            assertTrue(gestor.activo(empresaId, ModuloSistema.CONFIGURACION));
            assertFalse(gestor.activo(empresaId, ModuloSistema.GRANEL));
            assertEquals(PerfilInstalacion.MINIMERCADO, new ServicioPerfiles().perfilActual());
        } finally {
            for (ModuloSistema m : ModuloSistema.values()) {
                gestor.ajustar(empresaId, m, original.getOrDefault(m.codigo(), false));
            }
            Optional<Configuracion> actual = configRepo.porClave(empresaId, ServicioPerfiles.CLAVE_PERFIL);
            if (perfilOriginal.isPresent()) {
                Configuracion c = perfilOriginal.get();
                new ServicioConfiguracion().guardar(c.clave(), c.valor(), c.tipo(), c.grupo(), c.descripcion());
            } else {
                actual.ifPresent(c -> new ServicioConfiguracion().eliminar(c.id()));
            }
        }
    }

    @Test
    void migracionesInformanEstadoCompleto() {
        List<MigracionInfo> estado = new ServicioMigraciones().estado();
        assertFalse(estado.isEmpty());
        assertTrue(estado.stream().anyMatch(m -> "1".equals(m.version())));
        assertTrue(estado.stream().anyMatch(m -> "6".equals(m.version())));
        assertNotNull(estado.get(0).estado());
    }

    @Test
    void respaldoManualGeneraArchivoYRegistro() throws Exception {
        Assumptions.assumeTrue(enPath("pg_dump"), "pg_dump no disponible");
        ServicioRespaldo servicio = new ServicioRespaldo();
        ConfiguracionRepositorio configRepo = new ConfiguracionRepositorio();
        Optional<Configuracion> anterior = configRepo.porClave(empresa(), ServicioRespaldo.CLAVE_DIRECTORIO);
        Path directorio = temp.resolve("respaldos");
        servicio.guardarProgramacion(false, "23:00", directorio.toString());
        Respaldo respaldo = null;
        try {
            Respaldo creado = servicio.respaldar(false);
            respaldo = creado;
            assertTrue("OK".equals(creado.estado()), "Respaldo debe ser exitoso: " + creado.detalle());
            assertTrue(Files.isRegularFile(Path.of(creado.rutaArchivo())));
            assertTrue(Files.size(Path.of(creado.rutaArchivo())) > 0);
            assertTrue(servicio.historial(10).stream().anyMatch(r -> r.id().equals(creado.id())));

            Files.deleteIfExists(Path.of(creado.rutaArchivo()));
        } finally {
            if (respaldo != null && respaldo.id() != null) {
                servicio.eliminarRegistro(respaldo.id());
            }
            Optional<Configuracion> actual = configRepo.porClave(empresa(), ServicioRespaldo.CLAVE_DIRECTORIO);
            if (anterior.isPresent()) {
                Configuracion c = anterior.get();
                new ServicioConfiguracion().guardar(c.clave(), c.valor(), c.tipo(), c.grupo(), c.descripcion());
            } else {
                actual.ifPresent(c -> new ServicioConfiguracion().eliminar(c.id()));
            }
        }
    }

    private static boolean enPath(String comando) {
        try {
            Process proceso = new ProcessBuilder("which", comando).start();
            return proceso.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
