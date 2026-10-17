package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Dispositivo;
import ec.sfjfx.core.repositorio.DispositivoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

/** Administracion de dispositivos (impresoras, lector, balanza, gaveta). */
public final class ServicioDispositivo {

    public static final List<String> TIPOS = List.of(
            "IMPRESORA_TICKET", "IMPRESORA_A4", "LECTOR_BARRAS", "BALANZA", "GAVETA");

    private final DispositivoRepositorio repo = new DispositivoRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Dispositivo> listar() {
        return repo.porEmpresa(empresa());
    }

    public Dispositivo porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Dispositivo no encontrado"));
    }

    public Dispositivo guardar(Dispositivo d) {
        Validaciones.requerido(d.nombre(), "Nombre");
        Validaciones.requerido(d.tipo(), "Tipo");
        if (!TIPOS.contains(d.tipo())) {
            throw new ValidacionExcepcion("Tipo de dispositivo no valido: " + d.tipo());
        }
        Dispositivo dispositivo = new Dispositivo(d.id(), empresa(), d.sucursalId(), d.cajaId(),
                d.tipo(), d.nombre().trim(), json(d.configuracion()), d.activo());
        if (dispositivo.id() == null) {
            long id = repo.crear(dispositivo);
            auditoria.registrar("DISPOSITIVOS", "dispositivo", id, "CREAR", null,
                    Map.of("nombre", dispositivo.nombre(), "tipo", dispositivo.tipo()));
            return new Dispositivo(id, dispositivo.empresaId(), dispositivo.sucursalId(), dispositivo.cajaId(),
                    dispositivo.tipo(), dispositivo.nombre(), dispositivo.configuracion(), dispositivo.activo());
        }
        repo.actualizar(dispositivo);
        auditoria.registrar("DISPOSITIVOS", "dispositivo", dispositivo.id(), "EDITAR", null,
                Map.of("nombre", dispositivo.nombre(), "tipo", dispositivo.tipo()));
        return dispositivo;
    }

    public void eliminar(long id) {
        repo.eliminar(id, empresa());
        auditoria.registrar("DISPOSITIVOS", "dispositivo", id, "ELIMINAR", null, null);
    }

    /** Configuracion JSON de ejemplo segun el tipo de dispositivo. */
    public String configuracionSugerida(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "IMPRESORA_TICKET" -> "{\n  \"ancho_mm\": 80,\n  \"caracteres\": 48,\n  \"cortar\": true\n}";
            case "IMPRESORA_A4" -> "{\n  \"tamano\": \"A4\",\n  \"orientacion\": \"PORTRAIT\"\n}";
            case "LECTOR_BARRAS" -> "{\n  \"modo\": \"HID\",\n  \"sufijo\": \"ENTER\"\n}";
            case "BALANZA" -> "{\n  \"puerto\": \"/dev/ttyUSB0\",\n  \"baudios\": 9600,\n  \"formato\": \"PLU_PESO\"\n}";
            case "GAVETA" -> "{\n  \"pin\": 0,\n  \"abrir_al_cobrar\": true\n}";
            default -> "{}";
        };
    }

    private String json(String configuracion) {
        return configuracion == null || configuracion.isBlank() ? "{}" : configuracion.trim();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
