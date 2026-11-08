package com.mycompany.belle_store.persistencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.Producto;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor de persistencia en formato JSON para la entidad Producto.
 * Permite listar, agregar, editar, eliminar y filtrar productos en Productos.json.
 */
public class GestorProductos {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Productos.json";
    private final Gson gson;
    private final File archivo;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorProductos() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.archivo = ConfiguracionRutas.obtenerArchivo(NOMBRE_ARCHIVO);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Lee la lista completa de productos desde Productos.json.
     */
    public synchronized List<Producto> listar() {
        if (!archivo.exists() || archivo.length() == 0) {
            return new ArrayList<>();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            Type tipoLista = new TypeToken<List<Producto>>() {}.getType();
            List<Producto> productos = gson.fromJson(reader, tipoLista);
            return (productos != null) ? productos : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda la lista completa de productos sobrescribiendo el archivo.
     */
    public synchronized boolean guardarTodos(List<Producto> productos) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            gson.toJson(productos, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Error al escribir en " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un producto por su identificador único.
     */
    public Producto buscarPorId(String id) {
        if (id == null) return null;
        for (Producto p : listar()) {
            if (id.trim().equalsIgnoreCase(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Agrega un nuevo producto a la persistencia.
     */
    public synchronized boolean agregar(Producto nuevoProducto) {
        if (nuevoProducto == null || nuevoProducto.getId() == null) return false;
        List<Producto> lista = listar();

        // Evitar duplicados por ID
        if (buscarPorId(nuevoProducto.getId()) != null) {
            return false;
        }

        lista.add(nuevoProducto);
        return guardarTodos(lista);
    }

    /**
     * Actualiza un producto existente buscándolo por su ID.
     */
    public synchronized boolean actualizar(Producto productoActualizado) {
        if (productoActualizado == null || productoActualizado.getId() == null) return false;
        List<Producto> lista = listar();
        boolean encontrado = false;

        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equalsIgnoreCase(productoActualizado.getId())) {
                lista.set(i, productoActualizado);
                encontrado = true;
                break;
            }
        }

        return encontrado && guardarTodos(lista);
    }

    /**
     * Elimina un producto por su ID.
     */
    public synchronized boolean eliminar(String id) {
        if (id == null) return false;
        List<Producto> lista = listar();
        boolean removido = lista.removeIf(p -> p.getId().equalsIgnoreCase(id.trim()));
        return removido && guardarTodos(lista);
    }

    /**
     * Filtra la lista de productos por término de búsqueda (ID, Nombre o Categoría)
     * y por filtros específicos de categoría y estado.
     */
    public List<Producto> filtrar(String terminoBusqueda, String categoriaFiltro, String estadoFiltro) {
        List<Producto> todos = listar();
        return todos.stream().filter(p -> {
            // Filtro por término en la barra de búsqueda
            if (terminoBusqueda != null && !terminoBusqueda.trim().isEmpty()) {
                String term = terminoBusqueda.trim().toLowerCase();
                boolean coincideId = p.getId() != null && p.getId().toLowerCase().contains(term);
                boolean coincideNombre = p.getNombre() != null && p.getNombre().toLowerCase().contains(term);
                boolean coincideCat = p.getCategoria() != null && p.getCategoria().toLowerCase().contains(term);
                if (!coincideId && !coincideNombre && !coincideCat) {
                    return false;
                }
            }

            // Filtro por Categoría específica del acordeón
            if (categoriaFiltro != null && !categoriaFiltro.trim().isEmpty() && !categoriaFiltro.equalsIgnoreCase("TODAS")) {
                if (p.getCategoria() == null || !p.getCategoria().equalsIgnoreCase(categoriaFiltro.trim())) {
                    return false;
                }
            }

            // Filtro por Estado específico del acordeón
            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !estadoFiltro.equalsIgnoreCase("TODOS")) {
                if (p.getEstado() == null || !p.getEstado().equalsIgnoreCase(estadoFiltro.trim())) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }
}
