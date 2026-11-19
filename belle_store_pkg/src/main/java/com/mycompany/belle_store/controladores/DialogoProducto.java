package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.persistencia.GestorImagenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.validacion.Validaciones;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo modal compartido para crear o editar un {@link Producto}.
 * Valida los campos en línea (resaltado rojo + mensaje sin cerrar el diálogo),
 * deriva automáticamente el estado según el stock e incluye selector de imagen con vista previa.
 */
public final class DialogoProducto {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica (diálogo construido por código)
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    /** Categorías oficiales del diseño de Figma (las mismas del filtro lateral). */
    public static final List<String> CATEGORIAS = Arrays.asList("CAMISAS", "PANTALONES", "ACCESORIOS");

    private static final ButtonType BTN_CANCELAR = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor privado)
    // =========================================================================
    private DialogoProducto() {
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    /**
     * Abre el diálogo modal para crear ({@code original == null}) o editar un producto.
     *
     * @param dueno Ventana padre para centrar y vincular el modal.
     * @param original Producto a editar, o {@code null} si es un alta nueva.
     * @param gestorProductos Gestor usado para sugerir el siguiente ID y validar unicidad.
     * @return El nuevo objeto {@link Producto} listo para persistir, o vacío si se canceló.
     */
    public static Optional<Producto> mostrar(Window dueno, Producto original, GestorProductos gestorProductos) {
        boolean esNuevo = original == null;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(esNuevo ? "Añadir producto - Belle Store" : "Editar producto - " + original.getId());
        dialogo.setHeaderText(esNuevo ? "Ingresa los datos del nuevo producto:" : "Modifica los datos del producto:");
        aplicarEstilos(dialogo, dueno);

        ButtonType btnGuardar = new ButtonType(esNuevo ? "Guardar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, BTN_CANCELAR);

        TextField txtId = new TextField(esNuevo ? gestorProductos.generarSiguienteId() : original.getId());
        txtId.setDisable(!esNuevo); // El ID es la clave única: no se edita
        TextField txtNombre = new TextField(esNuevo ? "" : original.getNombre());
        txtNombre.setPromptText("Ej. CHAQUETA DE CUERO");

        ComboBox<String> cbCategoria = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        cbCategoria.setValue(esNuevo || !CATEGORIAS.contains(original.getCategoria())
                ? CATEGORIAS.get(0) : original.getCategoria());

        TextField txtStock = new TextField(esNuevo ? "1" : String.valueOf(original.getStock()));
        Label lblEstado = new Label();
        lblEstado.getStyleClass().add("etiqueta-estado-dialogo");
        Runnable actualizarEstado = () -> {
            try {
                int stock = Integer.parseInt(txtStock.getText().trim());
                lblEstado.setText(Validaciones.estadoPorStock(stock) + " (se calcula según el stock)");
            } catch (NumberFormatException e) {
                lblEstado.setText("—");
            }
        };
        txtStock.textProperty().addListener((obs, a, b) -> actualizarEstado.run());
        actualizarEstado.run();

        TextField txtPrecio = new TextField(esNuevo ? "" : String.valueOf((long) original.getPrecio()));
        txtPrecio.setPromptText("Valor en pesos, sin puntos (ej. 180000)");

        // --- Imagen: ruta editable + botón Examinar + vista previa ---
        TextField txtImagen = new TextField(esNuevo ? GestorImagenes.PLACEHOLDER : original.getImagen());
        txtImagen.setPromptText("img/..., imagenes/... o https://...");
        txtImagen.setPrefWidth(260);
        Button btnExaminar = new Button("Examinar...");
        btnExaminar.getStyleClass().add("boton-secundario");

        ImageView vistaPrevia = new ImageView();
        vistaPrevia.setFitWidth(90);
        vistaPrevia.setFitHeight(90);
        vistaPrevia.setPreserveRatio(true);
        Runnable actualizarVistaPrevia = () -> asignarMiniatura(vistaPrevia, txtImagen.getText(), 90);
        txtImagen.textProperty().addListener((obs, a, b) -> actualizarVistaPrevia.run());
        actualizarVistaPrevia.run();

        Label lblError = new Label();
        lblError.getStyleClass().add("mensaje-error");
        lblError.setWrapText(true);
        lblError.setMaxWidth(380);

        btnExaminar.setOnAction(e -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Selecciona la imagen del producto");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Imágenes (png, jpg, gif, bmp)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File elegido = selector.showOpenDialog(dialogo.getDialogPane().getScene().getWindow());
            if (elegido != null) {
                try {
                    txtImagen.setText(GestorImagenes.importarImagen(elegido));
                    lblError.setText("");
                } catch (IOException ex) {
                    lblError.setText(ex.getMessage());
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("ID del producto:"), 0, 0);
        grid.add(txtId, 1, 0, 2, 1);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1, 2, 1);
        grid.add(new Label("Categoría:"), 0, 2);
        grid.add(cbCategoria, 1, 2, 2, 1);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(txtStock, 1, 3, 2, 1);
        grid.add(new Label("Estado:"), 0, 4);
        grid.add(lblEstado, 1, 4, 2, 1);
        grid.add(new Label("Precio (COP):"), 0, 5);
        grid.add(txtPrecio, 1, 5, 2, 1);
        grid.add(new Label("Imagen:"), 0, 6);
        grid.add(txtImagen, 1, 6);
        grid.add(btnExaminar, 2, 6);
        grid.add(vistaPrevia, 1, 7);
        grid.add(lblError, 0, 8, 3, 1);
        dialogo.getDialogPane().setContent(grid);

        // Validación sin cerrar el diálogo: se consume el evento del botón Guardar si hay errores
        Button botonOk = (Button) dialogo.getDialogPane().lookupButton(btnGuardar);
        botonOk.addEventFilter(ActionEvent.ACTION, evento -> {
            limpiarResaltado(txtId, txtNombre, txtStock, txtPrecio);
            String error = validarFormulario(esNuevo, gestorProductos, txtId, txtNombre, txtStock, txtPrecio);
            if (error != null) {
                lblError.setText(error);
                evento.consume();
            }
        });

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == btnGuardar) {
            int stock = Integer.parseInt(txtStock.getText().trim());
            String imagen = txtImagen.getText() == null || txtImagen.getText().trim().isEmpty()
                    ? GestorImagenes.PLACEHOLDER : txtImagen.getText().trim();
            return Optional.of(new Producto(
                    txtId.getText().trim(),
                    imagen,
                    txtNombre.getText().trim().toUpperCase(),
                    cbCategoria.getValue(),
                    stock,
                    Double.parseDouble(txtPrecio.getText().trim())
            ));
        }
        return Optional.empty();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private static String validarFormulario(boolean esNuevo, GestorProductos gestorProductos,
                                            TextField txtId, TextField txtNombre,
                                            TextField txtStock, TextField txtPrecio) {
        String id = txtId.getText() == null ? "" : txtId.getText().trim();
        if (esNuevo) {
            if (!id.matches("[A-Za-z0-9-]{1,12}")) {
                resaltar(txtId);
                return "El ID debe tener entre 1 y 12 caracteres (letras, números o guion).";
            }
            if (gestorProductos.buscarPorId(id) != null) {
                resaltar(txtId);
                return "Ya existe un producto con el ID " + id + ".";
            }
        }
        String error = Validaciones.validarProducto(txtNombre.getText(), txtStock.getText(), txtPrecio.getText());
        if (error != null) {
            if (error.startsWith("El nombre")) resaltar(txtNombre);
            else if (error.startsWith("El stock")) resaltar(txtStock);
            else resaltar(txtPrecio);
        }
        return error;
    }

    /**
     * Asigna al ImageView la miniatura de la referencia indicada; ante cualquier error
     * (archivo inexistente, URL caída) muestra la imagen genérica.
     */
    static void asignarMiniatura(ImageView destino, String referencia, double tamano) {
        String url = GestorImagenes.resolverUrl(referencia);
        if (url == null) {
            destino.setImage(imagenGenerica(tamano));
            return;
        }
        try {
            Image imagen = new Image(url, tamano * 2, tamano * 2, true, true, true);
            destino.setImage(imagen);
            if (imagen.isError()) {
                destino.setImage(imagenGenerica(tamano));
            } else {
                imagen.errorProperty().addListener((obs, antes, ahora) -> {
                    if (ahora && destino.getImage() == imagen) {
                        destino.setImage(imagenGenerica(tamano));
                    }
                });
            }
        } catch (Exception e) {
            destino.setImage(imagenGenerica(tamano));
        }
    }

    private static Image imagenGenerica(double tamano) {
        String url = GestorImagenes.resolverUrl(GestorImagenes.PLACEHOLDER);
        return url != null ? new Image(url, tamano * 2, tamano * 2, true, true) : null;
    }

    static void aplicarEstilos(Dialog<?> dialogo, Window dueno) {
        try {
            dialogo.getDialogPane().getStylesheets().add(
                    DialogoProducto.class.getResource("/com/mycompany/belle_store/css/estilos.css").toExternalForm());
        } catch (NullPointerException ignorada) {
            // Sin estilos personalizados: se usa el aspecto por defecto
        }
        if (dueno != null) {
            dialogo.initOwner(dueno);
        }
    }

    static void resaltar(TextField campo) {
        if (!campo.getStyleClass().contains("campo-error")) {
            campo.getStyleClass().add("campo-error");
        }
    }

    static void limpiarResaltado(TextField... campos) {
        for (TextField campo : campos) {
            campo.getStyleClass().remove("campo-error");
        }
    }
}
