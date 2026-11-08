package com.mycompany.belle_store.persistencia;

import java.io.File;

/**
 * Clase utilitaria pedagógica para resolver la ubicación física de la carpeta 'datos/'
 * donde se almacenan los archivos JSON de persistencia.
 */
public class ConfiguracionRutas {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String CARPETA_DATOS = "datos";

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
     * Soporta ejecución tanto desde la raíz del proyecto como desde la subcarpeta belle_store_pkg.
     */
    public static File obtenerDirectorioDatos() {
        File dir = new File(CARPETA_DATOS);
        
        // Si no existe pero existe belle_store_pkg, ajustar ruta
        if (!dir.exists() && new File("belle_store_pkg").isDirectory()) {
            dir = new File("belle_store_pkg", CARPETA_DATOS);
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Retorna el archivo File correspondiente a un nombre de archivo JSON.
     */
    public static File obtenerArchivo(String nombreArchivo) {
        return new File(obtenerDirectorioDatos(), nombreArchivo);
    }
}
