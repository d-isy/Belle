package com.mycompany.belle_store.persistencia;

import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.ItemCarrito;
import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.ResumenCompra;
import com.mycompany.belle_store.modelos.Usuario;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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
     * Genera el siguiente ID numérico libre para una nueva orden (máximo existente + 1).
     */
    public String generarSiguienteId() {
        long maximo = 100;
        for (Orden o : listar()) {
            if (o.getId() == null) continue;
            try {
                maximo = Math.max(maximo, Long.parseLong(o.getId().trim()));
            } catch (NumberFormatException ignorada) {
                // IDs no numéricos no participan en la secuencia
            }
        }
        return String.valueOf(maximo + 1);
    }

    /**
     * Retorna las órdenes pertenecientes a un cliente específico, con las más recientes primero.
     */
    public List<Orden> listarPorCliente(String clienteIdentificacion) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()) {
            return List.of();
        }
        String cedula = clienteIdentificacion.trim();
        List<Orden> propias = listar().stream()
                .filter(o -> cedula.equalsIgnoreCase(o.getClienteIdentificacion()))
                .collect(Collectors.toList());
        Collections.reverse(propias);
        return propias;
    }

    /**
     * Procesa la compra del carrito actual de un cliente:
     * <ol>
     *   <li>Valida que haya productos en el carrito, dirección válida y stock suficiente para TODOS los ítems.</li>
     *   <li>Descuenta el inventario en {@code Productos.json} en una sola escritura atómica.</li>
     *   <li>Registra la nueva {@link Orden} en estado {@code "Pendiente"} con IVA (19%) y envío escalonado.</li>
     *   <li>Vacía el carrito del cliente.</li>
     * </ol>
     *
     * @throws IllegalStateException si el carrito está vacío, la dirección falta o no hay stock suficiente.
     */
    public synchronized Orden procesarCompra(Usuario cliente, String direccionEntrega,
                                             GestorCarrito gestorCarrito, GestorProductos gestorProductos) {
        if (cliente == null || cliente.getNumeroIdentificacion() == null
                || cliente.getNumeroIdentificacion().trim().isEmpty()) {
            throw new IllegalStateException("Debes iniciar sesión como cliente para completar la compra.");
        }
        if (direccionEntrega == null || direccionEntrega.trim().length() < 4) {
            throw new IllegalStateException("Por favor ingresa una dirección de entrega válida.");
        }

        String cedula = cliente.getNumeroIdentificacion().trim();
        List<ItemCarrito> itemsCarrito = gestorCarrito.obtenerItems(cedula);
        if (itemsCarrito.isEmpty()) {
            throw new IllegalStateException("Tu carrito de compras está vacío.");
        }

        List<Producto> catalogo = gestorProductos.listar();
        Map<String, Producto> porId = new LinkedHashMap<>();
        for (Producto p : catalogo) {
            if (p.getId() != null) {
                porId.put(p.getId().trim().toLowerCase(), p);
            }
        }

        // 1. Validación previa completa antes de modificar inventario (todo o nada)
        List<ItemOrden> articulosOrden = new ArrayList<>();
        double subtotal = 0.0;
        for (ItemCarrito item : itemsCarrito) {
            if (item.getProductoId() == null || item.getCantidad() <= 0) {
                continue;
            }
            Producto producto = porId.get(item.getProductoId().trim().toLowerCase());
            if (producto == null) {
                throw new IllegalStateException("Uno de los productos del carrito ya no existe en el catálogo.");
            }
            if (producto.getStock() < item.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para " + producto.getNombre()
                        + " (disponibles: " + producto.getStock() + ").");
            }
            articulosOrden.add(new ItemOrden(
                    producto.getId(),
                    producto.getNombre(),
                    item.getCantidad(),
                    producto.getPrecio()
            ));
            subtotal += producto.getPrecio() * item.getCantidad();
        }

        if (articulosOrden.isEmpty()) {
            throw new IllegalStateException("Tu carrito de compras no contiene artículos válidos.");
        }

        // 2. Descontar stock en memoria y persistir atómicamente en Productos.json
        for (ItemOrden articulo : articulosOrden) {
            Producto producto = porId.get(articulo.getProductoId().trim().toLowerCase());
            producto.setStock(producto.getStock() - articulo.getCantidad());
        }
        if (!gestorProductos.guardarTodos(catalogo)) {
            throw new IllegalStateException("No fue posible actualizar el inventario de productos.");
        }

        // 3. Calcular resumen (IVA 19% + envío escalonado) y crear la Orden
        ResumenCompra resumen = ResumenCompra.calcular(subtotal);
        Orden nuevaOrden = new Orden(
                generarSiguienteId(),
                cedula,
                articulosOrden,
                direccionEntrega.trim(),
                "Pendiente",
                LocalDateTime.now().format(FORMATO_FECHA),
                resumen.getCostoEnvio(),
                resumen.getImpuestos()
        );

        if (!agregar(nuevaOrden)) {
            throw new IllegalStateException("No fue posible registrar la orden de compra.");
        }

        // 4. Vaciar carrito del cliente
        gestorCarrito.vaciarCarrito(cedula);
        return nuevaOrden;
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
