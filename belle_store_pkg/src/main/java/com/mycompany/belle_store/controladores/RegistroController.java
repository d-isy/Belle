package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import com.mycompany.belle_store.validacion.Validaciones;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.UnaryOperator;

/**
 * Controlador de la vista de Registro de Clientes de Belle Store.
 * Estructurado en 5 secciones estándar con enfoque didáctico y académico.
 */
public class RegistroController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private Label lblNombre;

    @FXML
    private TextField campoNombre;

    @FXML
    private Label lblIdentificacion;

    @FXML
    private TextField campoIdentificacion;

    @FXML
    private Label lblCorreo;

    @FXML
    private TextField campoCorreo;

    @FXML
    private Label lblCelular;

    @FXML
    private TextField campoCelular;

    @FXML
    private Label lblDireccion;

    @FXML
    private TextField campoDireccion;

    @FXML
    private Label lblContrasena;

    @FXML
    private PasswordField campoContrasena;

    @FXML
    private Label lblConfirmarContrasena;

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

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        limpiarMensaje();
        ocultarCargando();
        configurarFiltrosTexto();
        configurarValidacionEnTiempoReal();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Procesa el formulario de registro de un nuevo cliente con validaciones visuales.
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

        // 1. Detección y marcado visual de campos vacíos obligatorios
        boolean hayCamposVacios = false;
        if (nombre.isEmpty()) {
            marcarError(campoNombre, lblNombre);
            hayCamposVacios = true;
        }
        if (identificacion.isEmpty()) {
            marcarError(campoIdentificacion, lblIdentificacion);
            hayCamposVacios = true;
        }
        if (correo.isEmpty()) {
            marcarError(campoCorreo, lblCorreo);
            hayCamposVacios = true;
        }
        if (celular.isEmpty()) {
            marcarError(campoCelular, lblCelular);
            hayCamposVacios = true;
        }
        if (direccion.isEmpty()) {
            marcarError(campoDireccion, lblDireccion);
            hayCamposVacios = true;
        }
        if (contrasena.isEmpty()) {
            marcarError(campoContrasena, lblContrasena);
            hayCamposVacios = true;
        }
        if (confirmarContrasena.isEmpty()) {
            marcarError(campoConfirmarContrasena, lblConfirmarContrasena);
            hayCamposVacios = true;
        }

        if (hayCamposVacios) {
            mostrarMensaje("Por favor, completa los campos requeridos marcados en rojo.", true);
            return;
        }

        // 2. Validación de longitud mínima de nombre
        if (!Validaciones.esNombreValido(nombre)) {
            marcarError(campoNombre, lblNombre);
            mostrarMensaje("El nombre completo debe tener al menos 3 caracteres.", true);
            return;
        }

        // 3. Validación de longitud de número de identificación (mínimo 5 dígitos)
        if (!Validaciones.esIdentificacionValida(identificacion)) {
            marcarError(campoIdentificacion, lblIdentificacion);
            mostrarMensaje("El número de identificación debe tener al menos 5 dígitos.", true);
            return;
        }

        // 4. Validación de no duplicidad de número de identificación
        if (gestorUsuarios.existeIdentificacion(identificacion)) {
            marcarError(campoIdentificacion, lblIdentificacion);
            mostrarMensaje("El número de identificación ya se encuentra registrado.", true);
            return;
        }

        // 5. Validación de formato de correo electrónico
        if (!esCorreoValido(correo)) {
            marcarError(campoCorreo, lblCorreo);
            mostrarMensaje("El correo electrónico ingresado no tiene un formato válido.", true);
            return;
        }

        // 6. Validación de no duplicidad de correo
        if (gestorUsuarios.existeCorreo(correo)) {
            marcarError(campoCorreo, lblCorreo);
            mostrarMensaje("El correo electrónico ya se encuentra registrado.", true);
            return;
        }

        // 7. Validación de número de celular (mínimo 10 dígitos)
        if (!Validaciones.esCelularValido(celular)) {
            marcarError(campoCelular, lblCelular);
            mostrarMensaje("El número de celular debe contener 10 dígitos.", true);
            return;
        }

        // 8. Validación de longitud mínima de contraseña
        if (!Validaciones.esContrasenaValida(contrasena)) {
            marcarError(campoContrasena, lblContrasena);
            mostrarMensaje("La contraseña debe tener al menos 4 caracteres.", true);
            return;
        }

        // 9. Validación de coincidencia de contraseñas
        if (!contrasena.equals(confirmarContrasena)) {
            marcarError(campoContrasena, lblContrasena);
            marcarError(campoConfirmarContrasena, lblConfirmarContrasena);
            mostrarMensaje("Las contraseñas no coinciden. Por favor verifícalas.", true);
            return;
        }

        // 10. Guardado con hash PBKDF2 en segundo plano (Task) para no congelar la interfaz
        mostrarCargando();
        limpiarMensaje();

        Usuario nuevoCliente = new Usuario(
                null, // El gestor asigna USR-(max+1) automáticamente
                nombre,
                identificacion,
                correo,
                celular,
                direccion,
                contrasena,
                Usuario.ROL_CLIENTE
        );

        Task<Boolean> tareaGuardar = new Task<>() {
            @Override
            protected Boolean call() {
                return gestorUsuarios.guardar(nuevoCliente);
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            ocultarCargando();
            if (Boolean.TRUE.equals(tareaGuardar.getValue())) {
                mostrarMensaje("¡Registro exitoso! Redirigiendo al inicio de sesión...", false);
                establecerFormularioDeshabilitado(true);

                // Transición fluida al Login tras breve pausa visual
                PauseTransition redireccion = new PauseTransition(Duration.seconds(1.5));
                redireccion.setOnFinished(ev -> irALogin());
                redireccion.play();
            } else {
                mostrarMensaje("No fue posible guardar el registro. Intenta nuevamente.", true);
            }
        });

        tareaGuardar.setOnFailed(e -> {
            ocultarCargando();
            mostrarMensaje("Ocurrió un error inesperado al guardar el registro.", true);
        });

        Thread hilo = new Thread(tareaGuardar, "belle-registro-save");
        hilo.setDaemon(true);
        hilo.start();
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
    /**
     * Configura filtros de entrada (TextFormatter) para restringir caracteres permitidos:
     * - Nombre: solo letras y espacios.
     * - Identificación: solo números (máximo 15 dígitos).
     * - Celular: solo números (máximo 10 dígitos).
     */
    private void configurarFiltrosTexto() {
        // Filtro para Nombre Completo: solo letras alfabéticas (con tildes/ñ) y espacios
        UnaryOperator<TextFormatter.Change> filtroLetras = cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (Validaciones.esTextoDeNombre(nuevoTexto)) {
                return cambio;
            }
            return null; // Rechazar caracteres no permitidos (ej. dígitos o símbolos)
        };
        campoNombre.setTextFormatter(new TextFormatter<>(filtroLetras));

        // Filtro para Identificación: solo dígitos numéricos (hasta 15 caracteres)
        UnaryOperator<TextFormatter.Change> filtroDigitosIdentificacion = cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (Validaciones.esSoloDigitos(nuevoTexto) && nuevoTexto.length() <= 15) {
                return cambio;
            }
            return null; // Rechazar letras o caracteres especiales
        };
        campoIdentificacion.setTextFormatter(new TextFormatter<>(filtroDigitosIdentificacion));

        // Filtro para Celular: solo dígitos numéricos (hasta 10 dígitos estándar)
        UnaryOperator<TextFormatter.Change> filtroDigitosCelular = cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (Validaciones.esSoloDigitos(nuevoTexto) && nuevoTexto.length() <= 10) {
                return cambio;
            }
            return null; // Rechazar letras o caracteres especiales
        };
        campoCelular.setTextFormatter(new TextFormatter<>(filtroDigitosCelular));
    }

    /**
     * Configura escuchadores de cambios (listeners) en tiempo real:
     * - Limpia automáticamente el resaltado de error al comenzar a escribir.
     * - Compara las contraseñas en tiempo real a medida que el usuario escribe.
     */
    private void configurarValidacionEnTiempoReal() {
        // Limpieza de error en tiempo real para campos individuales
        campoNombre.textProperty().addListener((obs, oldV, newV) -> {
            limpiarError(campoNombre, lblNombre);
            limpiarMensajeSiEsGeneral();
        });

        campoIdentificacion.textProperty().addListener((obs, oldV, newV) -> {
            limpiarError(campoIdentificacion, lblIdentificacion);
            limpiarMensajeSiEsGeneral();
        });

        campoCorreo.textProperty().addListener((obs, oldV, newV) -> {
            limpiarError(campoCorreo, lblCorreo);
            limpiarMensajeSiEsGeneral();
        });

        campoCelular.textProperty().addListener((obs, oldV, newV) -> {
            limpiarError(campoCelular, lblCelular);
            limpiarMensajeSiEsGeneral();
        });

        campoDireccion.textProperty().addListener((obs, oldV, newV) -> {
            limpiarError(campoDireccion, lblDireccion);
            limpiarMensajeSiEsGeneral();
        });

        // Validación y comparación de contraseñas en tiempo real
        campoContrasena.textProperty().addListener((obs, oldV, newV) -> validarContrasenasEnTiempoReal());
        campoConfirmarContrasena.textProperty().addListener((obs, oldV, newV) -> validarContrasenasEnTiempoReal());
    }

    /**
     * Compara las contraseñas en tiempo real mientras el usuario escribe.
     */
    private void validarContrasenasEnTiempoReal() {
        String pass1 = campoContrasena.getText() != null ? campoContrasena.getText() : "";
        String pass2 = campoConfirmarContrasena.getText() != null ? campoConfirmarContrasena.getText() : "";

        // Si el campo de confirmación está vacío, no se marca error aún (usuario no ha llegado a confirmar)
        if (pass2.isEmpty()) {
            limpiarError(campoContrasena, lblContrasena);
            limpiarError(campoConfirmarContrasena, lblConfirmarContrasena);
            removerValido(campoContrasena);
            removerValido(campoConfirmarContrasena);
            if ("Las contraseñas no coinciden.".equals(etiquetaMensaje.getText())) {
                limpiarMensaje();
            }
            return;
        }

        // Si ya hay texto en confirmación, comparamos en tiempo real
        if (pass1.equals(pass2)) {
            limpiarError(campoContrasena, lblContrasena);
            limpiarError(campoConfirmarContrasena, lblConfirmarContrasena);
            marcarValido(campoContrasena);
            marcarValido(campoConfirmarContrasena);
            if ("Las contraseñas no coinciden.".equals(etiquetaMensaje.getText())) {
                limpiarMensaje();
            }
        } else {
            removerValido(campoContrasena);
            removerValido(campoConfirmarContrasena);
            marcarError(campoConfirmarContrasena, lblConfirmarContrasena);
            mostrarMensaje("Las contraseñas no coinciden.", true);
        }
    }

    private void marcarError(TextInputControl campo, Label etiqueta) {
        if (campo != null) {
            removerValido(campo);
            if (!campo.getStyleClass().contains("campo-error")) {
                campo.getStyleClass().add("campo-error");
            }
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

    private void marcarValido(TextInputControl campo) {
        if (campo != null && !campo.getStyleClass().contains("campo-valido")) {
            campo.getStyleClass().add("campo-valido");
        }
    }

    private void removerValido(TextInputControl campo) {
        if (campo != null) {
            campo.getStyleClass().remove("campo-valido");
        }
    }

    private void limpiarMensajeSiEsGeneral() {
        if (etiquetaMensaje != null && "Por favor, completa los campos requeridos marcados en rojo.".equals(etiquetaMensaje.getText())) {
            limpiarMensaje();
        }
    }

    private boolean esCorreoValido(String correo) {
        return Validaciones.esCorreoValido(correo);
    }

    private void mostrarCargando() {
        contenedorCargando.setVisible(true);
        contenedorCargando.setManaged(true);
        establecerFormularioDeshabilitado(true);
    }

    private void ocultarCargando() {
        contenedorCargando.setVisible(false);
        contenedorCargando.setManaged(false);
        establecerFormularioDeshabilitado(false);
    }

    private void establecerFormularioDeshabilitado(boolean deshabilitado) {
        campoNombre.setDisable(deshabilitado);
        campoIdentificacion.setDisable(deshabilitado);
        campoCorreo.setDisable(deshabilitado);
        campoCelular.setDisable(deshabilitado);
        campoDireccion.setDisable(deshabilitado);
        campoContrasena.setDisable(deshabilitado);
        campoConfirmarContrasena.setDisable(deshabilitado);
        botonRegistrarse.setDisable(deshabilitado);
        if (enlaceLogin != null) {
            enlaceLogin.setDisable(deshabilitado);
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
}
