package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorCarrito;
import com.mycompany.belle_store.persistencia.GestorDeseados;
import com.mycompany.belle_store.sesion.SesionActual;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Controlador de la cabecera global compartida entre las 4 vistas de cliente
 * (Catálogo, Carrito de Compras, Lista de Deseados e Historial de Compras).
 *
 * <p>Centraliza la navegación superior, los contadores en vivo del carrito y deseados,
 * el acceso al perfil del usuario y el cierre de sesión seguro.</p>
 */
public class ClienteHeaderController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private Button btnBuscar;

    @FXML
    private Button btnDeseados;

    @FXML
    private Button btnCarrito;

    @FXML
    private Button btnMenu;

    @FXML
    private Label lblContadorDeseados;

    @FXML
    private Label lblContadorCarrito;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static String categoriaPendiente;
    private static String sexoPendiente;
    private static boolean activarBuscadorPendiente;

    private final GestorCarrito gestorCarrito = new GestorCarrito();
    private final GestorDeseados gestorDeseados = new GestorDeseados();

    private Runnable alSolicitarBusqueda;
    private Consumer<String> alSolicitarCategoria;
    private Consumer<String> alSolicitarSexo;
    private ContextMenu menuUsuario;

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        construirMenuUsuario();
        actualizarContadores();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    @FXML
    private void irACatalogoGeneral(ActionEvent event) {
        if (alSolicitarCategoria != null && alSolicitarSexo != null) {
            alSolicitarSexo.accept("TODOS");
            alSolicitarCategoria.accept("TODAS");
            return;
        }
        categoriaPendiente = "TODAS";
        sexoPendiente = "TODOS";
        navegarA("cliente_catalogo");
    }

    @FXML
    private void irAHombre(ActionEvent event) {
        if (alSolicitarSexo != null) {
            alSolicitarSexo.accept("HOMBRE");
            return;
        }
        sexoPendiente = "HOMBRE";
        navegarA("cliente_catalogo");
    }

    @FXML
    private void irAMujer(ActionEvent event) {
        if (alSolicitarSexo != null) {
            alSolicitarSexo.accept("MUJER");
            return;
        }
        sexoPendiente = "MUJER";
        navegarA("cliente_catalogo");
    }

    @FXML
    private void irAAccesorios(ActionEvent event) {
        if (alSolicitarCategoria != null && alSolicitarSexo != null) {
            alSolicitarSexo.accept("TODOS");
            alSolicitarCategoria.accept("ACCESORIOS");
            return;
        }
        categoriaPendiente = "ACCESORIOS";
        sexoPendiente = "TODOS";
        navegarA("cliente_catalogo");
    }

    @FXML
    private void activarBusqueda(ActionEvent event) {
        if (alSolicitarBusqueda != null) {
            alSolicitarBusqueda.run();
            return;
        }
        activarBuscadorPendiente = true;
        navegarA("cliente_catalogo");
    }

    @FXML
    private void irADeseados(ActionEvent event) {
        navegarA("cliente_deseados");
    }

    @FXML
    private void irACarrito(ActionEvent event) {
        navegarA("cliente_carrito");
    }

    @FXML
    private void mostrarMenuUsuario(ActionEvent event) {
        if (menuUsuario.isShowing()) {
            menuUsuario.hide();
        } else {
            menuUsuario.show(btnMenu, Side.BOTTOM, -130, 6);
        }
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Registra callbacks opcionales cuando la vista activa es el Catálogo,
     * para filtrar por categoría/sexo o enfocar la barra de búsqueda sin recargar toda la escena.
     */
    public void configurarCallbacksCatalogo(Runnable alSolicitarBusqueda,
                                            Consumer<String> alSolicitarCategoria,
                                            Consumer<String> alSolicitarSexo) {
        this.alSolicitarBusqueda = alSolicitarBusqueda;
        this.alSolicitarCategoria = alSolicitarCategoria;
        this.alSolicitarSexo = alSolicitarSexo;
    }

    /**
     * Consume y retorna una categoría solicitada antes de navegar al Catálogo (o {@code null}).
     */
    public static String consumirCategoriaPendiente() {
        String cat = categoriaPendiente;
        categoriaPendiente = null;
        return cat;
    }

    /**
     * Consume y retorna un filtro de sexo solicitado antes de navegar al Catálogo (o {@code null}).
     */
    public static String consumirSexoPendiente() {
        String s = sexoPendiente;
        sexoPendiente = null;
        return s;
    }

    /**
     * Consume y retorna si se solicitó enfocar el buscador al abrir el Catálogo.
     */
    public static boolean consumirActivarBuscadorPendiente() {
        boolean pendiente = activarBuscadorPendiente;
        activarBuscadorPendiente = false;
        return pendiente;
    }

    /**
     * Recalcula y muestra los badges numéricos de carrito y lista de deseados del cliente actual.
     */
    public void actualizarContadores() {
        Usuario actual = SesionActual.getUsuarioLogueado();
        if (actual == null || actual.getNumeroIdentificacion() == null) {
            actualizarBadge(lblContadorCarrito, 0);
            actualizarBadge(lblContadorDeseados, 0);
            return;
        }
        String cedula = actual.getNumeroIdentificacion();
        actualizarBadge(lblContadorCarrito, gestorCarrito.contarTotalUnidades(cedula));
        actualizarBadge(lblContadorDeseados, gestorDeseados.contarDeseados(cedula));
    }

    private void actualizarBadge(Label badge, int cantidad) {
        if (badge == null) return;
        boolean visible = cantidad > 0;
        badge.setText(cantidad > 99 ? "99+" : String.valueOf(cantidad));
        badge.setVisible(visible);
        badge.setManaged(visible);
    }

    private void construirMenuUsuario() {
        menuUsuario = new ContextMenu();
        menuUsuario.getStyleClass().add("menu-cliente-desplegable");

        MenuItem itemCatalogo = new MenuItem("Catálogo de productos");
        itemCatalogo.setOnAction(e -> navegarA("cliente_catalogo"));

        MenuItem itemHistorial = new MenuItem("Historial de compras");
        itemHistorial.setOnAction(e -> navegarA("cliente_historial"));

        MenuItem itemPerfil = new MenuItem("Mi perfil");
        itemPerfil.setOnAction(e -> mostrarDialogoPerfil());

        MenuItem itemCerrarSesion = new MenuItem("Cerrar sesión");
        itemCerrarSesion.setOnAction(e -> cerrarSesion());

        menuUsuario.getItems().addAll(
                itemCatalogo,
                itemHistorial,
                itemPerfil,
                new SeparatorMenuItem(),
                itemCerrarSesion
        );
    }

    private void mostrarDialogoPerfil() {
        Usuario actual = SesionActual.getUsuarioLogueado();
        if (actual == null) return;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Mi Perfil - Belle Store");
        dialogo.setHeaderText("Información de tu cuenta");
        Window ventana = btnMenu != null && btnMenu.getScene() != null ? btnMenu.getScene().getWindow() : null;
        if (ventana != null) {
            dialogo.initOwner(ventana);
        }
        dialogo.getDialogPane().getStylesheets().add(
                App.class.getResource("css/estilos.css").toExternalForm()
        );
        dialogo.getDialogPane().getButtonTypes().add(
                new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE)
        );

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 22, 12, 22));

        agregarFilaPerfil(grid, 0, "Nombre completo:", actual.getNombreCompleto());
        agregarFilaPerfil(grid, 1, "Identificación:", actual.getNumeroIdentificacion());
        agregarFilaPerfil(grid, 2, "Correo electrónico:", actual.getCorreo());
        agregarFilaPerfil(grid, 3, "Celular:", actual.getCelular());
        agregarFilaPerfil(grid, 4, "Dirección de entrega:", actual.getDireccion());
        agregarFilaPerfil(grid, 5, "Rol:", actual.getRol());

        dialogo.getDialogPane().setContent(grid);
        dialogo.showAndWait();
    }

    private void agregarFilaPerfil(GridPane grid, int fila, String clave, String valor) {
        Label lblClave = new Label(clave);
        lblClave.getStyleClass().add("etiqueta-dato-clave");
        Label lblValor = new Label(valor != null ? valor : "—");
        lblValor.getStyleClass().add("etiqueta-dato-valor");
        grid.add(lblClave, 0, fila);
        grid.add(lblValor, 1, fila);
    }

    private void cerrarSesion() {
        SesionActual.cerrarSesion();
        navegarA("login");
    }

    private void navegarA(String vista) {
        try {
            App.setRoot(vista);
        } catch (IOException e) {
            System.err.println("No fue posible navegar a " + vista + ": " + e.getMessage());
        }
    }
}
