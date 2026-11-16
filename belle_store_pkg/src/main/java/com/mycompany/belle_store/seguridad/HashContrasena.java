package com.mycompany.belle_store.seguridad;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilidad pedagógica de hash de contraseñas con PBKDF2 (incluido en el JDK, sin dependencias).
 *
 * <p><b>Formato almacenado en Usuarios.json (campo "contrasena"):</b></p>
 * <pre>pbkdf2$ITERACIONES$SALT_BASE64$HASH_BASE64</pre>
 * <ul>
 *   <li>Algoritmo: {@code PBKDF2WithHmacSHA256}</li>
 *   <li>Iteraciones: 65536 (leídas del propio texto almacenado al verificar)</li>
 *   <li>Salt: 16 bytes aleatorios, codificados en Base64 estándar</li>
 *   <li>Hash: 32 bytes (256 bits), codificados en Base64 estándar</li>
 * </ul>
 *
 * <p>Para generar un hash manualmente (por ejemplo para añadir un usuario a mano):</p>
 * <pre>java -cp target/classes com.mycompany.belle_store.seguridad.HashContrasena MiClave123</pre>
 */
public final class HashContrasena {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final String PREFIJO = "pbkdf2";
    public static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    public static final int ITERACIONES = 65536;
    public static final int BYTES_SALT = 16;
    public static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private HashContrasena() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /** Indica si un texto ya tiene el formato de hash almacenado. */
    public static boolean esHash(String texto) {
        return texto != null && texto.startsWith(PREFIJO + "$") && texto.split("\\$").length == 4;
    }

    /** Genera un hash nuevo con un salt aleatorio. */
    public static String generarHash(String contrasena) {
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        return generarHash(contrasena, salt, ITERACIONES);
    }

    /** Genera un hash con salt e iteraciones conocidos (útil para pruebas y uso manual). */
    public static String generarHash(String contrasena, byte[] salt, int iteraciones) {
        byte[] hash = derivar(contrasena, salt, iteraciones);
        return PREFIJO + "$" + iteraciones + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Verifica una contraseña en texto plano contra un hash almacenado (comparación en tiempo constante). */
    public static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || !esHash(almacenado)) {
            return false;
        }
        try {
            String[] partes = almacenado.split("\\$");
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(contrasena, salt, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String contrasena, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    /** Herramienta de línea de comandos: imprime el hash (y sus partes) de la contraseña indicada. */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java -cp target/classes com.mycompany.belle_store.seguridad.HashContrasena <contrasena>");
            return;
        }
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        String completo = generarHash(args[0], salt, ITERACIONES);
        String[] p = completo.split("\\$");
        System.out.println("Valor para el campo \"contrasena\" en Usuarios.json:");
        System.out.println(completo);
        System.out.println();
        System.out.println("Algoritmo   : " + ALGORITMO);
        System.out.println("Iteraciones : " + p[1]);
        System.out.println("Salt (B64)  : " + p[2]);
        System.out.println("Hash (B64)  : " + p[3]);
    }
}
