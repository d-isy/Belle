package com.mycompany.belle_store.modelos;

/**
 * Modelo representativo de un artículo individual dentro de una Orden de Compra.
 */
public class ItemOrden {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String productoId;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitario;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    public ItemOrden() {
    }

    public ItemOrden(String productoId, String nombreProducto, int cantidad, double precioUnitario) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
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

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return this.cantidad * this.precioUnitario;
    }

    /**
     * Devuelve el formato didáctico visible en la columna ARTÍCULOS de Figma: "Camisa - 1".
     */
    public String getDescripcionFormateada() {
        return nombreProducto + " - " + cantidad;
    }

    @Override
    public String toString() {
        return getDescripcionFormateada();
    }
}
