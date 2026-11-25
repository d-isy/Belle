package com.mycompany.belle_store.modelos;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa la lista de productos deseados (favoritos) de un cliente específico.
 * Se persiste en {@code Deseados.json} para conservar la selección entre sesiones.
 */
public class DeseadosCliente {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String clienteIdentificacion;
    private List<String> productosIds;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    public DeseadosCliente() {
        this.productosIds = new ArrayList<>();
    }

    public DeseadosCliente(String clienteIdentificacion, List<String> productosIds) {
        this.clienteIdentificacion = clienteIdentificacion;
        this.productosIds = (productosIds != null) ? new ArrayList<>(productosIds) : new ArrayList<>();
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

    public List<String> getProductosIds() {
        if (productosIds == null) {
            productosIds = new ArrayList<>();
        }
        return productosIds;
    }

    public void setProductosIds(List<String> productosIds) {
        this.productosIds = (productosIds != null) ? new ArrayList<>(productosIds) : new ArrayList<>();
    }
}
