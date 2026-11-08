package com.mycompany.belle_store.modelos;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modelo representativo de una Orden de Compra en el sistema.
 * Mapea directamente las columnas de la vista Admin Órdenes en Figma.
 */
public class Orden {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String id;
    private String clienteId; // Número de identificación del cliente (ej. "28483")
    private List<ItemOrden> articulos;
    private int totalArticulos;
    private String direccion;
    private String estado; // "No Entregado", "Entregado", "Pendiente"
    private double valorTotal;
    private String fecha;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    public Orden() {
        this.articulos = new ArrayList<>();
    }

    public Orden(String id, String clienteId, List<ItemOrden> articulos,
                 String direccion, String estado, double valorTotal, String fecha) {
        this.id = id;
        this.clienteId = clienteId;
        this.articulos = (articulos != null) ? articulos : new ArrayList<>();
        this.direccion = direccion;
        this.estado = estado;
        this.valorTotal = valorTotal;
        this.fecha = fecha;
        this.totalArticulos = calcularTotalArticulos();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemOrden> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<ItemOrden> articulos) {
        this.articulos = articulos;
        this.totalArticulos = calcularTotalArticulos();
    }

    public int getTotalArticulos() {
        if (totalArticulos <= 0 && articulos != null) {
            totalArticulos = calcularTotalArticulos();
        }
        return totalArticulos;
    }

    public void setTotalArticulos(int totalArticulos) {
        this.totalArticulos = totalArticulos;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public int calcularTotalArticulos() {
        if (articulos == null) return 0;
        int total = 0;
        for (ItemOrden item : articulos) {
            total += item.getCantidad();
        }
        return total;
    }

    /**
     * Retorna el texto para la celda de artículos:
     * "• Camisa - 1" o lista multilínea con viñetas.
     */
    public String getArticulosFormateados() {
        if (articulos == null || articulos.isEmpty()) {
            return "Sin artículos";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < articulos.size(); i++) {
            sb.append("• ").append(articulos.get(i).getDescripcionFormateada());
            if (i < articulos.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Formatea el valor total al formato de Figma: "$200.000 COP".
     */
    public String getValorTotalFormateado() {
        NumberFormat formato = NumberFormat.getInstance(new Locale("es", "CO"));
        return "$" + formato.format((long) this.valorTotal) + " COP";
    }

    @Override
    public String toString() {
        return "Orden{" +
                "id='" + id + '\'' +
                ", clienteId='" + clienteId + '\'' +
                ", totalArticulos=" + getTotalArticulos() +
                ", estado='" + estado + '\'' +
                ", valorTotal=" + getValorTotalFormateado() +
                '}';
    }
}
