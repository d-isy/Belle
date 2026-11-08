module com.mycompany.belle_store {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // Permitir acceso de reflexión a FXML y Gson
    opens com.mycompany.belle_store to javafx.fxml;
    opens com.mycompany.belle_store.modelos to com.google.gson, javafx.base;

    // Exportar paquetes para uso interno y pruebas
    exports com.mycompany.belle_store;
    exports com.mycompany.belle_store.modelos;
    exports com.mycompany.belle_store.persistencia;
    exports com.mycompany.belle_store.sesion;
}
