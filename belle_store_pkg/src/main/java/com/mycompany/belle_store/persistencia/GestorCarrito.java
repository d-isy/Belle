package com.mycompany.belle_store.persistencia;

import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.CarritoCliente;
import com.mycompany.belle_store.modelos.ItemCarrito;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.ResumenCompra;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en JSON para el carrito de compras de cada cliente ({@code Carritos.json}).
 * Hereda la escritura atómica de {@link GestorJson} y controla límites de stock y totales.
 */
public class GestorCarrito extends GestorJson<CarritoCliente> {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Carritos.json";
    private static final Type TIPO_LISTA = new TypeToken<List<CarritoCliente>>() {}.getType();

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorCarrito() {
        super(NOMBRE_ARCHIVO, TIPO_LISTA);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Retorna una copia de los ítems en el carrito del cliente indicado.
     */
    public List<ItemCarrito> obtenerItems(String clienteIdentificacion) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cedula = clienteIdentificacion.trim();
        for (CarritoCliente carrito : listar()) {
            if (cedula.equalsIgnoreCase(carrito.getClienteIdentificacion())) {
                return new ArrayList<>(carrito.getItems());
            }
        }
        return new ArrayList<>();
    }

    /**
     * Cuenta la cantidad total de unidades (sumando cantidades) en el carrito del cliente.
     */
    public int contarTotalUnidades(String clienteIdentificacion) {
        int total = 0;
        for (ItemCarrito item : obtenerItems(clienteIdentificacion)) {
            total += item.getCantidad();
        }
        return total;
    }

    /**
     * Agrega una cantidad de un producto al carrito del cliente, validando que no supere el stock.
     *
     * @return {@code true} si se agregó/incrementó correctamente; {@code false} si excede el stock o los datos no son válidos.
     */
    public synchronized boolean agregarProducto(String clienteIdentificacion, String productoId,
                                                int cantidad, int stockDisponible) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()
                || productoId == null || productoId.trim().isEmpty()
                || cantidad <= 0 || stockDisponible <= 0) {
            return false;
        }

        String cedula = clienteIdentificacion.trim();
        String idProd = productoId.trim();
        List<CarritoCliente> todos = listar();
        CarritoCliente carrito = buscarOCrear(todos, cedula);

        for (ItemCarrito item : carrito.getItems()) {
            if (idProd.equalsIgnoreCase(item.getProductoId())) {
                int nuevaCantidad = item.getCantidad() + cantidad;
                if (nuevaCantidad > stockDisponible) {
                    return false;
                }
                item.setCantidad(nuevaCantidad);
                return guardarTodos(todos);
            }
        }

        if (cantidad > stockDisponible) {
            return false;
        }
        carrito.getItems().add(new ItemCarrito(idProd, cantidad));
        return guardarTodos(todos);
    }

    /**
     * Actualiza la cantidad de un producto en el carrito. Si {@code nuevaCantidad <= 0}, lo elimina.
     *
     * @return {@code true} si la operación fue válida y se persistió; {@code false} si excede el stock.
     */
    public synchronized boolean actualizarCantidad(String clienteIdentificacion, String productoId,
                                                   int nuevaCantidad, int stockDisponible) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()
                || productoId == null || productoId.trim().isEmpty()) {
            return false;
        }

        if (nuevaCantidad <= 0) {
            return eliminarProducto(clienteIdentificacion, productoId);
        }
        if (nuevaCantidad > stockDisponible) {
            return false;
        }

        String cedula = clienteIdentificacion.trim();
        String idProd = productoId.trim();
        List<CarritoCliente> todos = listar();

        for (CarritoCliente carrito : todos) {
            if (cedula.equalsIgnoreCase(carrito.getClienteIdentificacion())) {
                for (ItemCarrito item : carrito.getItems()) {
                    if (idProd.equalsIgnoreCase(item.getProductoId())) {
                        item.setCantidad(nuevaCantidad);
                        return guardarTodos(todos);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Elimina un producto del carrito del cliente.
     */
    public synchronized boolean eliminarProducto(String clienteIdentificacion, String productoId) {
        if (clienteIdentificacion == null || productoId == null) {
            return false;
        }
        String cedula = clienteIdentificacion.trim();
        String idProd = productoId.trim();
        List<CarritoCliente> todos = listar();

        for (CarritoCliente carrito : todos) {
            if (cedula.equalsIgnoreCase(carrito.getClienteIdentificacion())) {
                boolean removido = carrito.getItems().removeIf(i -> idProd.equalsIgnoreCase(i.getProductoId()));
                return removido && guardarTodos(todos);
            }
        }
        return false;
    }

    /**
     * Vacía por completo el carrito del cliente indicado.
     */
    public synchronized boolean vaciarCarrito(String clienteIdentificacion) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()) {
            return false;
        }
        String cedula = clienteIdentificacion.trim();
        List<CarritoCliente> todos = listar();
        for (CarritoCliente carrito : todos) {
            if (cedula.equalsIgnoreCase(carrito.getClienteIdentificacion())) {
                carrito.getItems().clear();
                return guardarTodos(todos);
            }
        }
        return true;
    }

    /**
     * Calcula el resumen financiero (Subtotal, Envío escalonado, IVA 19% y Total)
     * usando los precios vigentes del catálogo de productos.
     */
    public ResumenCompra calcularResumen(String clienteIdentificacion, GestorProductos gestorProductos) {
        double subtotal = 0.0;
        for (ItemCarrito item : obtenerItems(clienteIdentificacion)) {
            Producto producto = gestorProductos.buscarPorId(item.getProductoId());
            if (producto != null && item.getCantidad() > 0) {
                subtotal += producto.getPrecio() * item.getCantidad();
            }
        }
        return ResumenCompra.calcular(subtotal);
    }

    private CarritoCliente buscarOCrear(List<CarritoCliente> todos, String cedula) {
        for (CarritoCliente carrito : todos) {
            if (cedula.equalsIgnoreCase(carrito.getClienteIdentificacion())) {
                return carrito;
            }
        }
        CarritoCliente nuevo = new CarritoCliente(cedula, new ArrayList<>());
        todos.add(nuevo);
        return nuevo;
    }
}
