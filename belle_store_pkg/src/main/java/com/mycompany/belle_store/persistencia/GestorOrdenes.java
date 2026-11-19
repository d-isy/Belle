package com.mycompany.belle_store.persistencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.Orden;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor de persistencia en formato JSON para la entidad Orden.
 * Permite listar, agregar, actualizar, eliminar y filtrar órdenes en Ordenes.json.
 */
public class GestorOrdenes {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Ordenes.json";
    private final Gson gson;
    private final File archivo;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorOrdenes() {
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
     * Lee la lista completa de órdenes desde Ordenes.json.
     */
    public synchronized List<Orden> listar() {
        if (!archivo.exists() || archivo.length() == 0) {
            return new ArrayList<>();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            Type tipoLista = new TypeToken<List<Orden>>() {}.getType();
            List<Orden> ordenes = gson.fromJson(reader, tipoLista);
            return (ordenes != null) ? ordenes : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda la lista completa de órdenes sobrescribiendo el archivo.
     */
    public synchronized boolean guardarTodos(List<Orden> ordenes) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            gson.toJson(ordenes, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Error al escribir en " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca una orden por su identificador único.
     */
    public Orden buscarPorId(String id) {
        if (id == null) return null;
        for (Orden o : listar()) {
            if (id.trim().equalsIgnoreCase(o.getId())) {
                return o;
            }
        }
        return null;
    }

    /**
     * Agrega una nueva orden a la persistencia.
     */
    public synchronized boolean agregar(Orden nuevaOrden) {
        if (nuevaOrden == null || nuevaOrden.getId() == null) return false;
        List<Orden> lista = listar();

        // Evitar duplicados por ID
        if (buscarPorId(nuevaOrden.getId()) != null) {
            return false;
        }

        lista.add(nuevaOrden);
        return guardarTodos(lista);
    }

    /**
     * Actualiza una orden existente por su ID.
     */
    public synchronized boolean actualizar(Orden ordenActualizada) {
        if (ordenActualizada == null || ordenActualizada.getId() == null) return false;
        List<Orden> lista = listar();
        boolean encontrado = false;

        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equalsIgnoreCase(ordenActualizada.getId())) {
                lista.set(i, ordenActualizada);
                encontrado = true;
                break;
            }
        }

        return encontrado && guardarTodos(lista);
    }

    /**
     * Elimina una orden por su ID.
     */
    public synchronized boolean eliminar(String id) {
        if (id == null) return false;
        List<Orden> lista = listar();
        boolean removido = lista.removeIf(o -> o.getId().equalsIgnoreCase(id.trim()));
        return removido && guardarTodos(lista);
    }

    /**
     * Filtra la lista de órdenes por término de búsqueda (ID o Cliente)
     * y por estado (ej. "Entregado", "No Entregado").
     */
    public List<Orden> filtrar(String terminoBusqueda, String estadoFiltro) {
        List<Orden> todas = listar();
        return todas.stream().filter(o -> {
            // Filtro de la barra de búsqueda ("Buscar por ID, Cliente...")
            if (terminoBusqueda != null && !terminoBusqueda.trim().isEmpty()) {
                String term = terminoBusqueda.trim().toLowerCase();
                boolean coincideId = o.getId() != null && o.getId().toLowerCase().contains(term);
                boolean coincideCliente = o.getClienteId() != null && o.getClienteId().toLowerCase().contains(term);
                if (!coincideId && !coincideCliente) {
                    return false;
                }
            }

            // Filtro por Estado en el acordeón lateral
            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !estadoFiltro.equalsIgnoreCase("TODOS")) {
                if (o.getEstado() == null || !o.getEstado().equalsIgnoreCase(estadoFiltro.trim())) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }
}
