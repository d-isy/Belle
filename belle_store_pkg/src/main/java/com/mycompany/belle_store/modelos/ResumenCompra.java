package com.mycompany.belle_store.modelos;

import com.mycompany.belle_store.util.Formato;

/**
 * Objeto de valor inmutable que encapsula el cálculo oficial de totales de una compra:
 * <ul>
 *   <li><strong>Impuestos (IVA 19%):</strong> {@code Math.round(subtotal * 0.19)} (0 si el subtotal es 0).</li>
 *   <li><strong>Envío escalonado:</strong> {@code $10.000 COP} si el subtotal es menor a {@code $300.000 COP},
 *       o {@code $0 COP} (envío gratis) si el subtotal es mayor o igual a {@code $300.000 COP} (o si está vacío).</li>
 * </ul>
 */
public final class ResumenCompra {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final double TASA_IVA = 0.19;
    public static final double COSTO_ENVIO_ESTANDAR = 10000.0;
    public static final double UMBRAL_ENVIO_GRATIS = 300000.0;

    private final double subtotal;
    private final double costoEnvio;
    private final double impuestos;
    private final double total;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado + fábrica estática)
    // =========================================================================
    private ResumenCompra(double subtotal, double costoEnvio, double impuestos) {
        this.subtotal = subtotal;
        this.costoEnvio = costoEnvio;
        this.impuestos = impuestos;
        this.total = subtotal + costoEnvio + impuestos;
    }

    /**
     * Calcula el resumen de compra aplicando las reglas oficiales de IVA (19%) y envío escalonado.
     */
    public static ResumenCompra calcular(double subtotal) {
        if (subtotal <= 0.0) {
            return new ResumenCompra(0.0, 0.0, 0.0);
        }
        double impuestos = Math.round(subtotal * TASA_IVA);
        double envio = (subtotal >= UMBRAL_ENVIO_GRATIS) ? 0.0 : COSTO_ENVIO_ESTANDAR;
        return new ResumenCompra(subtotal, envio, impuestos);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public double getSubtotal() {
        return subtotal;
    }

    public double getCostoEnvio() {
        return costoEnvio;
    }

    public double getImpuestos() {
        return impuestos;
    }

    public double getTotal() {
        return total;
    }

    /** Indica si la compra alcanzó el monto mínimo para envío gratuito. */
    public boolean esEnvioGratis() {
        return subtotal >= UMBRAL_ENVIO_GRATIS;
    }

    public String getSubtotalFormateado() {
        return Formato.moneda(subtotal);
    }

    public String getCostoEnvioFormateado() {
        if (esEnvioGratis()) {
            return "Gratis ($0 COP)";
        }
        return Formato.moneda(costoEnvio);
    }

    public String getImpuestosFormateado() {
        return Formato.moneda(impuestos);
    }

    public String getTotalFormateado() {
        return Formato.moneda(total);
    }
}
