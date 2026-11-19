package com.mycompany.belle_store.sesion;

import com.mycompany.belle_store.modelos.Usuario;

/**
 * Clase pedagógica que maneja la sesión activa en memoria de la aplicación.
 * Permite acceder de forma global al usuario que ha iniciado sesión y controlar
 * sus permisos y redirección según el rol.
 */
public class SesionActual {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de sesión
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static Usuario usuarioLogueado = null;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado para evitar instanciación)
    // =========================================================================
    private SesionActual() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de sesión
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Establece el usuario autenticado en la sesión.
     * @param usuario Instancia del usuario autenticado.
     */
    public static void iniciarSesion(Usuario usuario) {
        usuarioLogueado = usuario;
    }

    /**
     * Limpia la sesión actual al hacer logout.
     */
    public static void cerrarSesion() {
        usuarioLogueado = null;
    }

    /**
     * Obtiene el usuario que tiene sesión iniciada actualmente.
     * @return Usuario autenticado o null si no hay sesión.
     */
    public static Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    /**
     * Verifica si existe una sesión activa.
     */
    public static boolean haySesionIniciada() {
        return usuarioLogueado != null;
    }

    /**
     * Comprueba si el usuario logueado tiene rol de Administrador.
     */
    public static boolean esAdmin() {
        return usuarioLogueado != null && usuarioLogueado.esAdministrador();
    }

    /**
     * Comprueba si el usuario logueado tiene rol de Cliente.
     */
    public static boolean esCliente() {
        return usuarioLogueado != null && usuarioLogueado.esCliente();
    }
}
