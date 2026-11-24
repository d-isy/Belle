package com.mycompany.belle_store;

import com.mycompany.belle_store.persistencia.ConfiguracionRutas;
import com.mycompany.belle_store.persistencia.SeedDatos;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de entrada de la aplicación JavaFX Belle Store.
 * Centraliza la navegación entre vistas y el control de acceso por rol antes de cargar cada FXML.
 */
public class App extends Application {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en App principal
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static Scene scene;
    private static Stage ventana;

    // =========================================================================
    // 3. INICIALIZACIÓN (Ciclo de vida JavaFX)
    // =========================================================================
    @Override
    public void start(Stage stage) throws IOException {
        ventana = stage;
        // Carga de la vista inicial de Login
        scene = new Scene(loadFXML("login"), 1280, 768);
        stage.setScene(scene);
        stage.setTitle(tituloPara("login"));
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    /**
     * Reemplaza el nodo raíz de la escena activa con otra vista FXML,
     * validando primero que la sesión actual tenga permiso para abrirla.
     */
    public static void setRoot(String fxml) throws IOException {
        String vistaPermitida = resolverVistaPermitida(fxml);
        scene.setRoot(loadFXML(vistaPermitida));
        if (ventana != null) {
            ventana.setTitle(tituloPara(vistaPermitida));
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Guardia central de navegación por rol (se ejecuta ANTES de cargar el FXML).
     * Evita el error de redirigir desde {@code initialize()}, donde el FXML en curso
     * terminaría sobrescribiendo la redirección al Login.
     *
     * @param fxml Nombre de la vista solicitada (sin extensión).
     * @return La misma vista si el usuario tiene permiso, o {@code "login"} si no está autorizado.
     */
    public static String resolverVistaPermitida(String fxml) {
        if (fxml == null || fxml.trim().isEmpty()) {
            return "login";
        }
        String vista = fxml.trim();
        switch (vista) {
            case "admin_dashboard":
                return SesionActual.esAdmin() ? vista : "login";
            case "cliente_catalogo":
            case "cliente_carrito":
            case "cliente_deseados":
            case "cliente_historial":
            case "cliente_placeholder":
                return SesionActual.haySesionIniciada() ? vista : "login";
            default:
                return vista;
        }
    }

    /** Título de la ventana para cada vista (nombre del archivo FXML sin extensión). */
    private static String tituloPara(String fxml) {
        switch (fxml) {
            case "login":
                return "Belle Store - Iniciar Sesión";
            case "registro":
                return "Belle Store - Registro";
            case "admin_dashboard":
                return "Belle Store - Administrador";
            case "cliente_catalogo":
                return "Belle Store - Catálogo";
            case "cliente_carrito":
                return "Belle Store - Carrito de Compras";
            case "cliente_deseados":
                return "Belle Store - Lista de Deseados";
            case "cliente_historial":
                return "Belle Store - Historial de Compras";
            case "cliente_placeholder":
                return "Belle Store - Perfil del Cliente";
            default:
                return "Belle Store";
        }
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        System.out.println("[BelleStore] Carpeta de datos: "
                + ConfiguracionRutas.obtenerDirectorioDatos().getAbsolutePath());

        // Inicializar semillas de datos JSON si no existen
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();

        // Lanzar interfaz JavaFX
        launch();
    }
}
