package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Controlador de la vista de Registro de Clientes de Belle Store.
 * Estructurado en 5 secciones estándar con enfoque didáctico y académico.
 */
public class RegistroController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private TextField campoNombre;

    @FXML
    private TextField campoIdentificacion;

    @FXML
    private TextField campoCorreo;

    @FXML
    private TextField campoCelular;

    @FXML
    private TextField campoDireccion;

    @FXML
    private PasswordField campoContrasena;

    @FXML
    private PasswordField campoConfirmarContrasena;

    @FXML
    private Label etiquetaMensaje;

    @FXML
    private HBox contenedorCargando;

    @FXML
    private Button botonRegistrarse;

    @FXML
    private Hyperlink enlaceLogin;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();

    // Patrón didáctico de validación de formato de correo electrónico
    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        limpiarMensaje();
        ocultarCargando();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Procesa el formulario de registro de un nuevo cliente.
     */
    @FXML
    private void manejarRegistro(ActionEvent event) {
        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        String identificacion = campoIdentificacion.getText() != null ? campoIdentificacion.getText().trim() : "";
        String correo = campoCorreo.getText() != null ? campoCorreo.getText().trim() : "";
        String celular = campoCelular.getText() != null ? campoCelular.getText().trim() : "";
        String direccion = campoDireccion.getText() != null ? campoDireccion.getText().trim() : "";
        String contrasena = campoContrasena.getText() != null ? campoContrasena.getText() : "";
        String confirmarContrasena = campoConfirmarContrasena.getText() != null ? campoConfirmarContrasena.getText() : "";

        // 1. Validación de campos obligatorios
        if (nombre.isEmpty() || identificacion.isEmpty() || correo.isEmpty() ||
            celular.isEmpty() || direccion.isEmpty() || contrasena.isEmpty() || confirmarContrasena.isEmpty()) {
            mostrarMensaje("Por favor, completa todos los campos del formulario.", true);
            return;
        }

        // 2. Validación de formato de correo electrónico
        if (!esCorreoValido(correo)) {
            mostrarMensaje("El correo electrónico ingresado no tiene un formato válido.", true);
            return;
        }

        // 3. Validación de coincidencia de contraseñas
        if (!contrasena.equals(confirmarContrasena)) {
            mostrarMensaje("Las contraseñas no coinciden. Por favor verifícalas.", true);
            return;
        }

        // 4. Validación de longitud mínima de contraseña
        if (contrasena.length() < 4) {
            mostrarMensaje("La contraseña debe tener al menos 4 caracteres.", true);
            return;
        }

        // 5. Validación de no duplicidad de correo
        if (gestorUsuarios.existeCorreo(correo)) {
            mostrarMensaje("El correo electrónico ya se encuentra registrado.", true);
            return;
        }

        // 6. Validación de no duplicidad de número de identificación
        if (gestorUsuarios.existeIdentificacion(identificacion)) {
            mostrarMensaje("El número de identificación ya se encuentra registrado.", true);
            return;
        }

        // 7. Simulación asíncrona de guardado con PauseTransition
        mostrarCargando();
        limpiarMensaje();

        PauseTransition pausa = new PauseTransition(Duration.seconds(1.0));
        pausa.setOnFinished(e -> {
            // Instanciar el nuevo usuario cliente
            Usuario nuevoCliente = new Usuario(
                    null, // El gestor asigna USR-(size+1) automáticamente
                    nombre,
                    identificacion,
                    correo,
                    celular,
                    direccion,
                    contrasena,
                    Usuario.ROL_CLIENTE
            );

            boolean guardado = gestorUsuarios.guardar(nuevoCliente);

            if (guardado) {
                mostrarMensaje("¡Registro exitoso! Redirigiendo al inicio de sesión...", false);
                ocultarCargando();
                deshabilitarFormulario();

                // Transición fluida al Login tras breve pausa visual
                PauseTransition redireccion = new PauseTransition(Duration.seconds(1.5));
                redireccion.setOnFinished(ev -> irALogin());
                redireccion.play();
            } else {
                ocultarCargando();
                mostrarMensaje("No fue posible guardar el registro. Intenta nuevamente.", true);
            }
        });
        pausa.play();
    }

    /**
     * Regresa a la pantalla de Inicio de Sesión (Login).
     */
    @FXML
    private void irALogin() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Error al navegar a login: " + e.getMessage());
            mostrarMensaje("Error al navegar al inicio de sesión.", true);
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Validaciones y feedback)
    // =========================================================================
    private boolean esCorreoValido(String correo) {
        return correo != null && PATRON_CORREO.matcher(correo).matches();
    }

    private void mostrarCargando() {
        contenedorCargando.setVisible(true);
        contenedorCargando.setManaged(true);
        botonRegistrarse.setDisable(true);
    }

    private void ocultarCargando() {
        contenedorCargando.setVisible(false);
        contenedorCargando.setManaged(false);
        botonRegistrarse.setDisable(false);
    }

    private void deshabilitarFormulario() {
        campoNombre.setDisable(true);
        campoIdentificacion.setDisable(true);
        campoCorreo.setDisable(true);
        campoCelular.setDisable(true);
        campoDireccion.setDisable(true);
        campoContrasena.setDisable(true);
        campoConfirmarContrasena.setDisable(true);
        botonRegistrarse.setDisable(true);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        etiquetaMensaje.setText(texto);
        etiquetaMensaje.setVisible(true);
        etiquetaMensaje.setManaged(true);

        if (esError) {
            etiquetaMensaje.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            etiquetaMensaje.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 14px; -fx-font-weight: bold;");
        }
    }

    private void limpiarMensaje() {
        etiquetaMensaje.setText("");
        etiquetaMensaje.setVisible(false);
        etiquetaMensaje.setManaged(false);
    }
}
