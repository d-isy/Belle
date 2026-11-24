package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.persistencia.GestorUsuarios;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.util.Formato;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador principal del Dashboard de Administrador de Belle Store.
 * Gestiona el catálogo de Productos, las Órdenes de Compra, búsqueda en tiempo real y filtros laterales.
 * Los formularios modales se delegan en {@link DialogoProducto} y {@link DialogoOrden}.
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
    private TableColumn<Producto, String> colProdSexo;

    @FXML
    private TableColumn<Producto, Integer> colProdStock;

    @FXML
    private TableColumn<Producto, String> colProdEstado;

    @FXML
    private TableColumn<Producto, Double> colProdPrecio;

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
    private TableColumn<Orden, Double> colOrdValorTotal;

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

    // Filtros de Productos (Sexo)
    @FXML
    private Button btnSexoProdTodos;

    @FXML
    private Button btnSexoProdHombre;

    @FXML
    private Button btnSexoProdMujer;

    @FXML
    private Button btnSexoProdUnisex;

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

    private ModoVista modoActual = ModoVista.PRODUCTOS;
    private String categoriaProdFiltro = "TODAS";
    private String sexoProdFiltro = "TODOS";
    private String estadoProdFiltro = "TODOS";
    private String estadoOrdFiltro = "TODOS";

    // Caché cédula -> nombre del cliente (se refresca al filtrar órdenes para no leer JSON por celda)
    private Map<String, String> nombresClientes = new HashMap<>();

    // Mensajes de estado vacío para cada tabla
    private final Label placeholderProductos = new Label();
    private final Label placeholderOrdenes = new Label();

    // Botones de confirmación y avisos en español
    private static final ButtonType BTN_CANCELAR = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    private static final ButtonType BTN_ELIMINAR = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType BTN_ACEPTAR = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        // El control de acceso ya se valida antes de cargar esta vista en App.setRoot()
        if (!SesionActual.esAdmin()) {
            return;
        }

        configurarTablaProductos();
        configurarTablaOrdenes();

        placeholderProductos.getStyleClass().add("placeholder-tabla");
        placeholderOrdenes.getStyleClass().add("placeholder-tabla");
        tablaProductos.setPlaceholder(placeholderProductos);
        tablaOrdenes.setPlaceholder(placeholderOrdenes);

        campoBusqueda.textProperty().addListener((obs, oldV, newV) -> aplicarFiltrosActuales());

        mostrarVistaProductos(null);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
    // =========================================================================
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

    /**
     * Manejador unificado para los botones de filtro por Categoría de Productos.
     * Lee el valor del filtro desde el atributo {@code userData} definido en el FXML.
     */
    @FXML
    public void filtrarCategoriaProducto(ActionEvent event) {
        Button origen = (Button) event.getSource();
        categoriaProdFiltro = String.valueOf(origen.getUserData());
        actualizarBotonFiltroActivo(origen, Arrays.asList(btnCatTodas, btnCatCamisas, btnCatPantalones, btnCatAccesorios));
        aplicarFiltrosActuales();
    }

    /**
     * Manejador unificado para los botones de filtro por Sexo de Productos.
     */
    @FXML
    public void filtrarSexoProducto(ActionEvent event) {
        Button origen = (Button) event.getSource();
        sexoProdFiltro = String.valueOf(origen.getUserData());
        actualizarBotonFiltroActivo(origen, Arrays.asList(
                btnSexoProdTodos, btnSexoProdHombre, btnSexoProdMujer, btnSexoProdUnisex));
        aplicarFiltrosActuales();
    }

    /**
     * Manejador unificado para los botones de filtro por Estado de Productos.
     */
    @FXML
    public void filtrarEstadoProducto(ActionEvent event) {
        Button origen = (Button) event.getSource();
        estadoProdFiltro = String.valueOf(origen.getUserData());
        actualizarBotonFiltroActivo(origen, Arrays.asList(btnEstProdTodos, btnEstProdDisponible, btnEstProdAgotado));
        aplicarFiltrosActuales();
    }

    /**
     * Manejador unificado para los botones de filtro por Estado de Órdenes.
     */
    @FXML
    public void filtrarEstadoOrden(ActionEvent event) {
        Button origen = (Button) event.getSource();
        estadoOrdFiltro = String.valueOf(origen.getUserData());
        actualizarBotonFiltroActivo(origen, Arrays.asList(btnEstOrdTodos, btnEstOrdNoEntregado, btnEstOrdEntregado, btnEstOrdPendiente));
        aplicarFiltrosActuales();
    }

    @FXML
    public void manejarCerrarSesion(ActionEvent event) {
        SesionActual.cerrarSesion();
        redirigirALogin();
    }

    @FXML
    public void abrirModalAnadirProducto(ActionEvent event) {
        Optional<Producto> resultado = DialogoProducto.mostrar(obtenerVentana(), null, gestorProductos);
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
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Tablas, cell factories y CRUD)
    // =========================================================================
    private void configurarTablaProductos() {
        colProdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProdNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colProdCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colProdSexo.setCellValueFactory(new PropertyValueFactory<>("sexo"));
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        colProdImagen.setCellValueFactory(new PropertyValueFactory<>("imagen"));
        colProdImagen.setCellFactory(col -> crearCellFactoryImagenProducto());

        colProdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colProdEstado.setCellFactory(col -> crearCellFactoryEstadoProducto());

        // La columna guarda el número (para ordenar bien) y la celda lo muestra como "$200.000 COP"
        colProdPrecio.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrecio()));
        colProdPrecio.setCellFactory(col -> crearCellFactoryMoneda());

        colProdId.setComparator(Formato::compararNumericoOTexto);
        colProdAcciones.setCellFactory(col -> crearCellFactoryAccionesProductos());

        // Doble clic sobre una fila abre el modal de edición del producto
        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY
                        && evento.getClickCount() == 2
                        && !fila.isEmpty()
                        && !esClicEnBoton(evento.getTarget())) {
                    abrirModalEditarProducto(fila.getItem());
                }
            });
            return fila;
        });
    }

    private void configurarTablaOrdenes() {
        colOrdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrdTotalArticulos.setCellValueFactory(new PropertyValueFactory<>("totalArticulos"));
        colOrdDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        // Cliente: cédula (como en Figma) y, debajo, el nombre si está registrado
        colOrdCliente.setCellValueFactory(cellData -> {
            String cedula = cellData.getValue().getClienteIdentificacion();
            String nombre = nombresClientes.get(cedula);
            return new SimpleStringProperty(nombre != null ? cedula + "\n" + nombre : cedula);
        });

        colOrdArticulos.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getArticulosFormateados()));

        colOrdEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colOrdEstado.setCellFactory(col -> crearCellFactoryEstadoOrden());

        colOrdValorTotal.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorTotal()));
        colOrdValorTotal.setCellFactory(col -> crearCellFactoryMoneda());

        colOrdId.setComparator(Formato::compararNumericoOTexto);
        colOrdAcciones.setCellFactory(col -> crearCellFactoryAccionesOrdenes());

        // Altura de fila según cantidad de artículos + doble clic para editar la orden
        tablaOrdenes.setRowFactory(tv -> {
            TableRow<Orden> fila = new TableRow<>() {
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
            };
            fila.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY
                        && evento.getClickCount() == 2
                        && !fila.isEmpty()
                        && !esClicEnBoton(evento.getTarget())) {
                    abrirModalEditarOrden(fila.getItem());
                }
            });
            return fila;
        });
    }

    private void aplicarFiltrosActuales() {
        String textoBusqueda = campoBusqueda.getText() != null ? campoBusqueda.getText().trim() : "";

        if (modoActual == ModoVista.PRODUCTOS) {
            List<Producto> filtrados = gestorProductos.filtrar(
                    textoBusqueda, categoriaProdFiltro, estadoProdFiltro, sexoProdFiltro);
            tablaProductos.setItems(FXCollections.observableArrayList(filtrados));
            lblContadorRegistros.setText("Mostrando " + filtrados.size() + " producto(s)");

            boolean hayFiltro = !textoBusqueda.isEmpty()
                    || !"TODAS".equals(categoriaProdFiltro)
                    || !"TODOS".equals(sexoProdFiltro)
                    || !"TODOS".equals(estadoProdFiltro);
            placeholderProductos.setText(hayFiltro
                    ? "Ningún producto coincide con la búsqueda o los filtros."
                    : "Aún no hay productos registrados. Usa \"Añadir Producto +\" para crear el primero.");
        } else {
            nombresClientes = gestorUsuarios.mapearNombresPorIdentificacion();
            List<Orden> filtradas = gestorOrdenes.filtrar(textoBusqueda, estadoOrdFiltro, nombresClientes);
            tablaOrdenes.setItems(FXCollections.observableArrayList(filtradas));
            lblContadorRegistros.setText("Mostrando " + filtradas.size() + " orden(es)");

            boolean hayFiltro = !textoBusqueda.isEmpty() || !"TODOS".equals(estadoOrdFiltro);
            placeholderOrdenes.setText(hayFiltro
                    ? "Ninguna orden coincide con la búsqueda o el filtro."
                    : "Aún no hay órdenes registradas.");
        }
    }

    // ------------------------------------------------------------ Cell factories
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
                DialogoProducto.asignarMiniatura(imageView, referencia, 45);
                setGraphic(imageView);
            }
        };
    }

    private <S> TableCell<S, Double> crearCellFactoryMoneda() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : Formato.moneda(valor));
            }
        };
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
                btnEditar.setOnAction(e -> {
                    Producto fila = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fila != null) abrirModalEditarProducto(fila);
                });

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar producto"));
                btnEliminar.setOnAction(e -> {
                    Producto fila = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fila != null) abrirModalEliminarProducto(fila);
                });
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
                btnEditar.setOnAction(e -> {
                    Orden fila = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fila != null) abrirModalEditarOrden(fila);
                });

                btnEliminar.setGraphic(crearIcono("remove.png"));
                btnEliminar.setTooltip(new Tooltip("Eliminar orden"));
                btnEliminar.setOnAction(e -> {
                    Orden fila = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fila != null) abrirModalEliminarOrden(fila);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    // ------------------------------------------------------------ Acciones CRUD
    private void abrirModalEditarProducto(Producto producto) {
        Optional<Producto> resultado = DialogoProducto.mostrar(obtenerVentana(), producto, gestorProductos);
        resultado.ifPresent(editado -> {
            if (gestorProductos.actualizar(editado)) {
                aplicarFiltrosActuales();
                mostrarAlerta("Producto actualizado", "Los cambios se guardaron en Productos.json.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el producto.", Alert.AlertType.ERROR);
            }
        });
    }

    private void abrirModalEliminarProducto(Producto producto) {
        List<String> ordenesRelacionadas = gestorOrdenes.buscarIdsOrdenesConProducto(producto.getId());
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
        alerta.getButtonTypes().setAll(BTN_ELIMINAR, BTN_CANCELAR);
        DialogoProducto.aplicarEstilos(alerta, obtenerVentana());

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == BTN_ELIMINAR) {
            if (gestorProductos.eliminar(producto.getId())) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo eliminar el producto.", Alert.AlertType.ERROR);
            }
        }
    }

    private void abrirModalEditarOrden(Orden orden) {
        String nombreCliente = nombresClientes.get(orden.getClienteIdentificacion());
        Optional<Orden> resultado = DialogoOrden.mostrar(obtenerVentana(), orden, nombreCliente);
        resultado.ifPresent(copia -> {
            if (gestorOrdenes.actualizar(copia)) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar la orden.", Alert.AlertType.ERROR);
            }
        });
    }

    private void abrirModalEliminarOrden(Orden orden) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación de orden");
        alerta.setHeaderText("¿Deseas eliminar esta orden?");
        alerta.setContentText("Orden #" + orden.getId() + " del cliente " + orden.getClienteIdentificacion());
        alerta.getButtonTypes().setAll(BTN_ELIMINAR, BTN_CANCELAR);
        DialogoProducto.aplicarEstilos(alerta, obtenerVentana());

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == BTN_ELIMINAR) {
            if (gestorOrdenes.eliminar(orden.getId())) {
                aplicarFiltrosActuales();
            } else {
                mostrarAlerta("Error", "No se pudo eliminar la orden.", Alert.AlertType.ERROR);
            }
        }
    }

    // ------------------------------------------------------------ Utilidades UI
    /** Evita que un doble clic rápido sobre los botones de acción (Editar/Eliminar) abra el modal dos veces. */
    private boolean esClicEnBoton(Object objetivo) {
        Node actual = (objetivo instanceof Node) ? (Node) objetivo : null;
        while (actual != null && !(actual instanceof TableRow)) {
            if (actual instanceof Button) {
                return true;
            }
            actual = actual.getParent();
        }
        return false;
    }

    private Window obtenerVentana() {
        return tablaProductos != null && tablaProductos.getScene() != null
                ? tablaProductos.getScene().getWindow() : null;
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
        alerta.getButtonTypes().setAll(BTN_ACEPTAR);
        DialogoProducto.aplicarEstilos(alerta, obtenerVentana());
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
