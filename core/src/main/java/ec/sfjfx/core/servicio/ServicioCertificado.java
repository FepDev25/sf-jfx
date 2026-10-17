package ec.sfjfx.core.servicio;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.CertificadoFirma;
import ec.sfjfx.core.repositorio.CertificadoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Gestion del certificado de firma electronica (.p12). */
public final class ServicioCertificado {

    private final CertificadoRepositorio repo = new CertificadoRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<CertificadoFirma> listar() {
        return repo.listar(empresa());
    }

    public CertificadoFirma activo() {
        return repo.activo(empresa()).orElse(null);
    }

    public static char[] descifrar(String almacenado) {
        return almacenado == null ? new char[0] : new String(Base64.getDecoder().decode(almacenado)).toCharArray();
    }

    public long guardar(String alias, String rutaP12, String password) {
        if (rutaP12 == null || rutaP12.isBlank()) {
            throw new ValidacionExcepcion("Indique el archivo del certificado (.p12)");
        }
        Path ruta = Path.of(rutaP12);
        if (!Files.isRegularFile(ruta)) {
            throw new ValidacionExcepcion("El archivo del certificado no existe");
        }
        LocalDate desde;
        LocalDate hasta;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(ruta)) {
                ks.load(in, password.toCharArray());
            }
            String primerAlias = ks.aliases().nextElement();
            X509Certificate cert = (X509Certificate) ks.getCertificate(primerAlias);
            desde = aFecha(cert.getNotBefore());
            hasta = aFecha(cert.getNotAfter());
        } catch (Exception e) {
            throw new ValidacionExcepcion("No se pudo abrir el certificado (revise la contrasena): " + e.getMessage());
        }
        CertificadoFirma cf = new CertificadoFirma(null, empresa(), alias, ruta.toString(),
                Base64.getEncoder().encodeToString(password.getBytes()), desde, hasta, true);
        long id = repo.crear(cf);
        auditoria.registrar("FACTURACION", "certificado", id, "CREAR", null, Map.of("alias", alias));
        return id;
    }

    public void activar(long id) {
        repo.activar(empresa(), id);
        auditoria.registrar("FACTURACION", "certificado", id, "EDITAR", null, Map.of("activo", true));
    }

    public void eliminar(long id) {
        repo.eliminar(id);
        auditoria.registrar("FACTURACION", "certificado", id, "ELIMINAR", null, null);
    }

    private LocalDate aFecha(java.util.Date fecha) {
        return Instant.ofEpochMilli(fecha.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
