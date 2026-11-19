package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controlador de la vista provisional (Placeholder) para usuarios con rol CLIENTE.
 * Muestra la información del usuario autenticado en SesionActual y permite cerrar sesión.
 */
public class ClientePlaceholderController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private Label lblNombre;

    @FXML
    private Label lblIdentificacion;

    @FXML
    private Label lblCorreo;

    @FXML
    private Label lblCelular;

    @FXML
    private Label lblDireccion;

    @FXML
    private Label lblRol;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private Usuario usuarioActual;

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        // Verificar que exista una sesión activa
        if (!SesionActual.haySesionIniciada()) {
            redirigirALogin();
            return;
        }

        usuarioActual = SesionActual.getUsuarioLogueado();
        cargarDatosCliente();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Cierra la sesión activa en memoria y retorna a la pantalla de Login.
     */
    @FXML
    private void manejarCerrarSesion(ActionEvent event) {
        SesionActual.cerrarSesion();
        redirigirALogin();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Vuelca la información de la entidad Usuario en las etiquetas correspondientes.
     */
    private void cargarDatosCliente() {
        if (usuarioActual != null) {
            lblNombre.setText(usuarioActual.getNombreCompleto() != null ? usuarioActual.getNombreCompleto() : "N/A");
            lblIdentificacion.setText(usuarioActual.getNumeroIdentificacion() != null ? usuarioActual.getNumeroIdentificacion() : "N/A");
            lblCorreo.setText(usuarioActual.getCorreo() != null ? usuarioActual.getCorreo() : "N/A");
            lblCelular.setText(usuarioActual.getCelular() != null ? usuarioActual.getCelular() : "N/A");
            lblDireccion.setText(usuarioActual.getDireccion() != null ? usuarioActual.getDireccion() : "N/A");
            lblRol.setText(usuarioActual.getRol() != null ? usuarioActual.getRol() : "CLIENTE");
        }
    }

    private void redirigirALogin() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Error al retornar a login: " + e.getMessage());
        }
    }
}
