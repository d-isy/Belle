package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.ItemCarrito;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.ResumenCompra;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorCarrito;
import com.mycompany.belle_store.persistencia.GestorImagenes;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.util.Formato;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de la vista de Carrito de Compras ({@code cliente_carrito.fxml}).
 * Replica el frame {@code 48:296} de Figma, aplica la regla oficial de IVA (19%) y envío escalonado,
 * valida inventario en vivo y procesa la compra en segundo plano con {@link Task}.
 */
public class ClienteCarritoController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private ClienteHeaderController headerController;

    @FXML
    private ScrollPane scrollItems;

    @FXML
    private VBox contenedorItems;

    @FXML
    private VBox estadoVacio;

    @FXML
    private Label lblSubtotal;

    @FXML
    private Label lblEnvio;

    @FXML
    private Label lblImpuestos;

    @FXML
    private Label lblNotaEnvio;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnPagar;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final ButtonType BTN_ACEPTAR = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType BTN_CANCELAR = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

    private final GestorCarrito gestorCarrito = new GestorCarrito();
    private final GestorProductos gestorProductos = new GestorProductos();
    private final GestorOrdenes gestorOrdenes = new GestorOrdenes();

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        cargarCarrito();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    @FXML
    private void irAlCatalogo(ActionEvent event) {
        try {
            App.setRoot("cliente_catalogo");
        } catch (IOException e) {
            mostrarMensaje("No fue posible abrir el catálogo.", true);
        }
    }

    @FXML
    private void manejarPago(ActionEvent event) {
        Usuario cliente = SesionActual.getUsuarioLogueado();
        if (cliente == null) return;

        ResumenCompra resumen = gestorCarrito.calcularResumen(cliente.getNumeroIdentificacion(), gestorProductos);
        if (resumen.getSubtotal() <= 0) {
            mostrarMensaje("Agrega al menos un producto antes de pagar.", true);
            return;
        }

        Optional<String> direccionConfirmada = mostrarDialogoConfirmacionCompra(cliente, resumen);
        if (direccionConfirmada.isEmpty()) {
            return;
        }

        String direccion = direccionConfirmada.get();
        btnPagar.setDisable(true);
        btnPagar.setText("PROCESANDO COMPRA...");
        limpiarMensaje();

        // Ejecutar validación de stock, descuento de inventario y registro de Orden fuera del hilo UI
        Task<Orden> tareaCompra = new Task<>() {
            @Override
            protected Orden call() {
                return gestorOrdenes.procesarCompra(cliente, direccion, gestorCarrito, gestorProductos);
            }
        };

        tareaCompra.setOnSucceeded(e -> {
            Orden ordenCreada = tareaCompra.getValue();
            btnPagar.setText("PAGAR");
            if (headerController != null) {
                headerController.actualizarContadores();
            }
            mostrarAlertaExitoCompra(ordenCreada);
            try {
                App.setRoot("cliente_historial");
            } catch (IOException ex) {
                cargarCarrito();
            }
        });

        tareaCompra.setOnFailed(e -> {
            btnPagar.setText("PAGAR");
            Throwable error = tareaCompra.getException();
            String msg = (error != null && error.getMessage() != null)
                    ? error.getMessage()
                    : "No fue posible procesar tu compra. Intenta nuevamente.";
            mostrarMensaje(msg, true);
            cargarCarrito();
        });

        Thread hilo = new Thread(tareaCompra, "belle-procesar-compra");
        hilo.setDaemon(true);
        hilo.start();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private void cargarCarrito() {
        String cedula = obtenerCedulaCliente();
        List<ItemCarrito> items = gestorCarrito.obtenerItems(cedula);

        contenedorItems.getChildren().clear();
        int itemsValidos = 0;

        for (ItemCarrito item : items) {
            Producto producto = gestorProductos.buscarPorId(item.getProductoId());
            if (producto == null) {
                gestorCarrito.eliminarProducto(cedula, item.getProductoId());
                continue;
            }
            itemsValidos++;
            contenedorItems.getChildren().add(crearTarjetaItemCarrito(producto, item.getCantidad()));
        }

        boolean vacio = itemsValidos == 0;
        scrollItems.setVisible(!vacio);
        scrollItems.setManaged(!vacio);
        estadoVacio.setVisible(vacio);
        estadoVacio.setManaged(vacio);
        btnPagar.setDisable(vacio);

        actualizarResumen(cedula);
        if (headerController != null) {
            headerController.actualizarContadores();
        }
    }

    /**
     * Construye la tarjeta horizontal de cada ítem del carrito (fiel al frame 48:296 de Figma).
     */
    private HBox crearTarjetaItemCarrito(Producto producto, int cantidad) {
        HBox tarjeta = new HBox(18);
        tarjeta.getStyleClass().add("tarjeta-horizontal-cliente");
        tarjeta.setAlignment(Pos.CENTER_LEFT);

        // Miniatura izquierda
        ImageView imageView = new ImageView();
        imageView.setFitWidth(105);
        imageView.setFitHeight(105);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        cargarImagenConFallback(imageView, producto.getImagen(), 105, 105);

        // Columna central: nombre, precio unitario y controles de cantidad
        VBox centro = new VBox(8);
        HBox.setHgrow(centro, Priority.ALWAYS);

        Label lblNombre = new Label(producto.getNombre());
        lblNombre.getStyleClass().add("titulo-tarjeta-horizontal");

        Label lblPrecioUnitario = new Label(producto.getPrecioFormateado());
        lblPrecioUnitario.getStyleClass().add("precio-tarjeta-horizontal");

        // Controles de cantidad: "Cantidad [+] N [-]" + subtotal a la derecha
        Label lblEtiquetaCant = new Label("Cantidad");
        lblEtiquetaCant.getStyleClass().add("etiqueta-cantidad");

        Button btnMas = new Button("+");
        btnMas.getStyleClass().add("boton-cantidad");
        btnMas.setOnAction(e -> cambiarCantidad(producto, cantidad + 1));

        Label lblCantidad = new Label(String.valueOf(cantidad));
        lblCantidad.getStyleClass().add("valor-cantidad");

        Button btnMenos = new Button("-");
        btnMenos.getStyleClass().add("boton-cantidad");
        btnMenos.setOnAction(e -> cambiarCantidad(producto, cantidad - 1));

        Label lblSubtotalItem = new Label("Subtotal: " + Formato.moneda(producto.getPrecio() * cantidad));
        lblSubtotalItem.getStyleClass().add("subtotal-item-carrito");

        Button btnQuitar = new Button("Quitar");
        btnQuitar.getStyleClass().add("boton-quitar-item");
        btnQuitar.setOnAction(e -> eliminarItem(producto));

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        HBox filaInferior = new HBox(10, lblEtiquetaCant, btnMas, lblCantidad, btnMenos,
                espaciador, lblSubtotalItem, btnQuitar);
        filaInferior.setAlignment(Pos.CENTER_LEFT);

        centro.getChildren().addAll(lblNombre, lblPrecioUnitario, filaInferior);
        tarjeta.getChildren().addAll(imageView, centro);
        return tarjeta;
    }

    private void cambiarCantidad(Producto producto, int nuevaCantidad) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;
        limpiarMensaje();

        if (nuevaCantidad <= 0) {
            eliminarItem(producto);
            return;
        }

        Producto vigente = gestorProductos.buscarPorId(producto.getId());
        if (vigente == null || vigente.getStock() <= 0) {
            mostrarMensaje(producto.getNombre() + " ya no tiene stock disponible.", true);
            return;
        }

        boolean ok = gestorCarrito.actualizarCantidad(cedula, vigente.getId(), nuevaCantidad, vigente.getStock());
        if (!ok) {
            mostrarMensaje("Solo hay " + vigente.getStock() + " unidad(es) disponibles de " + vigente.getNombre() + ".", true);
            return;
        }
        cargarCarrito();
    }

    private void eliminarItem(Producto producto) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;
        gestorCarrito.eliminarProducto(cedula, producto.getId());
        limpiarMensaje();
        cargarCarrito();
    }

    private void actualizarResumen(String cedula) {
        ResumenCompra resumen = gestorCarrito.calcularResumen(cedula, gestorProductos);
        lblSubtotal.setText(resumen.getSubtotalFormateado());
        lblEnvio.setText(resumen.getCostoEnvioFormateado());
        lblImpuestos.setText(resumen.getImpuestosFormateado());
        lblTotal.setText(resumen.getTotalFormateado());

        if (resumen.getSubtotal() <= 0) {
            lblNotaEnvio.setText("Envío gratis en compras desde " + Formato.moneda(ResumenCompra.UMBRAL_ENVIO_GRATIS) + ".");
        } else if (resumen.esEnvioGratis()) {
            lblNotaEnvio.setText("¡Aplicaste a envío gratis por superar " + Formato.moneda(ResumenCompra.UMBRAL_ENVIO_GRATIS) + "!");
        } else {
            double faltante = ResumenCompra.UMBRAL_ENVIO_GRATIS - resumen.getSubtotal();
            lblNotaEnvio.setText("Te faltan " + Formato.moneda(faltante) + " para obtener envío gratis.");
        }
    }

    private Optional<String> mostrarDialogoConfirmacionCompra(Usuario cliente, ResumenCompra resumen) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Confirmar compra - Belle Store");
        dialogo.setHeaderText("Verifica tu dirección de entrega y el total de tu orden");
        Window dueno = btnPagar != null && btnPagar.getScene() != null ? btnPagar.getScene().getWindow() : null;
        if (dueno != null) {
            dialogo.initOwner(dueno);
        }
        dialogo.getDialogPane().getStylesheets().add(
                App.class.getResource("css/estilos.css").toExternalForm()
        );

        ButtonType btnConfirmar = new ButtonType("Confirmar compra", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnConfirmar, BTN_CANCELAR);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 12, 20));

        TextField txtDireccion = new TextField(cliente.getDireccion() != null ? cliente.getDireccion() : "");
        txtDireccion.setPromptText("Ej. Calle 45 # 10 - 20");
        txtDireccion.setPrefWidth(300);

        Label lblError = new Label();
        lblError.getStyleClass().add("mensaje-error");
        lblError.setVisible(false);
        lblError.setManaged(false);

        grid.add(new Label("Cliente:"), 0, 0);
        grid.add(new Label(cliente.getNombreCompleto() + " (" + cliente.getNumeroIdentificacion() + ")"), 1, 0);
        grid.add(new Label("Dirección de entrega:"), 0, 1);
        grid.add(txtDireccion, 1, 1);
        grid.add(new Separator(), 0, 2, 2, 1);
        grid.add(new Label("Subtotal:"), 0, 3);
        grid.add(new Label(resumen.getSubtotalFormateado()), 1, 3);
        grid.add(new Label("Envío:"), 0, 4);
        grid.add(new Label(resumen.getCostoEnvioFormateado()), 1, 4);
        grid.add(new Label("Impuestos (IVA 19%):"), 0, 5);
        grid.add(new Label(resumen.getImpuestosFormateado()), 1, 5);

        Label lblTotalEtq = new Label("Total a pagar:");
        lblTotalEtq.getStyleClass().add("etiqueta-estado-dialogo");
        Label lblTotalVal = new Label(resumen.getTotalFormateado());
        lblTotalVal.getStyleClass().add("etiqueta-estado-dialogo");
        grid.add(lblTotalEtq, 0, 6);
        grid.add(lblTotalVal, 1, 6);
        grid.add(lblError, 0, 7, 2, 1);

        dialogo.getDialogPane().setContent(grid);

        // Validar dirección sin cerrar el diálogo si está vacía
        Button okButton = (Button) dialogo.getDialogPane().lookupButton(btnConfirmar);
        okButton.addEventFilter(ActionEvent.ACTION, evento -> {
            String dir = txtDireccion.getText() != null ? txtDireccion.getText().trim() : "";
            if (dir.length() < 4) {
                txtDireccion.getStyleClass().add("campo-error");
                lblError.setText("Ingresa una dirección de entrega válida (mínimo 4 caracteres).");
                lblError.setVisible(true);
                lblError.setManaged(true);
                evento.consume();
            }
        });

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnConfirmar) {
            return Optional.of(txtDireccion.getText().trim());
        }
        return Optional.empty();
    }

    private void mostrarAlertaExitoCompra(Orden orden) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("¡Compra realizada!");
        alerta.setHeaderText("Tu orden #" + orden.getId() + " fue registrada con éxito");
        alerta.setContentText("Total pagado: " + orden.getValorTotalFormateado()
                + "\nEstado inicial: " + orden.getEstado()
                + "\nPuedes hacer seguimiento desde tu Historial de compras.");
        alerta.getButtonTypes().setAll(BTN_ACEPTAR);
        Window dueno = btnPagar != null && btnPagar.getScene() != null ? btnPagar.getScene().getWindow() : null;
        if (dueno != null) {
            alerta.initOwner(dueno);
        }
        alerta.showAndWait();
    }

    private void cargarImagenConFallback(ImageView vista, String referencia, double ancho, double alto) {
        String url = GestorImagenes.resolverUrlOPlaceholder(referencia);
        boolean remota = url.startsWith("http://") || url.startsWith("https://");
        Image img = new Image(url, ancho, alto, true, true, remota);
        img.errorProperty().addListener((obs, a, fallo) -> {
            if (Boolean.TRUE.equals(fallo)) {
                vista.setImage(new Image(GestorImagenes.resolverUrl(GestorImagenes.PLACEHOLDER),
                        ancho, alto, true, true));
            }
        });
        vista.setImage(img);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        if (lblMensaje == null) return;
        lblMensaje.setText(texto);
        lblMensaje.getStyleClass().setAll("label", esError ? "mensaje-error" : "mensaje-exito");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void limpiarMensaje() {
        if (lblMensaje == null) return;
        lblMensaje.setText("");
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }

    private String obtenerCedulaCliente() {
        Usuario actual = SesionActual.getUsuarioLogueado();
        return actual != null ? actual.getNumeroIdentificacion() : null;
    }
}
