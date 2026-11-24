package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorCarrito;
import com.mycompany.belle_store.persistencia.GestorDeseados;
import com.mycompany.belle_store.persistencia.GestorImagenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de la vista de Lista de Deseados ({@code cliente_deseados.fxml}).
 * Utiliza tarjetas horizontales diferenciadas del catálogo (con selector de cantidad,
 * corazón para retirar de deseados y botón para pasar al carrito respetando el stock).
 */
public class ClienteDeseadosController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private ClienteHeaderController headerController;

    @FXML
    private Label lblMensajeEstado;

    @FXML
    private ScrollPane scrollDeseados;

    @FXML
    private VBox contenedorDeseados;

    @FXML
    private VBox estadoVacio;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorDeseados gestorDeseados = new GestorDeseados();
    private final GestorProductos gestorProductos = new GestorProductos();
    private final GestorCarrito gestorCarrito = new GestorCarrito();

    // Cantidad elegida temporalmente por cada tarjeta de producto deseado
    private final Map<String, Integer> cantidadesSeleccionadas = new HashMap<>();
    private PauseTransition temporizadorMensaje;

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        cargarDeseados();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    @FXML
    private void irAlCatalogo(ActionEvent event) {
        try {
            App.setRoot("cliente_catalogo");
        } catch (IOException e) {
            mostrarMensajeTemporal("No fue posible abrir el catálogo.", true);
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private void cargarDeseados() {
        String cedula = obtenerCedulaCliente();
        List<String> ids = gestorDeseados.obtenerProductosIds(cedula);

        contenedorDeseados.getChildren().clear();
        int validos = 0;

        for (String idProducto : ids) {
            Producto producto = gestorProductos.buscarPorId(idProducto);
            if (producto == null) {
                gestorDeseados.eliminarDeseado(cedula, idProducto);
                continue;
            }
            validos++;
            contenedorDeseados.getChildren().add(crearTarjetaDeseadoHorizontal(producto));
        }

        boolean vacio = validos == 0;
        scrollDeseados.setVisible(!vacio);
        scrollDeseados.setManaged(!vacio);
        estadoVacio.setVisible(vacio);
        estadoVacio.setManaged(vacio);

        if (headerController != null) {
            headerController.actualizarContadores();
        }
    }

    /**
     * Construye una tarjeta horizontal de producto deseado (imagen, info + selector de cantidad,
     * botón corazón rojo para quitar y botón carrito para agregar al carrito).
     */
    private HBox crearTarjetaDeseadoHorizontal(Producto producto) {
        HBox tarjeta = new HBox(20);
        tarjeta.getStyleClass().add("tarjeta-horizontal-cliente");
        tarjeta.setAlignment(Pos.CENTER_LEFT);

        boolean agotado = producto.getStock() <= 0;
        int cantidadActual = cantidadesSeleccionadas.getOrDefault(producto.getId(), 1);
        if (!agotado && cantidadActual > producto.getStock()) {
            cantidadActual = producto.getStock();
        }
        cantidadesSeleccionadas.put(producto.getId(), Math.max(1, cantidadActual));

        // 1. Miniatura izquierda
        ImageView imageView = new ImageView();
        imageView.setFitWidth(110);
        imageView.setFitHeight(110);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        cargarImagenConFallback(imageView, producto.getImagen(), 110, 110);

        // 2. Información central + selector de cantidad
        VBox centro = new VBox(8);
        HBox.setHgrow(centro, Priority.ALWAYS);

        HBox filaTitulo = new HBox(10);
        filaTitulo.setAlignment(Pos.CENTER_LEFT);
        Label lblNombre = new Label(producto.getNombre());
        lblNombre.getStyleClass().add("titulo-tarjeta-horizontal");
        filaTitulo.getChildren().add(lblNombre);

        Label badgeEstado = new Label(producto.getEstado());
        badgeEstado.getStyleClass().add(agotado ? "badge-estado-agotado" : "badge-estado-disponible");
        filaTitulo.getChildren().add(badgeEstado);

        Label lblPrecio = new Label(producto.getPrecioFormateado());
        lblPrecio.getStyleClass().add("precio-tarjeta-horizontal");

        // Selector "Cantidad [+] N [-]"
        Label lblEtqCant = new Label("Cantidad");
        lblEtqCant.getStyleClass().add("etiqueta-cantidad");

        Label lblValorCant = new Label(String.valueOf(cantidadesSeleccionadas.get(producto.getId())));
        lblValorCant.getStyleClass().add("valor-cantidad");

        Button btnMas = new Button("+");
        btnMas.getStyleClass().add("boton-cantidad");
        btnMas.setDisable(agotado);
        btnMas.setOnAction(e -> {
            int actual = cantidadesSeleccionadas.getOrDefault(producto.getId(), 1);
            if (actual < producto.getStock()) {
                actual++;
                cantidadesSeleccionadas.put(producto.getId(), actual);
                lblValorCant.setText(String.valueOf(actual));
            } else {
                mostrarMensajeTemporal("Solo hay " + producto.getStock()
                        + " unidad(es) disponibles de " + producto.getNombre() + ".", true);
            }
        });

        Button btnMenos = new Button("-");
        btnMenos.getStyleClass().add("boton-cantidad");
        btnMenos.setDisable(agotado);
        btnMenos.setOnAction(e -> {
            int actual = cantidadesSeleccionadas.getOrDefault(producto.getId(), 1);
            if (actual > 1) {
                actual--;
                cantidadesSeleccionadas.put(producto.getId(), actual);
                lblValorCant.setText(String.valueOf(actual));
            }
        });

        HBox filaCantidad = new HBox(10, lblEtqCant, btnMas, lblValorCant, btnMenos);
        filaCantidad.setAlignment(Pos.CENTER_LEFT);

        centro.getChildren().addAll(filaTitulo, lblPrecio, filaCantidad);

        // 3. Acciones a la derecha: Corazón rojo (quitar) + Carrito (agregar al carrito)
        Button btnQuitar = new Button("♥");
        btnQuitar.getStyleClass().setAll("button", "boton-corazon-deseados");
        btnQuitar.setTooltip(new Tooltip("Quitar de deseados"));
        btnQuitar.setOnAction(e -> quitarDeDeseados(producto));

        ImageView iconoCarrito = new ImageView(
                new Image(App.class.getResource("img/cart.png").toExternalForm(), 24, 24, true, true)
        );
        Button btnAlCarrito = new Button();
        btnAlCarrito.setGraphic(iconoCarrito);
        btnAlCarrito.getStyleClass().add("boton-icono-accion-deseados");
        btnAlCarrito.setTooltip(new Tooltip(agotado ? "Producto agotado" : "Añadir al carrito"));
        btnAlCarrito.setDisable(agotado);
        btnAlCarrito.setOnAction(e -> pasarAlCarrito(producto));

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.SOMETIMES);

        HBox acciones = new HBox(14, btnQuitar, btnAlCarrito);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        tarjeta.getChildren().addAll(imageView, centro, espaciador, acciones);
        return tarjeta;
    }

    private void quitarDeDeseados(Producto producto) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;
        gestorDeseados.eliminarDeseado(cedula, producto.getId());
        cantidadesSeleccionadas.remove(producto.getId());
        mostrarMensajeTemporal(producto.getNombre() + " retirado de tu lista de deseados.", false);
        cargarDeseados();
    }

    private void pasarAlCarrito(Producto producto) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;

        Producto vigente = gestorProductos.buscarPorId(producto.getId());
        if (vigente == null || vigente.getStock() <= 0) {
            mostrarMensajeTemporal("Este producto se encuentra agotado.", true);
            cargarDeseados();
            return;
        }

        int cantidad = cantidadesSeleccionadas.getOrDefault(producto.getId(), 1);
        boolean ok = gestorCarrito.agregarProducto(cedula, vigente.getId(), cantidad, vigente.getStock());
        if (ok) {
            mostrarMensajeTemporal("Se añadieron " + cantidad + " unidad(es) de "
                    + vigente.getNombre() + " al carrito.", false);
            if (headerController != null) {
                headerController.actualizarContadores();
            }
        } else {
            mostrarMensajeTemporal("No se puede superar el stock disponible (" + vigente.getStock()
                    + ") sumando lo que ya tienes en el carrito.", true);
        }
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

    private void mostrarMensajeTemporal(String texto, boolean esError) {
        if (lblMensajeEstado == null) return;
        lblMensajeEstado.setText(texto);
        lblMensajeEstado.getStyleClass().setAll("label", esError ? "mensaje-error" : "mensaje-exito");
        lblMensajeEstado.setVisible(true);
        lblMensajeEstado.setManaged(true);

        if (temporizadorMensaje != null) {
            temporizadorMensaje.stop();
        }
        temporizadorMensaje = new PauseTransition(Duration.seconds(3));
        temporizadorMensaje.setOnFinished(e -> {
            lblMensajeEstado.setVisible(false);
            lblMensajeEstado.setManaged(false);
        });
        temporizadorMensaje.play();
    }

    private String obtenerCedulaCliente() {
        Usuario actual = SesionActual.getUsuarioLogueado();
        return actual != null ? actual.getNumeroIdentificacion() : null;
    }
}
