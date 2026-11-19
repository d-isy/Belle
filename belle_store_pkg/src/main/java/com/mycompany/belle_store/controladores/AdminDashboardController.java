package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorImagenes;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.validacion.Validaciones;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador principal del Dashboard de Administrador de Belle Store.
 * Gestiona el catálogo de Productos, las Órdenes de Compra, búsqueda en tiempo real,
 * filtros laterales y operaciones CRUD mediante modales didácticos.
 * Estructurado en 5 secciones estándar pedagógicas.
 */
public class AdminDashboardController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private Label lblTituloVista;

    @FXML
    private TextField campoBusqueda;

    @FXML
    private Button btnAnadirProducto;

    @FXML
    private Label lblContadorRegistros;

    @FXML
    private StackPane contenedorTablas;

    // --- Tabla de Productos ---
    @FXML
    private TableView<Producto> tablaProductos;

    @FXML
    private TableColumn<Producto, String> colProdId;

    @FXML
    private TableColumn<Producto, String> colProdImagen;

    @FXML
    private TableColumn<Producto, String> colProdNombre;

    @FXML
    private TableColumn<Producto, String> colProdCategoria;

    @FXML
    private TableColumn<Producto, Integer> colProdStock;

    @FXML
    private TableColumn<Producto, String> colProdEstado;

    @FXML
    private TableColumn<Producto, String> colProdPrecio;

    @FXML
    private TableColumn<Producto, Void> colProdAcciones;

    // --- Tabla de Órdenes ---
    @FXML
    private TableView<Orden> tablaOrdenes;

    @FXML
    private TableColumn<Orden, String> colOrdId;

    @FXML
    private TableColumn<Orden, String> colOrdCliente;

    @FXML
    private TableColumn<Orden, String> colOrdArticulos;

    @FXML
    private TableColumn<Orden, Integer> colOrdTotalArticulos;

    @FXML
    private TableColumn<Orden, String> colOrdDireccion;

    @FXML
    private TableColumn<Orden, String> colOrdEstado;

    @FXML
    private TableColumn<Orden, String> colOrdValorTotal;

    @FXML
    private TableColumn<Orden, Void> colOrdAcciones;

    // --- Barra Lateral (Sidebar) ---
    @FXML
    private Button btnMenuProductos;

    @FXML
    private Button btnMenuOrdenes;

    @FXML
    private VBox panelFiltrosProductos;

    @FXML
    private VBox panelFiltrosOrdenes;

    // Filtros de Productos (Categoría)
    @FXML
    private Button btnCatTodas;

    @FXML
    private Button btnCatCamisas;

    @FXML
    private Button btnCatPantalones;

    @FXML
    private Button btnCatAccesorios;

    // Filtros de Productos (Estado)
    @FXML
    private Button btnEstProdTodos;

    @FXML
    private Button btnEstProdDisponible;

    @FXML
    private Button btnEstProdAgotado;

    // Filtros de Órdenes (Estado)
    @FXML
    private Button btnEstOrdTodos;

    @FXML
    private Button btnEstOrdNoEntregado;

    @FXML
    private Button btnEstOrdEntregado;

    @FXML
    private Button btnEstOrdPendiente;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorProductos gestorProductos = new GestorProductos();
    private final GestorOrdenes gestorOrdenes = new GestorOrdenes();
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();

    public enum ModoVista {
        PRODUCTOS,
        ORDENES
    }

    // Categorías oficiales del diseño de Figma (las mismas del filtro lateral)
    private static final List<String> CATEGORIAS = Arrays.asList("CAMISAS", "PANTALONES", "ACCESORIOS");
    private static final List<String> ESTADOS_ORDEN = Arrays.asList("No Entregado", "Entregado", "Pendiente");

    private ModoVista modoActual = ModoVista.PRODUCTOS;
    private String categoriaProdFiltro = "TODAS";
    private String estadoProdFiltro = "TODOS";
    private String estadoOrdFiltro = "TODOS";

    // Caché cédula -> nombre del cliente (se refresca en cada filtrado para no leer el JSON por cada celda)
    private Map<String, String> nombresClientes = new HashMap<>();

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        // 1. Control de acceso: Verificar que el usuario sea Administrador
        if (!SesionActual.haySesionIniciada() || !SesionActual.esAdmin()) {
            redirigirALogin();
            return;
        }

        // 2. Configuración de columnas de las tablas
        configurarTablaProductos();
        configurarTablaOrdenes();

        // 3. Escuchador reactivo de búsqueda en tiempo real
        campoBusqueda.textProperty().addListener((obs, oldV, newV) -> aplicarFiltrosActuales());

        // 4. Cargar datos iniciales y mostrar vista de Productos por defecto
        mostrarVistaProductos(null);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
    /**
     * Cambia la vista activa al modo Catálogo de Productos.
     */
    @FXML
    public void mostrarVistaProductos(ActionEvent event) {
        modoActual = ModoVista.PRODUCTOS;

        tablaProductos.setVisible(true);
        tablaProductos.setManaged(true);
        tablaOrdenes.setVisible(false);
        tablaOrdenes.setManaged(false);

        btnAnadirProducto.setVisible(true);
        btnAnadirProducto.setManaged(true);

        panelFiltrosProductos.setVisible(true);
        panelFiltrosProductos.setManaged(true);
        panelFiltrosOrdenes.setVisible(false);
        panelFiltrosOrdenes.setManaged(false);

        marcarMenuActivo(btnMenuProductos);
        campoBusqueda.setPromptText("Buscar por ID, Nombre, Categoría...");
        aplicarFiltrosActuales();
    }

    /**
     * Cambia la vista activa al modo Órdenes de Compra.
     */
    @FXML
    public void mostrarVistaOrdenes(ActionEvent event) {
        modoActual = ModoVista.ORDENES;

        tablaProductos.setVisible(false);
        tablaProductos.setManaged(false);
        tablaOrdenes.setVisible(true);
        tablaOrdenes.setManaged(true);

        btnAnadirProducto.setVisible(false);
        btnAnadirProducto.setManaged(false);

        panelFiltrosProductos.setVisible(false);
        panelFiltrosProductos.setManaged(false);
        panelFiltrosOrdenes.setVisible(true);
        panelFiltrosOrdenes.setManaged(true);

        marcarMenuActivo(btnMenuOrdenes);
        campoBusqueda.setPromptText("Buscar por ID, Cliente...");
        aplicarFiltrosActuales();
    }

    // --- Filtros de Categoría de Productos ---
    @FXML
    public void filtrarCategoriaTodas(ActionEvent event) {
        categoriaProdFiltro = "TODAS";
        actualizarBotonFiltroActivo(btnCatTodas, Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarCategoriaCamisas(ActionEvent event) {
        categoriaProdFiltro = "CAMISAS";
        actualizarBotonFiltroActivo(btnCatCamisas, Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarCategoriaPantalones(ActionEvent event) {
        categoriaProdFiltro = "PANTALONES";
        actualizarBotonFiltroActivo(btnCatPantalones, Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarCategoriaAccesorios(ActionEvent event) {
        categoriaProdFiltro = "ACCESORIOS";
        actualizarBotonFiltroActivo(btnCatAccesorios, Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios));
        aplicarFiltrosActuales();
    }

    // --- Filtros de Estado de Productos ---
    @FXML
    public void filtrarEstadoProdTodos(ActionEvent event) {
        estadoProdFiltro = "TODOS";
        actualizarBotonFiltroActivo(btnEstProdTodos, Arrays.asList(btnEstProdTodos, btnEstProdDisponible, btnEstProdAgotado));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarEstadoProdDisponible(ActionEvent event) {
        estadoProdFiltro = "DISPONIBLE";
        actualizarBotonFiltroActivo(btnEstProdDisponible, Arrays.asList(btnEstProdTodos, btnEstProdDisponible, btnEstProdAgotado));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarEstadoProdAgotado(ActionEvent event) {
        estadoProdFiltro = "AGOTADO";
        actualizarBotonFiltroActivo(btnEstProdAgotado, Arrays.asList(btnEstProdTodos, btnEstProdDisponible, btnEstProdAgotado));
        aplicarFiltrosActuales();
    }

    // --- Filtros de Estado de Órdenes ---
    @FXML
    public void filtrarEstadoOrdTodos(ActionEvent event) {
        estadoOrdFiltro = "TODOS";
        actualizarBotonFiltroActivo(btnEstOrdTodos, Arrays.asList(btnEstOrdTodos, btnEstOrdNoEntregado, btnEstOrdEntregado, btnEstOrdPendiente));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarEstadoOrdNoEntregado(ActionEvent event) {
        estadoOrdFiltro = "No Entregado";
        actualizarBotonFiltroActivo(btnEstOrdNoEntregado, Arrays.asList(btnEstOrdTodos, btnEstOrdNoEntregado, btnEstOrdEntregado, btnEstOrdPendiente));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarEstadoOrdEntregado(ActionEvent event) {
        estadoOrdFiltro = "Entregado";
        actualizarBotonFiltroActivo(btnEstOrdEntregado, Arrays.asList(btnEstOrdTodos, btnEstOrdNoEntregado, btnEstOrdEntregado, btnEstOrdPendiente));
        aplicarFiltrosActuales();
    }

    @FXML
    public void filtrarEstadoOrdPendiente(ActionEvent event) {
        estadoOrdFiltro = "Pendiente";
        actualizarBotonFiltroActivo(btnEstOrdPendiente, Arrays.asList(btnEstOrdTodos, btnEstOrdNoEntregado, btnEstOrdEntregado, btnEstOrdPendiente));
        aplicarFiltrosActuales();
    }

    /**
     * Cierra la sesión activa del Administrador y regresa al Login.
     */
    @FXML
    public void manejarCerrarSesion(ActionEvent event) {
        SesionActual.cerrarSesion();
        redirigirALogin();
    }

    /**
     * Abre el modal para registrar un nuevo producto en Productos.json.
     */
    @FXML
    public void abrirModalAnadirProducto(ActionEvent event) {
        Optional<Producto> resultado = mostrarDialogoProducto(null);
        resultado.ifPresent(nuevo -> {
            if (gestorProductos.agregar(nuevo)) {
                aplicarFiltrosActuales();
                mostrarAlerta("Producto añadido", "El producto fue registrado en Productos.json.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo guardar el producto (ID duplicado o error de escritura).", Alert.AlertType.ERROR);
            }
        });
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Validaciones, modales, cell factories)
    // =========================================================================

    // ---------------------------------------------------------------- Tablas
    private void configurarTablaProductos() {
        colProdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProdNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colProdCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Miniatura ImageView para rutas locales / imágenes importadas / URLs, con fallback
        colProdImagen.setCellValueFactory(new PropertyValueFactory<>("imagen"));
        colProdImagen.setCellFactory(col -> crearCellFactoryImagenProducto());

        // Badge visual para el Estado del producto
        colProdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colProdEstado.setCellFactory(col -> crearCellFactoryEstadoProducto());

        // Formato monetario oficial de Figma
        colProdPrecio.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPrecioFormateado()));

        colProdAcciones.setCellFactory(col -> crearCellFactoryAccionesProductos());
    }

    private void configurarTablaOrdenes() {
        colOrdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrdTotalArticulos.setCellValueFactory(new PropertyValueFactory<>("totalArticulos"));
        colOrdDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        // Cliente: cédula (como en Figma) y, debajo, el nombre si el cliente está registrado
        colOrdCliente.setCellValueFactory(cellData -> {
            String cedula = cellData.getValue().getClienteId();
            String nombre = nombresClientes.get(cedula);
            return new SimpleStringProperty(nombre != null ? cedula + "\n" + nombre : cedula);
        });

        // Lista multilínea con viñetas ("• Camisa - 1")
        colOrdArticulos.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getArticulosFormateados()));

        colOrdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colOrdEstado.setCellFactory(col -> crearCellFactoryEstadoOrden());

        colOrdValorTotal.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValorTotalFormateado()));

        colOrdAcciones.setCellFactory(col -> crearCellFactoryAccionesOrdenes());

        // Altura de fila según la cantidad de artículos para que las viñetas no se recorten
        tablaOrdenes.setRowFactory(tv -> new TableRow<Orden>() {
            @Override
            protected void updateItem(Orden orden, boolean empty) {
                super.updateItem(orden, empty);
                if (empty || orden == null) {
                    setStyle("");
                } else {
                    int lineas = Math.max(orden.getArticulos().size(), 2);
                    setStyle("-fx-cell-size: " + (20 + lineas * 20) + "px;");
                }
            }
        });
    }

    /**
     * Aplica los filtros reactivos según la vista activa (Productos u Órdenes).
     */
    private void aplicarFiltrosActuales() {
        String textoBusqueda = campoBusqueda.getText() != null ? campoBusqueda.getText().trim() : "";

        if (modoActual == ModoVista.PRODUCTOS) {
            List<Producto> filtrados = gestorProductos.filtrar(textoBusqueda, categoriaProdFiltro, estadoProdFiltro);
            tablaProductos.setItems(FXCollections.observableArrayList(filtrados));
            lblContadorRegistros.setText("Mostrando " + filtrados.size() + " producto(s)");
        } else {
            refrescarNombresClientes();
            String termino = textoBusqueda.toLowerCase();

            // Filtra por estado en el gestor y por ID / cédula / nombre del cliente aquí
            List<Orden> filtradas = gestorOrdenes.filtrar("", estadoOrdFiltro).stream()
                    .filter(o -> termino.isEmpty()
                            || (o.getId() != null && o.getId().toLowerCase().contains(termino))
                            || (o.getClienteId() != null && o.getClienteId().toLowerCase().contains(termino))
                            || nombreContiene(o.getClienteId(), termino))
                    .collect(Collectors.toList());

            tablaOrdenes.setItems(FXCollections.observableArrayList(filtradas));
            lblContadorRegistros.setText("Mostrando " + filtradas.size() + " orden(es)");
        }
    }

    private void refrescarNombresClientes() {
        Map<String, String> mapa = new HashMap<>();
        for (Usuario u : gestorUsuarios.listar()) {
            if (u.getNumeroIdentificacion() != null) {
                mapa.put(u.getNumeroIdentificacion(), u.getNombreCompleto());
            }
        }
        nombresClientes = mapa;
    }

    private boolean nombreContiene(String cedula, String termino) {
        String nombre = nombresClientes.get(cedula);
        return nombre != null && nombre.toLowerCase().contains(termino);
    }

    // ------------------------------------------------------------ Cell factories
    /**
     * Fábrica de celda para renderizar miniaturas ImageView en la columna IMAGEN.
     * Soporta recursos del proyecto, imágenes importadas por el administrador y URLs externas.
     * Si la imagen no existe o falla la carga, muestra la imagen genérica.
     */
    private TableCell<Producto, String> crearCellFactoryImagenProducto() {
        return new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(45);
                imageView.setFitHeight(45);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String referencia, boolean empty) {
                super.updateItem(referencia, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                asignarMiniatura(imageView, referencia, 45);
                setGraphic(imageView);
            }
        };
    }

    /**
     * Asigna al ImageView la miniatura de la referencia indicada; ante cualquier error
     * (archivo inexistente, URL caída) muestra la imagen genérica.
     */
    private void asignarMiniatura(ImageView destino, String referencia, double tamano) {
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

    private Image imagenGenerica(double tamano) {
        String url = GestorImagenes.resolverUrl(GestorImagenes.PLACEHOLDER);
        return url != null ? new Image(url, tamano * 2, tamano * 2, true, true) : null;
    }

    private TableCell<Producto, String> crearCellFactoryEstadoProducto() {
        return new TableCell<>() {
            private final Label badge = new Label();

            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(estado.toUpperCase());
                badge.getStyleClass().setAll("DISPONIBLE".equalsIgnoreCase(estado)
                        ? "badge-estado-disponible" : "badge-estado-agotado");
                setGraphic(badge);
            }
        };
    }

    private TableCell<Orden, String> crearCellFactoryEstadoOrden() {
        return new TableCell<>() {
            private final Label badge = new Label();

            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(estado);
                String clase;
                if ("Entregado".equalsIgnoreCase(estado)) {
                    clase = "badge-estado-entregado";
                } else if ("No Entregado".equalsIgnoreCase(estado)) {
                    clase = "badge-estado-no-entregado";
                } else {
                    clase = "badge-estado-pendiente";
                }
                badge.getStyleClass().setAll(clase);
                setGraphic(badge);
            }
        };
    }

    private TableCell<Producto, Void> crearCellFactoryAccionesProductos() {
        return new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox contenedor = new HBox(8, btnEditar, btnEliminar);

            {
                contenedor.setAlignment(Pos.CENTER);
                btnEditar.getStyleClass().add("boton-accion-tabla");
                btnEliminar.getStyleClass().add("boton-accion-tabla");

                btnEditar.setGraphic(crearIcono("pencil.png"));
                btnEditar.setTooltip(new Tooltip("Editar producto"));
                btnEditar.setOnAction(e -> abrirModalEditarProducto(getTableView().getItems().get(getIndex())));

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar producto"));
                btnEliminar.setOnAction(e -> abrirModalEliminarProducto(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    private TableCell<Orden, Void> crearCellFactoryAccionesOrdenes() {
        return new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox contenedor = new HBox(8, btnEditar, btnEliminar);

            {
                contenedor.setAlignment(Pos.CENTER);
                btnEditar.getStyleClass().add("boton-accion-tabla");
                btnEliminar.getStyleClass().add("boton-accion-tabla");

                btnEditar.setGraphic(crearIcono("pencil.png"));
                btnEditar.setTooltip(new Tooltip("Actualizar orden"));
                btnEditar.setOnAction(e -> abrirModalEditarOrden(getTableView().getItems().get(getIndex())));

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar orden"));
                btnEliminar.setOnAction(e -> abrirModalEliminarOrden(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    // ------------------------------------------------------------ Modales de producto
    private void abrirModalEditarProducto(Producto producto) {
        if (producto == null) return;

        // Se edita una COPIA: el objeto de la tabla solo cambia si el guardado tiene éxito
        Optional<Producto> resultado = mostrarDialogoProducto(producto);
        resultado.ifPresent(editado -> {
            if (gestorProductos.actualizar(editado)) {
                aplicarFiltrosActuales();
                mostrarAlerta("Producto actualizado", "Los cambios se guardaron en Productos.json.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el producto.", Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Diálogo compartido para crear (original == null) o editar un producto.
     * Valida los campos dentro del propio diálogo (resaltado en rojo + mensaje) sin cerrarlo.
     * Incluye el botón "Examinar..." para enlazar una imagen del equipo con el producto.
     */
    private Optional<Producto> mostrarDialogoProducto(Producto original) {
        boolean esNuevo = original == null;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(esNuevo ? "Añadir producto - Belle Store" : "Editar producto - " + original.getId());
        dialogo.setHeaderText(esNuevo ? "Ingresa los datos del nuevo producto:" : "Modifica los datos del producto:");
        aplicarEstilosDialogo(dialogo);

        ButtonType btnGuardar = new ButtonType(esNuevo ? "Guardar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        TextField txtId = new TextField(esNuevo ? generarSiguienteIdProducto() : original.getId());
        txtId.setDisable(!esNuevo); // El ID es la clave única: no se edita
        TextField txtNombre = new TextField(esNuevo ? "" : original.getNombre());
        txtNombre.setPromptText("Ej. CHAQUETA DE CUERO");

        ComboBox<String> cbCategoria = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        cbCategoria.setValue(esNuevo || !CATEGORIAS.contains(original.getCategoria()) ? CATEGORIAS.get(0) : original.getCategoria());

        TextField txtStock = new TextField(esNuevo ? "1" : String.valueOf(original.getStock()));
        Label lblEstado = new Label();
        lblEstado.setStyle("-fx-font-weight: bold;");
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
        lblError.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
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
            String error = validarFormularioProducto(esNuevo, txtId, txtNombre, txtStock, txtPrecio);
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
                    Validaciones.estadoPorStock(stock),
                    Double.parseDouble(txtPrecio.getText().trim())
            ));
        }
        return Optional.empty();
    }

    /**
     * Valida el formulario de producto y resalta en rojo los campos erróneos.
     * @return mensaje de error o null si todo es válido.
     */
    private String validarFormularioProducto(boolean esNuevo, TextField txtId, TextField txtNombre,
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
     * Elimina un producto solo si ninguna orden lo referencia (integridad referencial).
     */
    private void abrirModalEliminarProducto(Producto producto) {
        if (producto == null) return;

        List<String> ordenesRelacionadas = gestorOrdenes.listar().stream()
                .filter(o -> o.getArticulos().stream().anyMatch(i -> producto.getId().equalsIgnoreCase(i.getProductoId())))
                .map(Orden::getId)
                .collect(Collectors.toList());

        if (!ordenesRelacionadas.isEmpty()) {
            mostrarAlerta("No se puede eliminar",
                    "El producto \"" + producto.getNombre() + "\" aparece en las órdenes: "
                            + String.join(", ", ordenesRelacionadas)
                            + ".\nElimina o modifica esas órdenes primero, o deja el producto sin stock (AGOTADO).",
                    Alert.AlertType.WARNING);
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText("¿Deseas eliminar este producto?");
        alerta.setContentText(producto.getNombre() + " (ID: " + producto.getId() + ")");
        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            if (gestorProductos.eliminar(producto.getId())) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo eliminar el producto.", Alert.AlertType.ERROR);
            }
        }
    }

    // ------------------------------------------------------------ Modales de orden
    private void abrirModalEditarOrden(Orden orden) {
        if (orden == null) return;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Actualizar orden #" + orden.getId());
        dialogo.setHeaderText("Modifica el estado o la dirección de entrega:");
        aplicarEstilosDialogo(dialogo);

        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnActualizar, ButtonType.CANCEL);

        TextField txtId = new TextField(orden.getId());
        txtId.setDisable(true);
        TextField txtCliente = new TextField(orden.getClienteId());
        txtCliente.setDisable(true);
        TextField txtDireccion = new TextField(orden.getDireccion());
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList(ESTADOS_ORDEN));
        cbEstado.setValue(orden.getEstado());

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");

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
            limpiarResaltado(txtDireccion);
            String dir = txtDireccion.getText() == null ? "" : txtDireccion.getText().trim();
            if (dir.length() < 5) {
                resaltar(txtDireccion);
                lblError.setText("La dirección debe tener al menos 5 caracteres.");
                evento.consume();
            }
        });

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnActualizar) {
            // Copia: el objeto mostrado en la tabla no cambia si el guardado falla
            Orden copia = new Orden(orden.getId(), orden.getClienteId(), orden.getArticulos(),
                    txtDireccion.getText().trim(), cbEstado.getValue(), orden.getValorTotal(), orden.getFecha());
            if (gestorOrdenes.actualizar(copia)) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la orden.", Alert.AlertType.ERROR);
            }
        }
    }

    private void abrirModalEliminarOrden(Orden orden) {
        if (orden == null) return;

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación de orden");
        alerta.setHeaderText("¿Deseas eliminar esta orden?");
        alerta.setContentText("Orden #" + orden.getId() + " del cliente " + orden.getClienteId());
        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            if (gestorOrdenes.eliminar(orden.getId())) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo eliminar la orden.", Alert.AlertType.ERROR);
            }
        }
    }

    // ------------------------------------------------------------ Utilidades
    /** Genera el siguiente ID numérico libre: máximo ID numérico existente + 1. */
    private String generarSiguienteIdProducto() {
        long maximo = 0;
        for (Producto p : gestorProductos.listar()) {
            try {
                maximo = Math.max(maximo, Long.parseLong(p.getId().trim()));
            } catch (NumberFormatException ignorada) {
                // IDs no numéricos no participan en la secuencia
            }
        }
        return String.valueOf(maximo + 1);
    }

    private void resaltar(TextField campo) {
        if (!campo.getStyleClass().contains("campo-error")) {
            campo.getStyleClass().add("campo-error");
        }
    }

    private void limpiarResaltado(TextField... campos) {
        for (TextField campo : campos) {
            campo.getStyleClass().remove("campo-error");
        }
    }

    private void aplicarEstilosDialogo(Dialog<?> dialogo) {
        try {
            dialogo.getDialogPane().getStylesheets().add(
                    getClass().getResource("/com/mycompany/belle_store/css/estilos.css").toExternalForm());
        } catch (NullPointerException ignorada) {
            // Sin estilos personalizados: se usa el aspecto por defecto
        }
        Window dueno = tablaProductos.getScene() != null ? tablaProductos.getScene().getWindow() : null;
        if (dueno != null) {
            dialogo.initOwner(dueno);
        }
    }

    private ImageView crearIcono(String nombreArchivo) {
        try (InputStream is = getClass().getResourceAsStream("/com/mycompany/belle_store/img/" + nombreArchivo)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (IOException ignorada) {
            // Se retorna un icono vacío
        }
        return new ImageView();
    }

    private void marcarMenuActivo(Button activo) {
        btnMenuProductos.getStyleClass().remove("item-menu-admin-activo");
        btnMenuOrdenes.getStyleClass().remove("item-menu-admin-activo");
        activo.getStyleClass().add("item-menu-admin-activo");
    }

    private void actualizarBotonFiltroActivo(Button seleccionado, List<Button> grupo) {
        for (Button btn : grupo) {
            btn.getStyleClass().remove("boton-filtro-opcion-activo");
        }
        seleccionado.getStyleClass().add("boton-filtro-opcion-activo");
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void redirigirALogin() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Error al navegar a login: " + e.getMessage());
        }
    }
}
