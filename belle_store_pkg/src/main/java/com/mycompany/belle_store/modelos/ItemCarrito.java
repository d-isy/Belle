package com.mycompany.belle_store.modelos;

/**
 * Representa un producto seleccionado dentro del carrito de compras de un cliente.
 * Almacena el identificador del producto y la cantidad deseada; el precio y el stock
 * vigentes se consultan siempre desde {@code Productos.json}.
 */
public class ItemCarrito {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String productoId;
    private int cantidad;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    /**
     * Constructor por defecto para Gson.
     */
    public ItemCarrito() {
    }

    public ItemCarrito(String productoId, int cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return "ItemCarrito{productoId='" + productoId + "', cantidad=" + cantidad + '}';
    }
}
