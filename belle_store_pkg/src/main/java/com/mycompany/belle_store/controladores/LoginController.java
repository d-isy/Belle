package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controlador de la vista de Inicio de Sesión (Login) de Belle Store.
 * Ejecuta la verificación PBKDF2 en segundo plano con {@link Task} para no congelar la interfaz.
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
    private Label etiquetaUsuario;

    @FXML
    private TextField campoUsuario;

    @FXML
    private Label etiquetaClave;

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
        configurarLimpiezaEnTiempoReal();
        Platform.runLater(() -> {
            if (campoUsuario != null) {
                campoUsuario.requestFocus();
            }
        });
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Al presionar Enter en el campo Usuario, mueve el foco al campo de contraseña.
     */
    @FXML
    private void enfocarClave(ActionEvent event) {
        if (campoClave != null) {
            campoClave.requestFocus();
        }
    }

    /**
     * Acción ejecutada al presionar el botón 'Ingresar' o dar Enter en la clave.
     */
    @FXML
    private void manejarIngreso(ActionEvent event) {
        String usuarioTexto = campoUsuario.getText() != null ? campoUsuario.getText().trim() : "";
        String claveTexto = campoClave.getText() != null ? campoClave.getText() : "";

        limpiarErroresVisuales();

        // 1. Validación de campos obligatorios
        boolean hayError = false;
        if (usuarioTexto.isEmpty()) {
            marcarError(campoUsuario, etiquetaUsuario);
            hayError = true;
        }
        if (claveTexto.isEmpty()) {
            marcarError(campoClave, etiquetaClave);
            hayError = true;
        }

        if (hayError) {
            mostrarMensaje("Por favor, ingresa tu usuario/correo y contraseña.", true);
            return;
        }

        // 2. Mostrar indicador de cargando y deshabilitar controles para evitar doble clic
        mostrarCargando();
        limpiarMensaje();

        // 3. Autenticación real (PBKDF2 + lectura JSON) en un hilo en segundo plano
        Task<Usuario> tareaAutenticar = new Task<>() {
            @Override
            protected Usuario call() {
                return gestorUsuarios.autenticar(usuarioTexto, claveTexto);
            }
        };

        tareaAutenticar.setOnSucceeded(e -> {
            ocultarCargando();
            Usuario usuarioAutenticado = tareaAutenticar.getValue();

            if (usuarioAutenticado != null) {
                SesionActual.iniciarSesion(usuarioAutenticado);
                mostrarMensaje("¡Inicio exitoso! Redirigiendo...", false);
                redirigirSegunRol(usuarioAutenticado);
            } else {
                marcarError(campoUsuario, etiquetaUsuario);
                marcarError(campoClave, etiquetaClave);
                mostrarMensaje("Usuario o contraseña incorrectos. Verifica tus datos.", true);
            }
        });

        tareaAutenticar.setOnFailed(e -> {
            ocultarCargando();
            mostrarMensaje("Ocurrió un error inesperado al verificar tus credenciales.", true);
        });

        Thread hilo = new Thread(tareaAutenticar, "belle-login-auth");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Navega a la vista de Registro de Clientes.
     */
    @FXML
    private void irARegistro(ActionEvent event) {
        try {
            App.setRoot("registro");
        } catch (IOException e) {
            mostrarMensaje("No fue posible abrir la vista de registro.", true);
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
     * Redirige al Administrador a su Dashboard o al Cliente al Catálogo principal.
     */
    private void redirigirSegunRol(Usuario usuario) {
        try {
            if (usuario.esAdministrador()) {
                App.setRoot("admin_dashboard");
            } else {
                App.setRoot("cliente_catalogo");
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
        if (enlaceRegistrarse != null) {
            enlaceRegistrarse.setDisable(true);
        }
    }

    private void ocultarCargando() {
        contenedorCargando.setVisible(false);
        contenedorCargando.setManaged(false);
        botonIngresar.setDisable(false);
        campoUsuario.setDisable(false);
        campoClave.setDisable(false);
        if (enlaceRegistrarse != null) {
            enlaceRegistrarse.setDisable(false);
        }
    }

    private void mostrarMensaje(String texto, boolean esError) {
        etiquetaMensaje.setText(texto);
        etiquetaMensaje.setVisible(true);
        etiquetaMensaje.setManaged(true);

        // El color y tamaño viven en estilos.css (.mensaje-error / .mensaje-exito)
        etiquetaMensaje.getStyleClass().setAll("label", esError ? "mensaje-error" : "mensaje-exito");
    }

    private void limpiarMensaje() {
        etiquetaMensaje.setText("");
        etiquetaMensaje.setVisible(false);
        etiquetaMensaje.setManaged(false);
    }

    private void configurarLimpiezaEnTiempoReal() {
        if (campoUsuario != null) {
            campoUsuario.textProperty().addListener((obs, oldVal, newVal) -> {
                limpiarError(campoUsuario, etiquetaUsuario);
                limpiarMensaje();
            });
        }
        if (campoClave != null) {
            campoClave.textProperty().addListener((obs, oldVal, newVal) -> {
                limpiarError(campoClave, etiquetaClave);
                limpiarMensaje();
            });
        }
    }

    private void marcarError(TextInputControl campo, Label etiqueta) {
        if (campo != null && !campo.getStyleClass().contains("campo-error")) {
            campo.getStyleClass().add("campo-error");
        }
        if (etiqueta != null && !etiqueta.getStyleClass().contains("etiqueta-campo-error")) {
            etiqueta.getStyleClass().add("etiqueta-campo-error");
        }
    }

    private void limpiarError(TextInputControl campo, Label etiqueta) {
        if (campo != null) {
            campo.getStyleClass().remove("campo-error");
        }
        if (etiqueta != null) {
            etiqueta.getStyleClass().remove("etiqueta-campo-error");
        }
    }

    private void limpiarErroresVisuales() {
        limpiarError(campoUsuario, etiquetaUsuario);
        limpiarError(campoClave, etiquetaClave);
    }
}
