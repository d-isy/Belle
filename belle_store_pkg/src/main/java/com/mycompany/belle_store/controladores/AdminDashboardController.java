package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controlador de la vista de Administrador.
 * En la Fase 2 provee navegación segura y cierre de sesión.
 * En la Fase 4 se expandirá con la gestión completa de tablas, filtros y modales.
 */
public class AdminDashboardController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private Label lblBienvenidaAdmin;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        if (!SesionActual.haySesionIniciada() || !SesionActual.esAdmin()) {
            redirigirALogin();
            return;
        }

        if (lblBienvenidaAdmin != null && SesionActual.getUsuarioLogueado() != null) {
            lblBienvenidaAdmin.setText("Administrador: " + SesionActual.getUsuarioLogueado().getNombreCompleto());
        }
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    @FXML
    private void manejarCerrarSesion(ActionEvent event) {
        SesionActual.cerrarSesion();
        redirigirALogin();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private void redirigirALogin() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Error al redirigir al login: " + e.getMessage());
        }
    }
}
