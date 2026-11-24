package com.mycompany.belle_store.persistencia;

import java.io.File;

/**
 * Clase utilitaria pedagógica para resolver la ubicación física de la carpeta 'datos/'
 * donde se almacenan los archivos JSON de persistencia (y las imágenes de productos).
 *
 * <p>Orden de resolución:</p>
 * <ol>
 *   <li>Propiedad del sistema {@code -Dbelle.datos=/ruta/absoluta} (la usan las pruebas).</li>
 *   <li>Carpeta {@code datos/} junto al {@code pom.xml} del proyecto (directorio actual o {@code belle_store_pkg/}).</li>
 *   <li>Carpeta {@code ~/.belle_store/datos} si no se encuentra el proyecto.</li>
 * </ol>
 */
public class ConfiguracionRutas {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final String PROPIEDAD_RUTA = "belle.datos";
    private static final String CARPETA_DATOS = "datos";
    private static final String CARPETA_IMAGENES = "imagenes";
    private static final String CARPETA_RECIBOS = "recibos";

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private ConfiguracionRutas() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Obtiene el directorio base 'datos/', asegurando su existencia en disco.
     */
    public static File obtenerDirectorioDatos() {
        File dir;
        String configurada = System.getProperty(PROPIEDAD_RUTA);

        if (configurada != null && !configurada.trim().isEmpty()) {
            dir = new File(configurada.trim());
        } else if (new File("pom.xml").isFile()) {
            dir = new File(CARPETA_DATOS);
        } else if (new File("belle_store_pkg", "pom.xml").isFile()) {
            dir = new File("belle_store_pkg", CARPETA_DATOS);
        } else {
            dir = new File(new File(System.getProperty("user.home"), ".belle_store"), CARPETA_DATOS);
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsoluteFile();
    }

    /**
     * Retorna el archivo File correspondiente a un nombre de archivo JSON.
     */
    public static File obtenerArchivo(String nombreArchivo) {
        return new File(obtenerDirectorioDatos(), nombreArchivo);
    }

    /**
     * Carpeta donde se copian las imágenes que el administrador asocia a los productos.
     */
    public static File obtenerDirectorioImagenes() {
        File dir = new File(obtenerDirectorioDatos(), CARPETA_IMAGENES);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Carpeta donde se guardan por defecto los comprobantes de compra en formato texto (.txt).
     */
    public static File obtenerDirectorioRecibos() {
        File dir = new File(obtenerDirectorioDatos(), CARPETA_RECIBOS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
