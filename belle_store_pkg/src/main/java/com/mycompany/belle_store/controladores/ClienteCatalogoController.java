package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorCarrito;
import com.mycompany.belle_store.persistencia.GestorDeseados;
import com.mycompany.belle_store.persistencia.GestorImagenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador de la vista principal de Catálogo para el cliente ({@code cliente_catalogo.fxml}).
 * Replica el frame {@code 42:193} de Figma con cuadrícula de tarjetas, filtro lateral por categoría,
 * búsqueda reactiva desde la lupa de cabecera, botón de deseados (corazón) y adición al carrito.
 */
public class ClienteCatalogoController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private ClienteHeaderController headerController;

    @FXML
    private HBox barraBusqueda;

    @FXML
    private TextField campoBusqueda;

    @FXML
    private Label lblCategoriaActual;

    @FXML
    private Label lblMensajeEstado;

    @FXML
    private ScrollPane scrollCatalogo;

    @FXML
    private FlowPane contenedorProductos;

    @FXML
    private VBox estadoVacio;

    @FXML
    private Button btnCatTodas;

    @FXML
    private Button btnCatCamisas;

    @FXML
    private Button btnCatPantalones;

    @FXML
    private Button btnCatAccesorios;

    @FXML
    private Button btnSexoTodos;

    @FXML
    private Button btnSexoHombre;

    @FXML
    private Button btnSexoMujer;

    @FXML
    private Button btnSexoUnisex;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorProductos gestorProductos = new GestorProductos();
    private final GestorCarrito gestorCarrito = new GestorCarrito();
    private final GestorDeseados gestorDeseados = new GestorDeseados();

    private String categoriaActiva = "TODAS";
    private String sexoActivo = "TODOS";
    private PauseTransition temporizadorMensaje;

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        if (headerController != null) {
            headerController.configurarCallbacksCatalogo(
                    this::alternarYEnfocarBuscador,
                    this::aplicarCategoriaDesdeCabecera,
                    this::aplicarSexoDesdeCabecera
            );
        }

        if (campoBusqueda != null) {
            campoBusqueda.textProperty().addListener((obs, oldV, newV) -> renderizarCatalogo());
        }

        // Aplicar navegación pendiente si el cliente venía desde otra pantalla
        String catPendiente = ClienteHeaderController.consumirCategoriaPendiente();
        if (catPendiente != null) {
            categoriaActiva = catPendiente;
        }
        String sexoPend = ClienteHeaderController.consumirSexoPendiente();
        if (sexoPend != null) {
            sexoActivo = sexoPend;
        }
        if (ClienteHeaderController.consumirActivarBuscadorPendiente()) {
            mostrarBuscador(true);
        }

        actualizarBotonesCategoria();
        actualizarBotonesSexo();
        renderizarCatalogo();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    @FXML
    private void seleccionarCategoria(ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Object dato = ((Button) event.getSource()).getUserData();
            categoriaActiva = (dato != null) ? dato.toString() : "TODAS";
        }
        actualizarBotonesCategoria();
        renderizarCatalogo();
    }

    @FXML
    private void seleccionarSexo(ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Object dato = ((Button) event.getSource()).getUserData();
            sexoActivo = (dato != null) ? dato.toString() : "TODOS";
        }
        actualizarBotonesSexo();
        actualizarTituloFiltro();
        renderizarCatalogo();
    }

    @FXML
    private void limpiarBusqueda(ActionEvent event) {
        if (campoBusqueda != null) {
            campoBusqueda.clear();
        }
        mostrarBuscador(false);
        renderizarCatalogo();
    }

    @FXML
    private void mostrarTodosLosProductos(ActionEvent event) {
        categoriaActiva = "TODAS";
        sexoActivo = "TODOS";
        if (campoBusqueda != null) {
            campoBusqueda.clear();
        }
        actualizarBotonesCategoria();
        actualizarBotonesSexo();
        renderizarCatalogo();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private void aplicarCategoriaDesdeCabecera(String categoria) {
        categoriaActiva = (categoria != null && !categoria.trim().isEmpty()) ? categoria : "TODAS";
        actualizarBotonesCategoria();
        renderizarCatalogo();
    }

    private void aplicarSexoDesdeCabecera(String sexo) {
        sexoActivo = (sexo != null && !sexo.trim().isEmpty()) ? sexo : "TODOS";
        actualizarBotonesSexo();
        actualizarTituloFiltro();
        renderizarCatalogo();
    }

    private void alternarYEnfocarBuscador() {
        boolean visible = barraBusqueda != null && barraBusqueda.isVisible();
        mostrarBuscador(!visible);
    }

    private void mostrarBuscador(boolean visible) {
        if (barraBusqueda == null) return;
        barraBusqueda.setVisible(visible);
        barraBusqueda.setManaged(visible);
        if (visible && campoBusqueda != null) {
            Platform.runLater(() -> campoBusqueda.requestFocus());
        }
    }

    private void actualizarBotonesCategoria() {
        List<Button> botones = Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios);
        for (Button btn : botones) {
            if (btn == null) continue;
            String valor = btn.getUserData() != null ? btn.getUserData().toString() : "";
            boolean activo = valor.equalsIgnoreCase(categoriaActiva);
            btn.getStyleClass().setAll("button",
                    activo ? "item-categoria-catalogo-activo" : "item-categoria-catalogo");
        }
        actualizarTituloFiltro();
    }

    private void actualizarBotonesSexo() {
        List<Button> botones = Arrays.asList(btnSexoTodos, btnSexoHombre, btnSexoMujer, btnSexoUnisex);
        for (Button btn : botones) {
            if (btn == null) continue;
            String valor = btn.getUserData() != null ? btn.getUserData().toString() : "";
            boolean activo = valor.equalsIgnoreCase(sexoActivo);
            btn.getStyleClass().setAll("button",
                    activo ? "boton-filtro-opcion-activo" : "boton-filtro-opcion");
        }
    }

    private void actualizarTituloFiltro() {
        if (lblCategoriaActual == null) return;
        boolean todasCat = "TODAS".equalsIgnoreCase(categoriaActiva);
        boolean todosSexo = "TODOS".equalsIgnoreCase(sexoActivo);
        if (todasCat && todosSexo) {
            lblCategoriaActual.setText("Todos los productos");
        } else if (!todasCat && todosSexo) {
            lblCategoriaActual.setText("Categoría: " + capitalizar(categoriaActiva));
        } else if (todasCat) {
            lblCategoriaActual.setText("Sección: " + capitalizar(sexoActivo));
        } else {
            lblCategoriaActual.setText(capitalizar(categoriaActiva) + " · " + capitalizar(sexoActivo));
        }
    }

    private void renderizarCatalogo() {
        String termino = campoBusqueda != null ? campoBusqueda.getText() : "";
        // Al filtrar por Hombre o Mujer en el catálogo de cliente se incluyen también productos UNISEX;
        // al filtrar explícitamente por UNISEX se muestran solo los UNISEX.
        boolean incluirUnisex = Producto.SEXO_HOMBRE.equalsIgnoreCase(sexoActivo)
                || Producto.SEXO_MUJER.equalsIgnoreCase(sexoActivo);
        List<Producto> productos = gestorProductos.filtrar(
                termino, categoriaActiva, "TODOS", sexoActivo, incluirUnisex);

        contenedorProductos.getChildren().clear();
        boolean vacio = productos.isEmpty();

        scrollCatalogo.setVisible(!vacio);
        scrollCatalogo.setManaged(!vacio);
        estadoVacio.setVisible(vacio);
        estadoVacio.setManaged(vacio);

        if (vacio) {
            return;
        }

        String cedula = obtenerCedulaCliente();
        for (Producto producto : productos) {
            boolean deseado = gestorDeseados.esDeseado(cedula, producto.getId());
            contenedorProductos.getChildren().add(crearTarjetaProducto(producto, deseado));
        }
    }

    /**
     * Construye visualmente una tarjeta de producto fiel al diseño vertical del catálogo de Figma.
     */
    private VBox crearTarjetaProducto(Producto producto, boolean esDeseado) {
        VBox tarjeta = new VBox(8);
        tarjeta.getStyleClass().add("tarjeta-producto-catalogo");

        // 1. Contenedor de imagen con proporción limpia y badge si está agotado
        StackPane marcoImagen = new StackPane();
        marcoImagen.getStyleClass().add("marco-imagen-producto");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(210);
        imageView.setFitHeight(225);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        cargarImagenConFallback(imageView, producto.getImagen(), 210, 225);
        marcoImagen.getChildren().add(imageView);

        boolean agotado = producto.getStock() <= 0;
        if (agotado) {
            Label badgeAgotado = new Label("AGOTADO");
            badgeAgotado.getStyleClass().add("badge-estado-agotado");
            StackPane.setAlignment(badgeAgotado, Pos.TOP_LEFT);
            marcoImagen.getChildren().add(badgeAgotado);
        }

        // 2. Fila de información: Nombre + Precio a la izquierda y Corazón a la derecha
        VBox info = new VBox(2);
        Label lblNombre = new Label(producto.getNombre());
        lblNombre.getStyleClass().add("nombre-producto-tarjeta");
        lblNombre.setWrapText(true);

        Label lblPrecio = new Label(producto.getPrecioFormateado());
        lblPrecio.getStyleClass().add("precio-producto-tarjeta");

        Label lblStock = new Label(capitalizar(producto.getSexo()) + " · "
                + (agotado ? "Sin unidades" : "Disponibles: " + producto.getStock()));
        lblStock.getStyleClass().add("stock-producto-tarjeta");
        info.getChildren().addAll(lblNombre, lblPrecio, lblStock);

        Button btnCorazon = new Button(esDeseado ? "♥" : "♡");
        btnCorazon.getStyleClass().setAll("button", esDeseado ? "boton-corazon-activo" : "boton-corazon");
        btnCorazon.setTooltip(new Tooltip(esDeseado ? "Quitar de deseados" : "Agregar a deseados"));
        btnCorazon.setOnAction(e -> alternarDeseado(producto, btnCorazon));

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox filaInfo = new HBox(8, info, espaciador, btnCorazon);
        filaInfo.setAlignment(Pos.TOP_LEFT);

        // 3. Botón principal oscuro "Añadir al carrito"
        Button btnAgregar = new Button(agotado ? "Agotado" : "Añadir al carrito");
        btnAgregar.setMaxWidth(Double.MAX_VALUE);
        btnAgregar.getStyleClass().add("boton-anadir-carrito");
        btnAgregar.setDisable(agotado);
        btnAgregar.setOnAction(e -> agregarAlCarrito(producto));

        tarjeta.getChildren().addAll(marcoImagen, filaInfo, btnAgregar);
        return tarjeta;
    }

    private void alternarDeseado(Producto producto, Button btnCorazon) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;

        boolean agregado = gestorDeseados.alternarDeseado(cedula, producto.getId());
        btnCorazon.setText(agregado ? "♥" : "♡");
        btnCorazon.getStyleClass().setAll("button", agregado ? "boton-corazon-activo" : "boton-corazon");
        btnCorazon.setTooltip(new Tooltip(agregado ? "Quitar de deseados" : "Agregar a deseados"));

        mostrarMensajeTemporal(
                agregado ? producto.getNombre() + " añadido a tu lista de deseados."
                         : producto.getNombre() + " retirado de tu lista de deseados.",
                false
        );
        if (headerController != null) {
            headerController.actualizarContadores();
        }
    }

    private void agregarAlCarrito(Producto producto) {
        String cedula = obtenerCedulaCliente();
        if (cedula == null) return;

        // Reconsultar el producto vigente en disco para evitar superar el stock actual
        Producto vigente = gestorProductos.buscarPorId(producto.getId());
        if (vigente == null || vigente.getStock() <= 0) {
            mostrarMensajeTemporal("Este producto se encuentra agotado.", true);
            renderizarCatalogo();
            return;
        }

        boolean ok = gestorCarrito.agregarProducto(cedula, vigente.getId(), 1, vigente.getStock());
        if (ok) {
            mostrarMensajeTemporal(vigente.getNombre() + " añadido al carrito.", false);
            if (headerController != null) {
                headerController.actualizarContadores();
            }
        } else {
            mostrarMensajeTemporal("Ya tienes el máximo disponible (" + vigente.getStock()
                    + ") de " + vigente.getNombre() + " en tu carrito.", true);
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

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        String lower = texto.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
