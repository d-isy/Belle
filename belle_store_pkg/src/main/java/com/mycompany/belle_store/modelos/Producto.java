package com.mycompany.belle_store.modelos;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Modelo de datos representativo de un Producto en Belle Store.
 * Almacena información del catálogo visual y del inventario de administración.
 */
public class Producto {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String id;
    private String imagen; // Ruta local (ej. "/img/...") o URL externa
    private String nombre;
    private String categoria;
    private int stock;
    private String estado; // "DISPONIBLE", "AGOTADO", etc.
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
     * Constructor completo para instanciar nuevos productos.
     */
    public Producto(String id, String imagen, String nombre, String categoria,
                    int stock, String estado, double precio) {
        this.id = id;
        this.imagen = imagen;
        this.nombre = nombre;
        this.categoria = categoria;
        this.stock = stock;
        this.estado = estado;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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
        NumberFormat formato = NumberFormat.getInstance(new Locale("es", "CO"));
        return "$" + formato.format((long) this.precio) + " COP";
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", categoria='" + categoria + '\'' +
                ", stock=" + stock +
                ", estado='" + estado + '\'' +
                ", precio=" + getPrecioFormateado() +
                '}';
    }
}
