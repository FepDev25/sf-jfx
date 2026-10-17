package ec.sfjfx.core.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashing de contrasenas y PIN con PBKDF2 (solo JDK, sin dependencias externas).
 *
 * <p>Formato almacenado: {@code pbkdf2$<iteraciones>$<saltBase64>$<hashBase64>}.
 */
public final class SeguridadPass {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_CLAVE = 256;
    private static final int LONGITUD_SAL = 16;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SeguridadPass() {
    }

    /** Genera un hash nuevo para la contrasena dada. */
    public static String hashear(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia");
        }
        byte[] sal = new byte[LONGITUD_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(password.toCharArray(), sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Verifica una contrasena contra un hash almacenado. */
    public static boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(password.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, LONGITUD_CLAVE);
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            return fabrica.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo derivar la contrasena", e);
        }
    }

    /** Utilidad de diagnostico: {@code java -cp ... SeguridadPass "clave"}. */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: SeguridadPass <contrasena>");
            return;
        }
        String hash = hashear(args[0]);
        System.out.println(hash);
        System.out.println("verifica(" + args[0] + ") = " + verificar(args[0], hash));
        System.out.println("bytes utf8 = " + args[0].getBytes(StandardCharsets.UTF_8).length);
    }
}
