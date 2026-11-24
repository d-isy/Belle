package com.mycompany.belle_store.util;

import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.ConfiguracionRutas;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Utilidad pedagógica para generar y guardar comprobantes de compra en formato {@code .txt}
 * sin dependencias externas (solo {@code java.nio.file}).
 */
public final class GeneradorComprobante {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String SEPARADOR = "============================================================";
    private static final String LINEA = "------------------------------------------------------------";

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private GeneradorComprobante() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Genera el contenido textual completo del comprobante de compra en español.
     */
    public static String generarTexto(Orden orden, Usuario cliente) {
        if (orden == null) {
            throw new IllegalArgumentException("La orden no puede ser nula.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARADOR).append(System.lineSeparator());
        sb.append("                  BELLE STORE - COMPROBANTE                 ").append(System.lineSeparator());
        sb.append(SEPARADOR).append(System.lineSeparator());
        sb.append("N° Orden:      #").append(orden.getId()).append(System.lineSeparator());
        sb.append("Tipo:          ").append(orden.getTipo()).append(System.lineSeparator());
        sb.append("Fecha:         ").append(orden.getFecha() != null ? orden.getFecha() : "—").append(System.lineSeparator());
        sb.append("Estado:        ").append(orden.getEstado() != null ? orden.getEstado() : "—").append(System.lineSeparator());
        sb.append(LINEA).append(System.lineSeparator());

        if (cliente != null) {
            sb.append("Cliente:       ").append(cliente.getNombreCompleto()).append(System.lineSeparator());
            sb.append("Correo:        ").append(cliente.getCorreo()).append(System.lineSeparator());
        }
        sb.append("Identificación:").append(" ").append(orden.getClienteIdentificacion()).append(System.lineSeparator());
        sb.append("Dirección:     ").append(orden.getDireccion() != null ? orden.getDireccion() : "—").append(System.lineSeparator());
        sb.append(LINEA).append(System.lineSeparator());
        sb.append("ARTÍCULOS COMPRADOS:").append(System.lineSeparator());

        for (ItemOrden item : orden.getArticulos()) {
            sb.append(String.format("  - %s | Cant: %d | Unit: %s | Subtotal: %s",
                    item.getNombreProducto(),
                    item.getCantidad(),
                    Formato.moneda(item.getPrecioUnitario()),
                    Formato.moneda(item.getSubtotal())))
              .append(System.lineSeparator());
        }

        sb.append(LINEA).append(System.lineSeparator());
        sb.append("Total artículos: ").append(orden.getTotalArticulos()).append(System.lineSeparator());
        sb.append("Subtotal:        ").append(Formato.moneda(orden.getSubtotalArticulos())).append(System.lineSeparator());
        sb.append("Envío:           ").append(orden.getCostoEnvioEfectivo() <= 0
                ? "Gratis ($0 COP)" : Formato.moneda(orden.getCostoEnvioEfectivo())).append(System.lineSeparator());
        sb.append("Impuestos (IVA): ").append(Formato.moneda(orden.getImpuestosEfectivos())).append(System.lineSeparator());
        sb.append("TOTAL PAGADO:    ").append(orden.getTotalConImpuestosYEnvioFormateado()).append(System.lineSeparator());
        sb.append(SEPARADOR).append(System.lineSeparator());
        sb.append("Gracias por comprar en Belle Store.").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Guarda el comprobante en el archivo de destino especificado en codificación UTF-8.
     */
    public static File guardarEnArchivo(Orden orden, Usuario cliente, File destino) throws IOException {
        if (destino == null) {
            throw new IllegalArgumentException("El archivo de destino no puede ser nulo.");
        }
        File carpeta = destino.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }
        Files.writeString(destino.toPath(), generarTexto(orden, cliente), StandardCharsets.UTF_8);
        return destino;
    }

    /**
     * Guarda el comprobante automáticamente en {@code datos/recibos/comprobante_orden_<id>.txt}.
     */
    public static File guardarEnRecibos(Orden orden, Usuario cliente) throws IOException {
        String idLimpio = orden.getId() != null ? orden.getId().replaceAll("[^A-Za-z0-9_-]", "_") : "sin_id";
        File destino = new File(ConfiguracionRutas.obtenerDirectorioRecibos(),
                "comprobante_orden_" + idLimpio + ".txt");
        return guardarEnArchivo(orden, cliente, destino);
    }
}
