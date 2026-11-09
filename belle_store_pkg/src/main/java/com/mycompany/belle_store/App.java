package com.mycompany.belle_store;

import com.mycompany.belle_store.persistencia.SeedDatos;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de entrada de la aplicación JavaFX Belle Store.
 */
public class App extends Application {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en App principal
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static Scene scene;

    // =========================================================================
    // 3. INICIALIZACIÓN (Ciclo de vida JavaFX)
    // =========================================================================
    @Override
    public void start(Stage stage) throws IOException {
        // Carga de la vista inicial de Login
        scene = new Scene(loadFXML("login"), 1280, 768);
        stage.setScene(scene);
        stage.setTitle("Belle Store - Iniciar Sesión");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    /**
     * Reemplaza el nodo raíz de la escena activa con otra vista FXML.
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        // Inicializar semillas de datos JSON si no existen
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();

        // Lanzar interfaz JavaFX
        launch();
    }
}
