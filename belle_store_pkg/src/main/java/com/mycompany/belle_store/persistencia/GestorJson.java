package com.mycompany.belle_store.persistencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base genérica para la persistencia de listas de entidades en archivos JSON.
 * Centraliza la lectura/escritura con Google Gson y garantiza escritura atómica
 * mediante un archivo temporal para evitar corrupción o pérdida de datos si falla a mitad.
 *
 * @param <T> Tipo de modelo persistido (por ejemplo, {@code Usuario}, {@code Producto} u {@code Orden}).
 */
public abstract class GestorJson<T> {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    protected final String nombreArchivo;
    protected final Gson gson;
    protected final File archivo;
    private final Type tipoLista;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    protected GestorJson(String nombreArchivo, Type tipoLista) {
        this.nombreArchivo = nombreArchivo;
        this.tipoLista = tipoLista;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.archivo = ConfiguracionRutas.obtenerArchivo(nombreArchivo);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Lee y retorna la lista completa de elementos desde el archivo JSON.
     */
    public synchronized List<T> listar() {
        if (!archivo.exists() || archivo.length() == 0) {
            return new ArrayList<>();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            List<T> elementos = gson.fromJson(reader, tipoLista);
            return (elementos != null) ? elementos : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer " + nombreArchivo + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda la lista completa de forma atómica: escribe primero en un archivo {@code .tmp}
     * en la misma carpeta y luego reemplaza el archivo final. Si ocurre un error durante
     * la escritura, el JSON original permanece intacto.
     */
    public synchronized boolean guardarTodos(List<T> elementos) {
        File carpeta = archivo.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }

        File temporal = new File(carpeta, nombreArchivo + ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporal), StandardCharsets.UTF_8)) {
                gson.toJson(elementos, writer);
            }

            try {
                Files.move(temporal.toPath(), archivo.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error al escribir en " + nombreArchivo + ": " + e.getMessage());
            if (temporal.exists()) {
                temporal.delete();
            }
            return false;
        }
    }
}
