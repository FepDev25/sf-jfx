package ec.sfjfx.core.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.seguridad.ServicioAutenticacion;
import ec.sfjfx.core.seguridad.SesionActual;

class ReportesIntegracionTest {

    private static ServicioReportes reportes;

    @BeforeAll
    static void preparar() throws Exception {
        Path config = Path.of(System.getProperty("user.home"), ".sfjfx", "sfjfx.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config), "Sin configuracion local");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();
        Assumptions.assumeTrue(ConexionBD.probar(), "Base de datos no disponible");
        SesionActual.iniciar(new ServicioAutenticacion().iniciar("admin", "admin"));
        reportes = new ServicioReportes();
    }

    @Test
    void catalogoCubreTodosLosGrupos() {
        List<DefinicionReporte> catalogo = reportes.catalogo();
        assertTrue(catalogo.size() >= 20, "Debe haber reportes suficientes");
        for (GrupoReporte grupo : GrupoReporte.values()) {
            assertFalse(CatalogoReportes.porGrupo(grupo).isEmpty(), "Grupo sin reportes: " + grupo);
        }
    }

    @Test
    void todosLosReportesEjecutanConFiltros() {
        LocalDate hasta = LocalDate.now();
        FiltroReporte filtro = new FiltroReporte(hasta.minusDays(365), hasta, null, null, null);
        for (DefinicionReporte definicion : reportes.catalogo()) {
            ResultadoReporte resultado = reportes.ejecutar(definicion.codigo(), filtro);
            assertNotNull(resultado, definicion.codigo());
            assertEquals(definicion.codigo(), resultado.codigo());
            assertFalse(resultado.columnas().isEmpty(), definicion.codigo() + " sin columnas");
            assertEquals(resultado.columnas().size(), resultado.totales().size(), definicion.codigo());
            assertNotNull(resultado.filas(), definicion.codigo());
        }
    }

    @Test
    void todosLosReportesEjecutanConFiltrosAcotados() {
        long empresaId = SesionActual.requerida().empresa().id();
        Long sucursalId = new ec.sfjfx.core.repositorio.SucursalRepositorio().porEmpresa(empresaId)
                .stream().findFirst().map(s -> s.id()).orElse(null);
        Long categoriaId = new ec.sfjfx.core.repositorio.CategoriaRepositorio().listar(empresaId)
                .stream().findFirst().map(c -> c.id()).orElse(null);
        Long usuarioId = new ec.sfjfx.core.repositorio.UsuarioRepositorio().listar(empresaId)
                .stream().findFirst().map(u -> u.id()).orElse(null);
        FiltroReporte filtro = new FiltroReporte(LocalDate.now().minusDays(365), LocalDate.now(),
                sucursalId, categoriaId, usuarioId);
        for (DefinicionReporte definicion : reportes.catalogo()) {
            ResultadoReporte resultado = reportes.ejecutar(definicion.codigo(), filtro);
            assertNotNull(resultado.filas(), definicion.codigo());
        }
    }

    @Test
    void dashboardDevuelveIndicadoresYSenales() {
        PanelDashboard panel = reportes.dashboard();
        assertNotNull(panel);
        assertNotNull(panel.ventasHoy());
        assertNotNull(panel.cuentasPorCobrar());
        assertNotNull(panel.cuentasPorPagar());
        assertNotNull(panel.ventasUltimosDias());
        assertNotNull(panel.mediosDePago());
        assertNotNull(panel.topProductos());
    }

    @Test
    void reporteGuardadoSeListaYSeElimina() {
        String nombre = "Prueba " + System.currentTimeMillis();
        long id = reportes.guardar("ventas_por_dia", nombre, FiltroReporte.ultimosDias(7));
        try {
            assertTrue(reportes.guardados().stream().anyMatch(r -> r.id() == id && r.nombre().equals(nombre)));
        } finally {
            reportes.eliminarGuardado(id);
        }
        assertFalse(reportes.guardados().stream().anyMatch(r -> r.id() == id));
    }

    @Test
    void exportaCsvXlsxYPdf() throws Exception {
        ResultadoReporte resultado = reportes.ejecutar("ventas_por_dia", FiltroReporte.ultimosDias(30));

        byte[] csv = ExportadorReporte.csv(resultado);
        assertTrue(csv.length > 3);
        assertEquals((byte) 0xEF, csv[0]);

        byte[] xlsx = ExportadorReporte.xlsx(resultado);
        assertTrue(xlsx.length > 100);
        try (XSSFWorkbook libro = new XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            assertNotNull(libro.getSheetAt(0));
            assertTrue(libro.getSheetAt(0).getLastRowNum() >= 3);
            assertEquals("Fecha", libro.getSheetAt(0).getRow(3).getCell(0).getStringCellValue());
        }

        byte[] pdf = ExportadorReporte.pdf(resultado);
        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
