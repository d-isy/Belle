package com.mycompany.belle_store.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilidades de formato de presentación compartidas por modelos y controladores.
 * Centraliza el formato monetario de Figma (ej. {@code $200.000 COP}) para no repetirlo.
 */
public final class Formato {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    /** Configuración regional de Colombia (separador de miles: punto). */
    private static final Locale LOCALE_COLOMBIA = Locale.forLanguageTag("es-CO");

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private Formato() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Formatea un valor en pesos al estándar visual de Figma: {@code $200.000 COP}.
     * Se trunca a pesos enteros (no se muestran decimales).
     */
    public static String moneda(double valor) {
        NumberFormat formato = NumberFormat.getInstance(LOCALE_COLOMBIA);
        return "$" + formato.format((long) valor) + " COP";
    }

    /**
     * Compara dos textos que normalmente son números (por ejemplo IDs como "234" y "1000").
     * Si ambos son numéricos se ordenan por valor; si no, alfabéticamente sin distinguir mayúsculas.
     */
    public static int compararNumericoOTexto(String a, String b) {
        if (a == null || b == null) {
            return a == null ? (b == null ? 0 : -1) : 1;
        }
        try {
            return Long.compare(Long.parseLong(a.trim()), Long.parseLong(b.trim()));
        } catch (NumberFormatException e) {
            return a.compareToIgnoreCase(b);
        }
    }
}
