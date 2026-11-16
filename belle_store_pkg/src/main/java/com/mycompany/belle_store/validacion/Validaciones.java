package com.mycompany.belle_store.validacion;

import java.util.regex.Pattern;

/**
 * Reglas de validación compartidas por los controladores (y probadas directamente en las pruebas).
 */
public final class Validaciones {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final String REGEX_NOMBRE_PARCIAL = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$";
    public static final String REGEX_DIGITOS_PARCIAL = "^\\d*$";
    public static final int LONGITUD_MIN_CLAVE = 4;
    public static final int MAX_STOCK = 100000;
    public static final double MAX_PRECIO = 100000000.0;

    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile(REGEX_NOMBRE_PARCIAL);
    private static final Pattern PATRON_DIGITOS = Pattern.compile(REGEX_DIGITOS_PARCIAL);

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private Validaciones() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public static boolean esCorreoValido(String correo) {
        return correo != null && PATRON_CORREO.matcher(correo.trim()).matches();
    }

    /** Texto parcial permitido mientras se escribe un nombre: solo letras y espacios. */
    public static boolean esTextoDeNombre(String texto) {
        return texto != null && PATRON_NOMBRE.matcher(texto).matches();
    }

    /** Nombre completo: al menos 3 letras (sin contar espacios). */
    public static boolean esNombreValido(String nombre) {
        return nombre != null && esTextoDeNombre(nombre) && nombre.replaceAll("\\s", "").length() >= 3;
    }

    /** Texto parcial permitido mientras se escribe un número: solo dígitos. */
    public static boolean esSoloDigitos(String texto) {
        return texto != null && PATRON_DIGITOS.matcher(texto).matches();
    }

    public static boolean esCelularValido(String celular) {
        return celular != null && celular.matches("\\d{10}");
    }

    public static boolean esIdentificacionValida(String id) {
        return id != null && id.matches("\\d{5,15}");
    }

    public static boolean esContrasenaValida(String clave) {
        return clave != null && clave.length() >= LONGITUD_MIN_CLAVE;
    }

    /** Estado de un producto derivado de su stock: 0 => AGOTADO, >0 => DISPONIBLE. */
    public static String estadoPorStock(int stock) {
        return stock > 0 ? "DISPONIBLE" : "AGOTADO";
    }

    /**
     * Valida los datos de un producto.
     * @return mensaje de error, o {@code null} si todo es válido.
     */
    public static String validarProducto(String nombre, String stockTexto, String precioTexto) {
        if (nombre == null || nombre.trim().length() < 3) {
            return "El nombre del producto debe tener al menos 3 caracteres.";
        }
        int stock;
        try {
            stock = Integer.parseInt(stockTexto == null ? "" : stockTexto.trim());
        } catch (NumberFormatException e) {
            return "El stock debe ser un número entero.";
        }
        if (stock < 0 || stock > MAX_STOCK) {
            return "El stock debe estar entre 0 y " + MAX_STOCK + ".";
        }
        double precio;
        try {
            precio = Double.parseDouble(precioTexto == null ? "" : precioTexto.trim());
        } catch (NumberFormatException e) {
            return "El precio debe ser un número (sin puntos ni símbolos).";
        }
        if (precio <= 0 || precio > MAX_PRECIO || Double.isNaN(precio)) {
            return "El precio debe ser mayor que 0.";
        }
        return null;
    }
}
