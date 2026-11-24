package com.mycompany.belle_store.modelos;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa los artículos del carrito de compras pertenecientes a un cliente específico.
 * Se persiste en {@code Carritos.json} para conservar el carrito entre sesiones.
 */
public class CarritoCliente {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String clienteIdentificacion;
    private List<ItemCarrito> items;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    public CarritoCliente() {
        this.items = new ArrayList<>();
    }

    public CarritoCliente(String clienteIdentificacion, List<ItemCarrito> items) {
        this.clienteIdentificacion = clienteIdentificacion;
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public String getClienteIdentificacion() {
        return clienteIdentificacion;
    }

    public void setClienteIdentificacion(String clienteIdentificacion) {
        this.clienteIdentificacion = clienteIdentificacion;
    }

    public List<ItemCarrito> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<ItemCarrito> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }
}
