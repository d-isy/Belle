package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Controlador principal del Dashboard de Administrador de Belle Store (Fase 4).
 * Gestiona el catálogo de Productos, las Órdenes de Compra, búsqueda en tiempo real,
 * acordeón interactivo de filtros y operaciones CRUD completas mediante modales didácticos.
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

    public enum ModoVista {
        PRODUCTOS,
        ORDENES
    }

    private ModoVista modoActual = ModoVista.PRODUCTOS;
    private String categoriaProdFiltro = "TODAS";
    private String estadoProdFiltro = "TODOS";
    private String estadoOrdFiltro = "TODOS";

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

        // 2. Configuración de columnas de la tabla de Productos
        configurarTablaProductos();

        // 3. Configuración de columnas de la tabla de Órdenes
        configurarTablaOrdenes();

        // 4. Escuchador reactivo de búsqueda en tiempo real
        campoBusqueda.textProperty().addListener((obs, oldV, newV) -> aplicarFiltrosActuales());

        // 5. Cargar datos iniciales y mostrar vista de Productos por defecto
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
        lblTituloVista.setText("Administrador");

        // Alternar visibilidad de componentes
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

        // Estilos del menú lateral
        btnMenuProductos.getStyleClass().removeAll("item-menu-admin-activo");
        btnMenuOrdenes.getStyleClass().removeAll("item-menu-admin-activo");
        btnMenuProductos.getStyleClass().add("item-menu-admin-activo");

        campoBusqueda.setPromptText("Buscar por ID, Nombre, Categoría...");
        aplicarFiltrosActuales();
    }

    /**
     * Cambia la vista activa al modo Órdenes de Compra.
     */
    @FXML
    public void mostrarVistaOrdenes(ActionEvent event) {
        modoActual = ModoVista.ORDENES;
        lblTituloVista.setText("Administrador");

        // Alternar visibilidad de componentes
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

        // Estilos del menú lateral
        btnMenuProductos.getStyleClass().removeAll("item-menu-admin-activo");
        btnMenuOrdenes.getStyleClass().removeAll("item-menu-admin-activo");
        btnMenuOrdenes.getStyleClass().add("item-menu-admin-activo");

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
     * Abre el modal didáctico para registrar un nuevo producto en Productos.json.
     */
    @FXML
    public void abrirModalAnadirProducto(ActionEvent event) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Añadir Producto - Belle Store");
        dialogo.setHeaderText("Ingresa los datos del nuevo producto textil o accesorio:");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtId = new TextField(String.valueOf(gestorProductos.listar().size() + 235));
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej. CHAQUETA DE CUERO");

        ComboBox<String> cbCategoria = new ComboBox<>(FXCollections.observableArrayList(
                "CAMISAS", "PANTALONES", "ACCESORIOS", "CHAQUETAS", "CALZADO"
        ));
        cbCategoria.setValue("CAMISAS");

        TextField txtStock = new TextField("5");
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList("DISPONIBLE", "AGOTADO"));
        cbEstado.setValue("DISPONIBLE");

        TextField txtPrecio = new TextField("180000");
        txtPrecio.setPromptText("Valor en pesos sin puntos (ej. 180000)");

        TextField txtImagen = new TextField("img/camisa_azul.png");
        txtImagen.setPromptText("Ruta local (img/...) o URL remota (https://...)");

        grid.add(new Label("ID del Producto:"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Nombre Comercial:"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Categoría:"), 0, 2);
        grid.add(cbCategoria, 1, 2);
        grid.add(new Label("Stock Inicial:"), 0, 3);
        grid.add(txtStock, 1, 3);
        grid.add(new Label("Estado:"), 0, 4);
        grid.add(cbEstado, 1, 4);
        grid.add(new Label("Precio Unitario (COP):"), 0, 5);
        grid.add(txtPrecio, 1, 5);
        grid.add(new Label("Ruta o URL Imagen:"), 0, 6);
        grid.add(txtImagen, 1, 6);

        dialogo.getDialogPane().setContent(grid);

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnGuardar) {
            String id = txtId.getText().trim();
            String nombre = txtNombre.getText().trim();
            String categoria = cbCategoria.getValue();
            String estado = cbEstado.getValue();
            String imagen = txtImagen.getText().trim();

            if (id.isEmpty() || nombre.isEmpty()) {
                mostrarAlerta("Campos requeridos", "El ID y el Nombre son obligatorios.", Alert.AlertType.WARNING);
                return;
            }

            int stock;
            double precio;
            try {
                stock = Integer.parseInt(txtStock.getText().trim());
                precio = Double.parseDouble(txtPrecio.getText().trim());
            } catch (NumberFormatException e) {
                mostrarAlerta("Formato numérico", "Stock debe ser entero y Precio numérico.", Alert.AlertType.ERROR);
                return;
            }

            if (imagen.isEmpty()) {
                imagen = "img/placeholder_producto.png";
            }

            Producto nuevo = new Producto(id, imagen, nombre, categoria, stock, estado, precio);
            boolean guardado = gestorProductos.agregar(nuevo);

            if (guardado) {
                aplicarFiltrosActuales();
                mostrarAlerta("Producto Añadido", "El producto fue registrado exitosamente en Productos.json.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "Ya existe un producto con el ID especificado.", Alert.AlertType.ERROR);
            }
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Validaciones, modales, cell factories)
    // =========================================================================
    /**
     * Configura las fábricas de valores y celdas de la tabla de Productos.
     */
    private void configurarTablaProductos() {
        colProdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProdNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colProdCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Celda personalizada: Miniatura ImageView para local/remota con fallback
        colProdImagen.setCellValueFactory(new PropertyValueFactory<>("imagen"));
        colProdImagen.setCellFactory(col -> crearCellFactoryImagenProducto());

        // Celda personalizada: Badge visual para el Estado del producto
        colProdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colProdEstado.setCellFactory(col -> crearCellFactoryEstadoProducto());

        // Celda personalizada: Formato monetario oficial de Figma
        colProdPrecio.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPrecioFormateado()));

        // Celda de acciones con iconos de Editar (Lápiz) y Eliminar (Caneca/Remove)
        colProdAcciones.setCellFactory(col -> crearCellFactoryAccionesProductos());
    }

    /**
     * Configura las fábricas de valores y celdas de la tabla de Órdenes.
     */
    private void configurarTablaOrdenes() {
        colOrdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrdCliente.setCellValueFactory(new PropertyValueFactory<>("clienteId"));
        colOrdTotalArticulos.setCellValueFactory(new PropertyValueFactory<>("totalArticulos"));
        colOrdDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        // Celda personalizada: Lista multilínea con viñetas ("• Camisa - 1")
        colOrdArticulos.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getArticulosFormateados()));

        // Celda personalizada: Badge visual para el Estado de la orden
        colOrdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colOrdEstado.setCellFactory(col -> crearCellFactoryEstadoOrden());

        // Celda personalizada: Formato monetario
        colOrdValorTotal.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValorTotalFormateado()));

        // Celda de acciones con iconos de Editar y Eliminar
        colOrdAcciones.setCellFactory(col -> crearCellFactoryAccionesOrdenes());
    }

    /**
     * Aplica los filtros reactivos según la vista activa (Productos u Órdenes).
     */
    private void aplicarFiltrosActuales() {
        String textoBusqueda = campoBusqueda.getText() != null ? campoBusqueda.getText().trim() : "";

        if (modoActual == ModoVista.PRODUCTOS) {
            List<Producto> productosFiltrados = gestorProductos.filtrar(
                    textoBusqueda,
                    categoriaProdFiltro,
                    estadoProdFiltro
            );
            tablaProductos.setItems(FXCollections.observableArrayList(productosFiltrados));
            lblContadorRegistros.setText("Mostrando " + productosFiltrados.size() + " producto(s)");
        } else {
            List<Orden> ordenesFiltradas = gestorOrdenes.filtrar(
                    textoBusqueda,
                    estadoOrdFiltro
            );
            tablaOrdenes.setItems(FXCollections.observableArrayList(ordenesFiltradas));
            lblContadorRegistros.setText("Mostrando " + ordenesFiltradas.size() + " orden(es)");
        }
    }

    /**
     * Fábrica de celda didáctica para renderizar miniaturas ImageView en la columna IMAGEN.
     * Soporta URLs externas (http/https), recursos locales de proyecto y fallback seguro.
     */
    private TableCell<Producto, String> crearCellFactoryImagenProducto() {
        return new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(45);
                imageView.setFitHeight(45);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
            }

            @Override
            protected void updateItem(String rutaOUrl, boolean empty) {
                super.updateItem(rutaOUrl, empty);
                if (empty || rutaOUrl == null || rutaOUrl.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                try {
                    Image imagen;
                    if (rutaOUrl.startsWith("http://") || rutaOUrl.startsWith("https://")) {
                        imagen = new Image(rutaOUrl, 45, 45, true, true, true);
                    } else {
                        String rutaNormalizada = rutaOUrl.startsWith("/") ? rutaOUrl : "/com/mycompany/belle_store/" + rutaOUrl;
                        InputStream is = getClass().getResourceAsStream(rutaNormalizada);
                        if (is != null) {
                            imagen = new Image(is, 45, 45, true, true);
                        } else {
                            imagen = obtenerImagenPlaceholder();
                        }
                    }
                    imageView.setImage(imagen);
                    setGraphic(imageView);
                    setText(null);
                } catch (Exception e) {
                    imageView.setImage(obtenerImagenPlaceholder());
                    setGraphic(imageView);
                    setText(null);
                }
            }
        };
    }

    /**
     * Retorna la imagen placeholder por defecto de los recursos del proyecto.
     */
    private Image obtenerImagenPlaceholder() {
        try {
            InputStream is = getClass().getResourceAsStream("/com/mycompany/belle_store/img/placeholder_producto.png");
            if (is != null) {
                return new Image(is, 45, 45, true, true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Celda con Badge visual para el estado de un Producto.
     */
    private TableCell<Producto, String> crearCellFactoryEstadoProducto() {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                badge.setText(estado.toUpperCase());
                badge.getStyleClass().clear();

                if ("DISPONIBLE".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-disponible");
                } else {
                    badge.getStyleClass().add("badge-estado-agotado");
                }

                setGraphic(badge);
                setText(null);
            }
        };
    }

    /**
     * Celda con Badge visual para el estado logístico de una Orden.
     */
    private TableCell<Orden, String> crearCellFactoryEstadoOrden() {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                badge.setText(estado);
                badge.getStyleClass().clear();

                if ("Entregado".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-entregado");
                } else if ("No Entregado".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-no-entregado");
                } else {
                    badge.getStyleClass().add("badge-estado-pendiente");
                }

                setGraphic(badge);
                setText(null);
            }
        };
    }

    /**
     * Fábrica de celda de acciones (Editar y Eliminar) para la tabla de Productos.
     */
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
                btnEditar.setTooltip(new Tooltip("Editar Producto"));
                btnEditar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    abrirModalEditarProducto(p);
                });

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar Producto"));
                btnEliminar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    abrirModalEliminarProducto(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(contenedor);
                }
            }
        };
    }

    /**
     * Fábrica de celda de acciones (Editar y Eliminar) para la tabla de Órdenes.
     */
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
                btnEditar.setTooltip(new Tooltip("Actualizar Orden"));
                btnEditar.setOnAction(e -> {
                    Orden o = getTableView().getItems().get(getIndex());
                    abrirModalEditarOrden(o);
                });

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar Orden"));
                btnEliminar.setOnAction(e -> {
                    Orden o = getTableView().getItems().get(getIndex());
                    abrirModalEliminarOrden(o);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(contenedor);
                }
            }
        };
    }

    /**
     * Modal didáctico para Editar un Producto existente.
     */
    private void abrirModalEditarProducto(Producto producto) {
        if (producto == null) return;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar Producto - " + producto.getId());
        dialogo.setHeaderText("Modifica los valores del producto:");

        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtId = new TextField(producto.getId());
        txtId.setDisable(true); // El ID es clave única inmutable

        TextField txtNombre = new TextField(producto.getNombre());
        ComboBox<String> cbCategoria = new ComboBox<>(FXCollections.observableArrayList(
                "CAMISAS", "PANTALONES", "ACCESORIOS", "CHAQUETAS", "CALZADO"
        ));
        cbCategoria.setValue(producto.getCategoria());

        TextField txtStock = new TextField(String.valueOf(producto.getStock()));
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList("DISPONIBLE", "AGOTADO"));
        cbEstado.setValue(producto.getEstado());

        TextField txtPrecio = new TextField(String.valueOf(producto.getPrecio()));
        TextField txtImagen = new TextField(producto.getImagen());

        grid.add(new Label("ID (No editable):"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Categoría:"), 0, 2);
        grid.add(cbCategoria, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(txtStock, 1, 3);
        grid.add(new Label("Estado:"), 0, 4);
        grid.add(cbEstado, 1, 4);
        grid.add(new Label("Precio:"), 0, 5);
        grid.add(txtPrecio, 1, 5);
        grid.add(new Label("Ruta/URL Imagen:"), 0, 6);
        grid.add(txtImagen, 1, 6);

        dialogo.getDialogPane().setContent(grid);

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnActualizar) {
            try {
                producto.setNombre(txtNombre.getText().trim());
                producto.setCategoria(cbCategoria.getValue());
                producto.setStock(Integer.parseInt(txtStock.getText().trim()));
                producto.setEstado(cbEstado.getValue());
                producto.setPrecio(Double.parseDouble(txtPrecio.getText().trim()));
                producto.setImagen(txtImagen.getText().trim());

                boolean actualizado = gestorProductos.actualizar(producto);
                if (actualizado) {
                    aplicarFiltrosActuales();
                    mostrarAlerta("Éxito", "Producto actualizado correctamente en Productos.json.", Alert.AlertType.INFORMATION);
                } else {
                    mostrarAlerta("Error", "No se pudo actualizar el producto.", Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                mostrarAlerta("Error de Formato", "Verifica los datos numéricos ingresados.", Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Modal didáctico para Eliminar un Producto de Productos.json.
     */
    private void abrirModalEliminarProducto(Producto producto) {
        if (producto == null) return;

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Eliminación");
        alerta.setHeaderText("¿Deseas eliminar este producto?");
        alerta.setContentText("Se eliminará definitivamente:\n" + producto.getNombre() + " (ID: " + producto.getId() + ")");

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            boolean eliminado = gestorProductos.eliminar(producto.getId());
            if (eliminado) {
                aplicarFiltrosActuales();
                mostrarAlerta("Producto Eliminado", "El producto ha sido eliminado de la persistencia.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo eliminar el producto especificado.", Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Modal didáctico para Editar una Orden existente.
     */
    private void abrirModalEditarOrden(Orden orden) {
        if (orden == null) return;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Actualizar Orden #" + orden.getId());
        dialogo.setHeaderText("Modifica el estado o la dirección de entrega de la orden:");

        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtId = new TextField(orden.getId());
        txtId.setDisable(true);

        TextField txtCliente = new TextField(orden.getClienteId());
        txtCliente.setDisable(true);

        TextField txtDireccion = new TextField(orden.getDireccion());
        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList(
                "No Entregado", "Entregado", "Pendiente"
        ));
        cbEstado.setValue(orden.getEstado());

        grid.add(new Label("Orden ID:"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Cliente:"), 0, 1);
        grid.add(txtCliente, 1, 1);
        grid.add(new Label("Dirección:"), 0, 2);
        grid.add(txtDireccion, 1, 2);
        grid.add(new Label("Estado Logístico:"), 0, 3);
        grid.add(cbEstado, 1, 3);

        dialogo.getDialogPane().setContent(grid);

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnActualizar) {
            orden.setDireccion(txtDireccion.getText().trim());
            orden.setEstado(cbEstado.getValue());

            boolean actualizado = gestorOrdenes.actualizar(orden);
            if (actualizado) {
                aplicarFiltrosActuales();
                mostrarAlerta("Éxito", "Orden actualizada correctamente en Ordenes.json.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la orden.", Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Modal didáctico para Eliminar una Orden de Ordenes.json.
     */
    private void abrirModalEliminarOrden(Orden orden) {
        if (orden == null) return;

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Eliminación de Orden");
        alerta.setHeaderText("¿Deseas eliminar esta orden?");
        alerta.setContentText("Se eliminará definitivamente la orden #" + orden.getId() + " del cliente " + orden.getClienteId());

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            boolean eliminado = gestorOrdenes.eliminar(orden.getId());
            if (eliminado) {
                aplicarFiltrosActuales();
                mostrarAlerta("Orden Eliminada", "La orden fue eliminada de la persistencia.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo eliminar la orden.", Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Crea un nodo ImageView con el icono especificado ubicado en los recursos.
     */
    private ImageView crearIcono(String nombreArchivo) {
        try {
            InputStream is = getClass().getResourceAsStream("/com/mycompany/belle_store/img/" + nombreArchivo);
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {
        }
        return new ImageView();
    }

    /**
     * Actualiza la clase visual activa para el grupo de botones de filtro.
     */
    private void actualizarBotonFiltroActivo(Button seleccionado, List<Button> grupo) {
        for (Button btn : grupo) {
            btn.getStyleClass().remove("boton-filtro-opcion-activo");
        }
        if (!seleccionado.getStyleClass().contains("boton-filtro-opcion-activo")) {
            seleccionado.getStyleClass().add("boton-filtro-opcion-activo");
        }
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
