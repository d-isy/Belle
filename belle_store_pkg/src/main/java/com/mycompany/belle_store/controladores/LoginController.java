package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controlador de la vista de Inicio de Sesión (Login) de Belle Store.
 * Estructurado en 5 secciones pedagógicas con fines académicos.
 */
public class LoginController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private StackPane contenedorImagen;

    @FXML
    private ImageView imagenEditorial;

    @FXML
    private TextField campoUsuario;

    @FXML
    private PasswordField campoClave;

    @FXML
    private Label etiquetaMensaje;

    @FXML
    private HBox contenedorCargando;

    @FXML
    private Button botonIngresar;

    @FXML
    private Hyperlink enlaceRegistrarse;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        limpiarMensaje();
        ocultarCargando();
        configurarEscalaImagen();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Acción ejecutada al presionar el botón 'Ingresar' o dar Enter en la clave.
     */
    @FXML
    private void manejarIngreso(ActionEvent event) {
        String usuarioTexto = campoUsuario.getText() != null ? campoUsuario.getText().trim() : "";
        String claveTexto = campoClave.getText() != null ? campoClave.getText().trim() : "";

        // 1. Validación de campos obligatorios
        if (usuarioTexto.isEmpty() || claveTexto.isEmpty()) {
            mostrarMensaje("Por favor, ingresa tu usuario y contraseña.", true);
            return;
        }

        // 2. Mostrar indicador de cargando y deshabilitar controles para evitar doble clic
        mostrarCargando();
        limpiarMensaje();

        // 3. Simulación de espera asíncrona no bloqueante (PauseTransition)
        PauseTransition pausa = new PauseTransition(Duration.seconds(1.0));
        pausa.setOnFinished(e -> {
            ocultarCargando();

            // 4. Validación de credenciales contra la persistencia JSON
            Usuario usuarioAutenticado = gestorUsuarios.autenticar(usuarioTexto, claveTexto);

            if (usuarioAutenticado != null) {
                // Registrar en memoria la sesión activa
                SesionActual.iniciarSesion(usuarioAutenticado);
                mostrarMensaje("¡Inicio exitoso! Redirigiendo...", false);

                // 5. Redirección según rol
                redirigirSegunRol(usuarioAutenticado);
            } else {
                mostrarMensaje("Usuario o contraseña incorrectos. Verifica tus datos.", true);
            }
        });
        pausa.play();
    }

    /**
     * Navega a la vista de Registro de Clientes (Fase 3).
     */
    @FXML
    private void irARegistro(ActionEvent event) {
        try {
            App.setRoot("registro");
        } catch (IOException e) {
            mostrarMensaje("La vista de registro estará disponible en la Fase 3.", true);
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Validaciones, carga de vistas)
    // =========================================================================
    /**
     * Configura el redimensionamiento responsivo de la imagen promocional (image 1)
     * para que encaje por completo (contain) sin recortar a la modelo ni sus accesorios.
     */
    private void configurarEscalaImagen() {
        if (contenedorImagen != null && imagenEditorial != null) {
            imagenEditorial.setPreserveRatio(true);
            contenedorImagen.widthProperty().addListener((obs, oldV, newV) -> ajustarDimensionesImagen());
            contenedorImagen.heightProperty().addListener((obs, oldV, newV) -> ajustarDimensionesImagen());
        }
    }

    private void ajustarDimensionesImagen() {
        if (contenedorImagen == null || imagenEditorial == null) return;

        double anchoContenedor = contenedorImagen.getWidth();
        double altoContenedor = contenedorImagen.getHeight();

        if (anchoContenedor <= 0 || altoContenedor <= 0) return;

        // Dimensiones nativas de 'image 1' en Figma (720x1024)
        double anchoNativo = 720.0;
        double altoNativo = 1024.0;

        // Escala 'contain' para que la imagen completa quepa sin ningún recorte
        double escala = Math.min(anchoContenedor / anchoNativo, altoContenedor / altoNativo);
        imagenEditorial.setFitWidth(anchoNativo * escala);
        imagenEditorial.setFitHeight(altoNativo * escala);

        // Sin recorte para asegurar que toda la modelo sea visible
        contenedorImagen.setClip(null);
    }

    /**
     * Redirige al Administrador a su Dashboard o al Cliente a su vista Placeholder.
     */
    private void redirigirSegunRol(Usuario usuario) {
        try {
            if (usuario.esAdministrador()) {
                App.setRoot("admin_dashboard");
            } else {
                App.setRoot("cliente_placeholder");
            }
        } catch (IOException e) {
            System.err.println("Error al cargar la siguiente vista: " + e.getMessage());
            mostrarMensaje("Error al cargar la vista de destino: " + e.getMessage(), true);
        }
    }

    private void mostrarCargando() {
        contenedorCargando.setVisible(true);
        contenedorCargando.setManaged(true);
        botonIngresar.setDisable(true);
        campoUsuario.setDisable(true);
        campoClave.setDisable(true);
    }

    private void ocultarCargando() {
        contenedorCargando.setVisible(false);
        contenedorCargando.setManaged(false);
        botonIngresar.setDisable(false);
        campoUsuario.setDisable(false);
        campoClave.setDisable(false);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        etiquetaMensaje.setText(texto);
        etiquetaMensaje.setVisible(true);
        etiquetaMensaje.setManaged(true);

        if (esError) {
            etiquetaMensaje.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            etiquetaMensaje.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
    }

    private void limpiarMensaje() {
        etiquetaMensaje.setText("");
        etiquetaMensaje.setVisible(false);
        etiquetaMensaje.setManaged(false);
    }
}
