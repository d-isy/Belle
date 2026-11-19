package com.mycompany.belle_store.modelos;

import com.google.gson.annotations.SerializedName;
import com.mycompany.belle_store.util.Formato;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo representativo de una Orden de Compra en el sistema.
 * Mapea directamente las columnas de la vista Admin Órdenes en Figma.
 *
 * <p>{@code totalArticulos} y {@code valorTotal} se calculan siempre desde la lista
 * de {@code articulos} para garantizar consistencia (una única fuente de verdad).</p>
 */
public class Orden {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String id;

    // Número de identificación (cédula) del cliente (ej. "28483").
    // alternate = {"clienteId"} mantiene compatibilidad al leer archivos Ordenes.json antiguos.
    @SerializedName(value = "clienteIdentificacion", alternate = {"clienteId"})
    private String clienteIdentificacion;

    private List<ItemOrden> articulos;
    private String direccion;
    private String estado; // "No Entregado", "Entregado", "Pendiente"
    private String fecha;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    public Orden() {
        this.articulos = new ArrayList<>();
    }

    public Orden(String id, String clienteIdentificacion, List<ItemOrden> articulos,
                 String direccion, String estado, String fecha) {
        this.id = id;
        this.clienteIdentificacion = clienteIdentificacion;
        this.articulos = (articulos != null) ? new ArrayList<>(articulos) : new ArrayList<>();
        this.direccion = direccion;
        this.estado = estado;
        this.fecha = fecha;
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

    public String getClienteIdentificacion() {
        return clienteIdentificacion;
    }

    public void setClienteIdentificacion(String clienteIdentificacion) {
        this.clienteIdentificacion = clienteIdentificacion;
    }

    public List<ItemOrden> getArticulos() {
        if (articulos == null) {
            articulos = new ArrayList<>();
        }
        return articulos;
    }

    public void setArticulos(List<ItemOrden> articulos) {
        this.articulos = (articulos != null) ? new ArrayList<>(articulos) : new ArrayList<>();
    }

    /**
     * Cantidad total de unidades sumando todos los artículos de la orden.
     */
    public int getTotalArticulos() {
        int total = 0;
        for (ItemOrden item : getArticulos()) {
            total += item.getCantidad();
        }
        return total;
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

    /**
     * Valor total de la orden calculado como la suma de los subtotales de sus artículos.
     */
    public double getValorTotal() {
        double total = 0.0;
        for (ItemOrden item : getArticulos()) {
            total += item.getSubtotal();
        }
        return total;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    /**
     * Retorna el texto para la celda de artículos:
     * "• Camisa - 1" o lista multilínea con viñetas.
     */
    public String getArticulosFormateados() {
        List<ItemOrden> lista = getArticulos();
        if (lista.isEmpty()) {
            return "Sin artículos";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append("• ").append(lista.get(i).getDescripcionFormateada());
            if (i < lista.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Formatea el valor total al formato de Figma: "$200.000 COP".
     */
    public String getValorTotalFormateado() {
        return Formato.moneda(getValorTotal());
    }

    @Override
    public String toString() {
        return "Orden{" +
                "id='" + id + '\'' +
                ", clienteIdentificacion='" + clienteIdentificacion + '\'' +
                ", totalArticulos=" + getTotalArticulos() +
                ", estado='" + estado + '\'' +
                ", valorTotal=" + getValorTotalFormateado() +
                '}';
    }
}
