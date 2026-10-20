package ec.sfjfx.core.servicio;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.config.AppInfo;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.modelo.Respaldo;
import ec.sfjfx.core.repositorio.RespaldoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;

/** Estado consolidado de la instalacion para la pantalla de operacion. */
public final class ServicioEstadoInstalacion {

    public record Estado(
            String appVersion,
            String entorno,
            String dbVersion,
            String schemaVersion,
            boolean instalada,
            String instalacionNombre,
            String modo,
            String codigoInstalacion,
            String licenciaMensaje,
            boolean licenciaValida,
            boolean mantenimientoVigente,
            String ultimoRespaldo,
            String ultimoRespaldoEstado) {
    }

    public Estado estado() {
        ServicioInstalacion.Estado instalacion = new ServicioInstalacion().estado();
        ServicioLicencia licenciaServicio = new ServicioLicencia();
        ServicioLicencia.EstadoLicencia licencia = licenciaServicio.estado();

        String ultimoRespaldo = "Sin respaldos registrados";
        String ultimoRespaldoEstado = "-";
        if (SesionActual.haySesion()) {
            long empresaId = SesionActual.requerida().empresa().id();
            var ultimo = new RespaldoRepositorio().ultimo(empresaId);
            if (ultimo.isPresent()) {
                Respaldo respaldo = ultimo.get();
                ultimoRespaldo = respaldo.creadoEn() + " · " + respaldo.rutaArchivo();
                ultimoRespaldoEstado = respaldo.estado();
            }
        }

        return new Estado(
                AppInfo.version(),
                AppConfig.entorno(),
                ConexionBD.version(),
                instalacion.versionEsquema(),
                instalacion.instalada(),
                instalacion.nombre(),
                instalacion.modo(),
                licenciaServicio.codigoInstalacion(),
                licencia.mensaje(),
                licencia.valida(),
                licencia.mantenimientoVigente(),
                ultimoRespaldo,
                ultimoRespaldoEstado);
    }
}
