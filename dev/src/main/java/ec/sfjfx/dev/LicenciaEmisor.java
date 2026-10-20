package ec.sfjfx.dev;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;

import ec.sfjfx.core.modelo.Licencia;

/**
 * Herramienta interna para generar el par de claves de licencia y emitir licencias.
 *
 * <p>No forma parte del producto. La clave privada debe custodiarse fuera del repositorio y nunca
 * distribuirse con la aplicacion. La clave publica se configura en la instalacion con
 * {@code licencia.clave_publica}.
 */
public final class LicenciaEmisor {

    private LicenciaEmisor() {
    }

    public static void generarClaves(Path publica, Path privada) throws GeneralSecurityException, IOException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        escribirPem(publica, "PUBLIC KEY", par.getPublic().getEncoded());
        escribirPem(privada, "PRIVATE KEY", par.getPrivate().getEncoded());
    }

    public static Licencia emitir(Licencia licencia, Path privada, Path salida)
            throws GeneralSecurityException, IOException {
        PrivateKey clave = leerPrivada(privada);
        Signature firma = Signature.getInstance("SHA256withRSA");
        firma.initSign(clave);
        firma.update(licencia.payload().getBytes(StandardCharsets.UTF_8));
        String valorFirma = Base64.getEncoder().encodeToString(firma.sign());

        Licencia firmada = new Licencia(
                licencia.versionLicencia(),
                licencia.idInstalacion(),
                licencia.cliente(),
                licencia.ruc(),
                licencia.edicion(),
                licencia.maxSucursales(),
                licencia.maxCajas(),
                licencia.emitidaEn(),
                licencia.mantenimientoHasta(),
                valorFirma);

        Properties props = new Properties();
        props.setProperty("versionLicencia", Integer.toString(firmada.versionLicencia()));
        props.setProperty("idInstalacion", firmada.idInstalacion());
        props.setProperty("cliente", firmada.cliente());
        props.setProperty("ruc", firmada.ruc() == null ? "" : firmada.ruc());
        props.setProperty("edicion", firmada.edicion());
        props.setProperty("maxSucursales", Integer.toString(firmada.maxSucursales()));
        props.setProperty("maxCajas", Integer.toString(firmada.maxCajas()));
        props.setProperty("emitidaEn", firmada.emitidaEn().toString());
        props.setProperty("mantenimientoHasta", firmada.mantenimientoHasta().toString());
        props.setProperty("firma", firmada.firma());

        if (salida.getParent() != null) {
            Files.createDirectories(salida.getParent());
        }
        StringWriter texto = new StringWriter();
        props.store(texto, "sf-jfx - licencia");
        Files.writeString(salida, texto.toString(), StandardCharsets.UTF_8);
        return firmada;
    }

    private static void escribirPem(Path ruta, String tipo, byte[] contenido) throws IOException {
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(contenido);
        String pem = "-----BEGIN " + tipo + "-----\n" + base64
                + "\n-----END " + tipo + "-----\n";
        Files.writeString(ruta, pem, StandardCharsets.UTF_8);
    }

    private static PrivateKey leerPrivada(Path ruta) throws IOException, GeneralSecurityException {
        String pem = Files.readString(ruta, StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            uso();
            return;
        }
        switch (args[0]) {
            case "generar-claves" -> {
                if (args.length != 3) {
                    uso();
                    return;
                }
                generarClaves(Path.of(args[1]), Path.of(args[2]));
                System.out.println("Clave publica: " + args[1]);
                System.out.println("Clave privada: " + args[2]);
                System.out.println("Configure licencia.clave_publica=" + args[1]
                        + " en la instalacion y custodie la clave privada fuera del repositorio.");
            }
            case "emitir" -> {
                if (args.length != 10) {
                    uso();
                    return;
                }
                Licencia licencia = new Licencia(
                        1,
                        args[3],
                        args[4],
                        args[5],
                        args[6].toUpperCase(java.util.Locale.ROOT),
                        Integer.parseInt(args[7]),
                        Integer.parseInt(args[8]),
                        LocalDate.now(),
                        LocalDate.parse(args[9]),
                        "");
                Path salida = Path.of(args[2]);
                emitir(licencia, Path.of(args[1]), salida);
                System.out.println("Licencia emitida: " + salida.toAbsolutePath());
                System.out.println("Instalacion: " + licencia.idInstalacion());
                System.out.println("Mantenimiento hasta: " + licencia.mantenimientoHasta());
            }
            default -> uso();
        }
    }

    private static void uso() {
        System.out.println("Uso:");
        System.out.println("  generar-claves <publica.pem> <privada.pem>");
        System.out.println("  emitir <privada.pem> <salida.properties> <idInstalacion> <cliente> <ruc> "
                + "<edicion> <maxSucursales> <maxCajas> <mantenimientoHasta yyyy-MM-dd>");
        System.out.println("Ediciones: SUPERMERCADO, MINIMERCADO, TIENDA");
    }
}
