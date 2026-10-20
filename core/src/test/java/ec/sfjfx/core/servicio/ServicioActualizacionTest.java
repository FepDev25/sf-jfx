package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ec.sfjfx.core.config.AppConfig;

/** Verifica el manifiesto firmado y la descarga de un paquete de actualizacion. */
class ServicioActualizacionTest {

    @TempDir
    Path directorio;

    @AfterEach
    void limpiar() {
        System.clearProperty("sfjfx.config");
        AppConfig.recargar();
    }

    @Test
    void verificaFirmaDescargaYDetectaManifiestoAlterado() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();

        Path clavePublica = directorio.resolve("publica.pem");
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(par.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Files.writeString(clavePublica, pem, StandardCharsets.UTF_8);

        Path paquete = directorio.resolve("paquete.zip");
        Files.writeString(paquete, "contenido-de-prueba", StandardCharsets.UTF_8);
        String sha256 = sha256(paquete);

        ServicioActualizacion.Actualizacion actualizacion = new ServicioActualizacion.Actualizacion(
                "99.0.0",
                paquete.toUri().toString(),
                sha256,
                "Version de prueba",
                "");
        Signature firma = Signature.getInstance("SHA256withRSA");
        firma.initSign(par.getPrivate());
        firma.update(actualizacion.payload().getBytes(StandardCharsets.UTF_8));
        String firmaBase64 = Base64.getEncoder().encodeToString(firma.sign());

        Path manifiesto = directorio.resolve("manifiesto.properties");
        Properties props = new Properties();
        props.setProperty("version", actualizacion.version());
        props.setProperty("url", actualizacion.url());
        props.setProperty("sha256", actualizacion.sha256());
        props.setProperty("notas", actualizacion.notas());
        props.setProperty("firma", firmaBase64);
        try (var salida = Files.newBufferedWriter(manifiesto, StandardCharsets.UTF_8)) {
            props.store(salida, "manifiesto de prueba");
        }

        Path config = directorio.resolve("sfjfx.properties");
        Files.writeString(config,
                "app.entorno=dev\n"
                + "actualizacion.manifiesto=" + manifiesto + "\n"
                + "actualizacion.clave_publica=" + clavePublica + "\n"
                + "actualizacion.directorio=" + directorio.resolve("descargas") + "\n");
        System.setProperty("sfjfx.config", config.toString());
        AppConfig.recargar();

        ServicioActualizacion servicio = new ServicioActualizacion();
        ServicioActualizacion.Estado estado = servicio.consultar();
        assertTrue(estado.hayActualizacion(), estado.mensaje());
        assertEquals("99.0.0", estado.disponible().version());

        Path descargado = servicio.descargar(estado.disponible());
        assertTrue(Files.isRegularFile(descargado));
        assertEquals("contenido-de-prueba", Files.readString(descargado));

        String alterado = Files.readString(manifiesto).replace("Version de prueba", "Version alterada");
        Files.writeString(manifiesto, alterado);
        ServicioActualizacion.Estado estadoAlterado = servicio.consultar();
        assertFalse(estadoAlterado.hayActualizacion());
        assertTrue(estadoAlterado.mensaje().toLowerCase().contains("firma"));
    }

    private static String sha256(Path archivo) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(archivo)));
    }
}
