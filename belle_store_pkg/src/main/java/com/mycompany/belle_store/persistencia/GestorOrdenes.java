package com.mycompany.belle_store.persistencia;

import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.Orden;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestor de persistencia en formato JSON para la entidad Orden.
 * Hereda lectura/escritura atómica de {@link GestorJson} y provee CRUD y filtrado.
 */
public class GestorOrdenes extends GestorJson<Orden> {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Ordenes.json";
    private static final Type TIPO_LISTA = new TypeToken<List<Orden>>() {}.getType();

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorOrdenes() {
        super(NOMBRE_ARCHIVO, TIPO_LISTA);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
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
     * Busca los IDs de las órdenes que contienen el producto indicado (integridad referencial).
     */
    public List<String> buscarIdsOrdenesConProducto(String productoId) {
        if (productoId == null) return List.of();
        return listar().stream()
                .filter(o -> o.getArticulos().stream()
                        .anyMatch(i -> productoId.equalsIgnoreCase(i.getProductoId())))
                .map(Orden::getId)
                .collect(Collectors.toList());
    }

    /**
     * Filtra órdenes por término de búsqueda (ID de orden o identificación del cliente) y por estado.
     */
    public List<Orden> filtrar(String terminoBusqueda, String estadoFiltro) {
        return filtrar(terminoBusqueda, estadoFiltro, null);
    }

    /**
     * Filtra órdenes por término de búsqueda (ID, identificación del cliente o nombre del cliente
     * si se proporciona el mapa {@code identificacion -> nombre}) y por estado logístico.
     */
    public List<Orden> filtrar(String terminoBusqueda, String estadoFiltro, Map<String, String> nombresClientes) {
        List<Orden> todas = listar();
        return todas.stream().filter(o -> {
            // Filtro de la barra de búsqueda ("Buscar por ID, Cliente...")
            if (terminoBusqueda != null && !terminoBusqueda.trim().isEmpty()) {
                String term = terminoBusqueda.trim().toLowerCase();
                String cedula = o.getClienteIdentificacion();
                boolean coincideId = o.getId() != null && o.getId().toLowerCase().contains(term);
                boolean coincideCedula = cedula != null && cedula.toLowerCase().contains(term);
                boolean coincideNombre = false;
                if (nombresClientes != null && cedula != null) {
                    String nombre = nombresClientes.get(cedula);
                    coincideNombre = nombre != null && nombre.toLowerCase().contains(term);
                }
                if (!coincideId && !coincideCedula && !coincideNombre) {
                    return false;
                }
            }

            // Filtro por Estado en el panel lateral
            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !estadoFiltro.equalsIgnoreCase("TODOS")) {
                if (o.getEstado() == null || !o.getEstado().equalsIgnoreCase(estadoFiltro.trim())) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }
}
