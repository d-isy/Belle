module com.mycompany.belle_store {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.belle_store to javafx.fxml;
    exports com.mycompany.belle_store;
}
