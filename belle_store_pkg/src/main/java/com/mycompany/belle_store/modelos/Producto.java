package com.mycompany.belle_store.modelos;

import com.mycompany.belle_store.util.Formato;
import com.mycompany.belle_store.validacion.Validaciones;

/**
 * Modelo de datos representativo de un Producto en Belle Store.
 * Almacena información del catálogo visual y del inventario de administración.
 *
 * <p>El estado ({@code DISPONIBLE} / {@code AGOTADO}) no se persiste aparte:
 * siempre se deriva del {@code stock} para evitar inconsistencias.</p>
 */
public class Producto {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String id;
    private String imagen; // Ruta local (ej. "img/...") o importada ("imagenes/...") o URL externa
    private String nombre;
    private String categoria;
    private int stock;
    private double precio;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    /**
     * Constructor por defecto para Gson.
     */
    public Producto() {
    }

    /**
     * Constructor principal para instanciar nuevos productos.
     * El estado se deriva automáticamente del stock ({@code 0 => AGOTADO}, {@code >0 => DISPONIBLE}).
     */
    public Producto(String id, String imagen, String nombre, String categoria,
                    int stock, double precio) {
        this.id = id;
        this.imagen = imagen;
        this.nombre = nombre;
        this.categoria = categoria;
        this.stock = stock;
        this.precio = precio;
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Getters, Setters y Formato)
    // =========================================================================
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Estado derivado del inventario actual ({@code DISPONIBLE} si stock > 0, si no {@code AGOTADO}).
     */
    public String getEstado() {
        return Validaciones.estadoPorStock(this.stock);
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    /**
     * Formatea el precio al estándar visual visto en Figma: "$200.000 COP".
     */
    public String getPrecioFormateado() {
        return Formato.moneda(this.precio);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", categoria='" + categoria + '\'' +
                ", stock=" + stock +
                ", estado='" + getEstado() + '\'' +
                ", precio=" + getPrecioFormateado() +
                '}';
    }
}
