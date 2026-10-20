package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Properties;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.config.AppInfo;

/**
 * Canal de actualizacion remota asistida.
 *
 * <p>La aplicacion consulta un manifiesto firmado, verifica la firma y descarga el paquete
 * comprobando su SHA-256. La aplicacion del paquete la realiza el tecnico con el instalador o el
 * script de actualizacion, que respalda y aplica migraciones.
 */
public final class ServicioActualizacion {

    public static final String CLAVE_MANIFIESTO = "actualizacion.manifiesto";
    public static final String CLAVE_CLAVE_PUBLICA = "actualizacion.clave_publica";
    public static final String CLAVE_DIRECTORIO = "actualizacion.directorio";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Actualizacion(String version, String url, String sha256, String notas, String firma) {
        public String payload() {
            return "version=" + version + "\n"
                    + "url=" + url + "\n"
                    + "sha256=" + sha256 + "\n"
                    + "notas=" + (notas == null ? "" : notas);
        }
    }

    public record Estado(
            String versionActual,
            Actualizacion disponible,
            String mensaje,
            boolean hayActualizacion) {
    }

    public Estado consultar() {
        String origen = AppConfig.obtener(CLAVE_MANIFIESTO, null);
        if (origen == null || origen.isBlank()) {
            return new Estado(AppInfo.version(), null,
                    "No hay un canal de actualizaciones configurado (" + CLAVE_MANIFIESTO + ").", false);
        }
        try {
            String contenido = leer(origen.trim());
            Properties props = new Properties();
            try (var reader = new java.io.StringReader(contenido)) {
                props.load(reader);
            }
            Actualizacion actualizacion = new Actualizacion(
                    requerido(props, "version"),
                    requerido(props, "url"),
                    requerido(props, "sha256").toLowerCase(java.util.Locale.ROOT),
                    props.getProperty("notas", ""),
                    requerido(props, "firma"));

            PublicKey clave = cargarClavePublica();
            if (clave == null) {
                return new Estado(AppInfo.version(), null,
                        "Falta la clave publica de actualizaciones.", false);
            }
            if (!verificar(actualizacion, clave)) {
                return new Estado(AppInfo.version(), null,
                        "La firma del manifiesto de actualizacion no es valida.", false);
            }
            if (compararVersiones(actualizacion.version(), AppInfo.version()) <= 0) {
                return new Estado(AppInfo.version(), actualizacion,
                        "La aplicacion ya esta en la version mas reciente.", false);
            }
            return new Estado(AppInfo.version(), actualizacion,
                    "Actualizacion disponible: " + actualizacion.version(), true);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Estado(AppInfo.version(), null,
                    "No se pudo consultar el canal de actualizaciones: " + e.getMessage(), false);
        } catch (RuntimeException e) {
            return new Estado(AppInfo.version(), null,
                    "Manifiesto de actualizacion invalido: " + e.getMessage(), false);
        }
    }

    public Path descargar(Actualizacion actualizacion) {
        if (actualizacion == null) {
            throw new IllegalArgumentException("No hay una actualizacion seleccionada");
        }
        try {
            Path directorio = directorio();
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("sf-jfx-" + actualizacion.version() + ".zip");
            String url = actualizacion.url().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMinutes(5))
                        .GET()
                        .build();
                HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(destino));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    Files.deleteIfExists(destino);
                    throw new IOException("HTTP " + response.statusCode());
                }
            } else if (url.startsWith("file:")) {
                Files.copy(Path.of(URI.create(url)), destino, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(Path.of(url), destino, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = sha256(destino);
            if (!sha256.equalsIgnoreCase(actualizacion.sha256())) {
                Files.deleteIfExists(destino);
                throw new IllegalStateException("El SHA-256 del paquete no coincide con el manifiesto");
            }
            return destino;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("No se pudo descargar la actualizacion: " + e.getMessage(), e);
        }
    }

    public Path directorio() {
        String valor = AppConfig.obtener(CLAVE_DIRECTORIO, null);
        if (valor == null || valor.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".sfjfx", "actualizaciones");
        }
        String ruta = expandir(valor.trim());
        Path path = Path.of(ruta);
        return path.isAbsolute() ? path : Path.of(System.getProperty("user.home")).resolve(path);
    }

    private String leer(String origen) throws IOException, InterruptedException {
        if (origen.startsWith("http://") || origen.startsWith("https://")) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(origen))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return response.body();
        }
        if (origen.startsWith("file:")) {
            return Files.readString(Path.of(URI.create(origen)), StandardCharsets.UTF_8);
        }
        return Files.readString(Path.of(origen), StandardCharsets.UTF_8);
    }

    private String requerido(Properties props, String clave) {
        String valor = props.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo '" + clave + "' en el manifiesto");
        }
        return valor.trim();
    }

    private boolean verificar(Actualizacion actualizacion, PublicKey clave) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(clave);
            signature.update(actualizacion.payload().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(actualizacion.firma()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private PublicKey cargarClavePublica() {
        String configurada = AppConfig.obtener(CLAVE_CLAVE_PUBLICA, null);
        if (configurada == null || configurada.isBlank()) {
            configurada = AppConfig.obtener(ServicioLicencia.CLAVE_CLAVE_PUBLICA, null);
        }
        Path ruta = configurada == null || configurada.isBlank()
                ? Path.of(System.getProperty("user.home"), ".sfjfx", "licencia", "public-key.pem")
                : Path.of(expandir(configurada.trim()));
        if (!ruta.isAbsolute()) {
            ruta = Path.of(System.getProperty("user.home")).resolve(ruta);
        }
        if (!Files.isRegularFile(ruta)) {
            return null;
        }
        try {
            String pem = Files.readString(ruta, StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] bytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    private static String sha256(Path archivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var entrada = Files.newInputStream(archivo)) {
                byte[] buffer = new byte[8192];
                int leidos;
                while ((leidos = entrada.read(buffer)) >= 0) {
                    digest.update(buffer, 0, leidos);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static int compararVersiones(String a, String b) {
        String[] pa = a.replace("-SNAPSHOT", "").split("[.-]");
        String[] pb = b.replace("-SNAPSHOT", "").split("[.-]");
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = i < pa.length ? numero(pa[i]) : 0;
            int vb = i < pb.length ? numero(pb[i]) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int numero(String valor) {
        try {
            return Integer.parseInt(valor.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String expandir(String ruta) {
        return ruta.startsWith("~")
                ? System.getProperty("user.home") + ruta.substring(1)
                : ruta;
    }
}
