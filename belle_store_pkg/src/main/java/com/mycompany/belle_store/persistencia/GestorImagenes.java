package com.mycompany.belle_store.persistencia;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Resuelve y administra las imágenes asociadas a los productos.
 *
 * <p>El campo {@code imagen} de un producto admite tres formas:</p>
 * <ul>
 *   <li>{@code img/archivo.png}: recurso incluido en el proyecto (classpath).</li>
 *   <li>{@code imagenes/archivo.png}: imagen copiada por el administrador a la carpeta {@code datos/imagenes/}.</li>
 *   <li>{@code https://...}: URL externa.</li>
 * </ul>
 */
public class GestorImagenes {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final String PLACEHOLDER = "img/placeholder_producto.png";
    private static final String BASE_RECURSOS = "/com/mycompany/belle_store/";

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private GestorImagenes() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Convierte la referencia guardada en el producto en una URL cargable por {@code javafx.scene.image.Image}.
     * @return URL en texto, o {@code null} si la imagen no existe.
     */
    public static String resolverUrl(String referencia) {
        if (referencia == null || referencia.trim().isEmpty()) {
            return null;
        }
        String ref = referencia.trim();
        String minuscula = ref.toLowerCase(Locale.ROOT);

        if (minuscula.startsWith("http://") || minuscula.startsWith("https://")) {
            return ref;
        }

        if (ref.startsWith("img/")) {
            URL recurso = GestorImagenes.class.getResource(BASE_RECURSOS + ref);
            return recurso != null ? recurso.toExternalForm() : null;
        }

        File enDatos = new File(ConfiguracionRutas.obtenerDirectorioDatos(), ref);
        if (enDatos.isFile()) {
            return enDatos.toURI().toString();
        }
        File absoluta = new File(ref);
        return absoluta.isFile() ? absoluta.toURI().toString() : null;
    }

    /** Resuelve la imagen del producto o, si no existe, la imagen genérica. Nunca retorna null. */
    public static String resolverUrlOPlaceholder(String referencia) {
        String url = resolverUrl(referencia);
        return url != null ? url : resolverUrl(PLACEHOLDER);
    }

    /** Indica si el archivo tiene una extensión de imagen soportada por JavaFX. */
    public static boolean esImagenSoportada(File archivo) {
        if (archivo == null) return false;
        String n = archivo.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp");
    }

    /**
     * Copia la imagen elegida a {@code datos/imagenes/} (sin sobrescribir otras) y retorna la
     * referencia relativa que se guarda en el producto, por ejemplo {@code imagenes/camisa.png}.
     */
    public static String importarImagen(File origen) throws IOException {
        if (!esImagenSoportada(origen) || !origen.isFile()) {
            throw new IOException("El archivo seleccionado no es una imagen válida (png, jpg, gif, bmp).");
        }
        File carpeta = ConfiguracionRutas.obtenerDirectorioImagenes();
        String nombre = origen.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        File destino = new File(carpeta, nombre);

        int contador = 1;
        while (destino.exists()) {
            int punto = nombre.lastIndexOf('.');
            String base = punto > 0 ? nombre.substring(0, punto) : nombre;
            String ext = punto > 0 ? nombre.substring(punto) : "";
            destino = new File(carpeta, base + "_" + contador++ + ext);
        }
        Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        return "imagenes/" + destino.getName();
    }
}
