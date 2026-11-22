package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.modelos.Orden;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo modal para actualizar el estado logístico y la dirección de entrega de una {@link Orden}.
 * Retorna una copia nueva de la orden para que el objeto en la tabla no cambie si falla el guardado.
 */
public final class DialogoOrden {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica (diálogo construido por código)
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    public static final List<String> ESTADOS_ORDEN = Arrays.asList("No Entregado", "Entregado", "Pendiente");

    private static final ButtonType BTN_CANCELAR = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private DialogoOrden() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    /**
     * Muestra el diálogo de edición de una orden existente.
     *
     * @param dueno Ventana padre del diálogo.
     * @param orden Orden seleccionada en la tabla.
     * @param nombreCliente Nombre del cliente (puede ser {@code null} si no está registrado).
     * @return Copia actualizada de la orden lista para guardar, o vacío si se canceló.
     */
    public static Optional<Orden> mostrar(Window dueno, Orden orden, String nombreCliente) {
        if (orden == null) {
            return Optional.empty();
        }

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Actualizar orden #" + orden.getId());
        dialogo.setHeaderText("Modifica el estado o la dirección de entrega:");
        DialogoProducto.aplicarEstilos(dialogo, dueno);

        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnActualizar, BTN_CANCELAR);

        TextField txtId = new TextField(orden.getId());
        txtId.setDisable(true);

        String etiquetaCliente = nombreCliente != null
                ? orden.getClienteIdentificacion() + " - " + nombreCliente
                : orden.getClienteIdentificacion();
        TextField txtCliente = new TextField(etiquetaCliente);
        txtCliente.setDisable(true);

        TextField txtDireccion = new TextField(orden.getDireccion());
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList(ESTADOS_ORDEN));
        cbEstado.setValue(orden.getEstado());

        Label lblError = new Label();
        lblError.getStyleClass().add("mensaje-error");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("Orden:"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Cliente:"), 0, 1);
        grid.add(txtCliente, 1, 1);
        grid.add(new Label("Dirección:"), 0, 2);
        grid.add(txtDireccion, 1, 2);
        grid.add(new Label("Estado:"), 0, 3);
        grid.add(cbEstado, 1, 3);
        grid.add(lblError, 0, 4, 2, 1);
        dialogo.getDialogPane().setContent(grid);

        Button botonOk = (Button) dialogo.getDialogPane().lookupButton(btnActualizar);
        botonOk.addEventFilter(ActionEvent.ACTION, evento -> {
            DialogoProducto.limpiarResaltado(txtDireccion);
            String dir = txtDireccion.getText() == null ? "" : txtDireccion.getText().trim();
            if (dir.length() < 5) {
                DialogoProducto.resaltar(txtDireccion);
                lblError.setText("La dirección debe tener al menos 5 caracteres.");
                evento.consume();
            }
        });

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnActualizar) {
            return Optional.of(new Orden(
                    orden.getId(),
                    orden.getClienteIdentificacion(),
                    orden.getArticulos(),
                    txtDireccion.getText().trim(),
                    cbEstado.getValue(),
                    orden.getFecha()
            ));
        }
        return Optional.empty();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES - No aplica
    // =========================================================================
}
