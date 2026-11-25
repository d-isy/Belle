package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.ConfiguracionRutas;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.persistencia.GestorProductos;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.util.Formato;
import com.mycompany.belle_store.util.GeneradorComprobante;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de la vista de Historial de Compras ({@code cliente_historial.fxml}).
 * Replica el frame {@code 86:261} de Figma mostrando únicamente las órdenes del cliente autenticado,
 * con ordenamiento numérico en código/total, insignias de color, detalle completo con comprobante
 * {@code .txt} y cancelación de órdenes en estado {@code Pendiente} con devolución de stock.
 */
public class ClienteHistorialController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private ClienteHeaderController headerController;

    @FXML
    private Button btnVerDetalle;

    @FXML
    private Button btnCancelarOrden;

    @FXML
    private Label lblMensajeHistorial;

    @FXML
    private TableView<Orden> tablaHistorial;

    @FXML
    private TableColumn<Orden, String> colFecha;

    @FXML
    private TableColumn<Orden, String> colCodigo;

    @FXML
    private TableColumn<Orden, String> colTipo;

    @FXML
    private TableColumn<Orden, String> colProductos;

    @FXML
    private TableColumn<Orden, Double> colTotal;

    @FXML
    private TableColumn<Orden, String> colEstado;

    @FXML
    private Label lblContadorOrdenes;

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final ButtonType BTN_CERRAR = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
    private static final ButtonType BTN_CANCELAR_DLG = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);

    private final GestorOrdenes gestorOrdenes = new GestorOrdenes();
    private final GestorProductos gestorProductos = new GestorProductos();

    // =========================================================================
    // 3. INICIALIZACIÓN (initialize y configuración inicial)
    // =========================================================================
    @FXML
    public void initialize() {
        configurarTabla();
        cargarHistorial();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN
    // =========================================================================
    @FXML
    private void irAlCatalogo(ActionEvent event) {
        try {
            App.setRoot("cliente_catalogo");
        } catch (IOException e) {
            System.err.println("Error al regresar al catálogo: " + e.getMessage());
        }
    }

    @FXML
    private void manejarVerDetalle(ActionEvent event) {
        Orden seleccionada = tablaHistorial.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            mostrarDetalleOrden(seleccionada);
        }
    }

    @FXML
    private void manejarCancelarOrden(ActionEvent event) {
        Orden seleccionada = tablaHistorial.getSelectionModel().getSelectedItem();
        Usuario cliente = SesionActual.getUsuarioLogueado();
        if (seleccionada == null || cliente == null) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar orden - Belle Store");
        confirmacion.setHeaderText("¿Deseas cancelar la orden #" + seleccionada.getId() + "?");
        confirmacion.setContentText("Al cancelar, las unidades compradas volverán automáticamente al inventario.");
        ButtonType btnSiCancelar = new ButtonType("Sí, cancelar orden", ButtonBar.ButtonData.OK_DONE);
        confirmacion.getButtonTypes().setAll(btnSiCancelar, BTN_CANCELAR_DLG);
        Window dueno = obtenerVentana();
        if (dueno != null) {
            confirmacion.initOwner(dueno);
        }

        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != btnSiCancelar) {
            return;
        }

        btnCancelarOrden.setDisable(true);
        Task<Orden> tareaCancelar = new Task<>() {
            @Override
            protected Orden call() {
                return gestorOrdenes.cancelarOrdenPendiente(
                        seleccionada.getId(), cliente.getNumeroIdentificacion(), gestorProductos);
            }
        };

        tareaCancelar.setOnSucceeded(e -> {
            mostrarMensaje("Orden #" + seleccionada.getId()
                    + " cancelada con éxito. El stock fue restaurado en el catálogo.", false);
            cargarHistorial();
        });

        tareaCancelar.setOnFailed(e -> {
            Throwable err = tareaCancelar.getException();
            mostrarMensaje(err != null && err.getMessage() != null
                    ? err.getMessage() : "No fue posible cancelar la orden.", true);
            cargarHistorial();
        });

        Thread hilo = new Thread(tareaCancelar, "belle-cancelar-orden");
        hilo.setDaemon(true);
        hilo.start();
    }

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    private void configurarTabla() {
        colFecha.setCellValueFactory(dato -> new SimpleStringProperty(dato.getValue().getFecha()));

        colCodigo.setCellValueFactory(dato -> new SimpleStringProperty(dato.getValue().getId()));
        colCodigo.setComparator(Formato::compararNumericoOTexto);
        colCodigo.getStyleClass().add("celda-centrada");

        colTipo.setCellValueFactory(dato -> new SimpleStringProperty(dato.getValue().getTipo()));
        colTipo.getStyleClass().add("celda-centrada");

        colProductos.setCellValueFactory(dato -> new SimpleStringProperty(dato.getValue().getResumenProductos()));
        colProductos.setCellFactory(col -> new TableCell<>() {
            private final Label etiqueta = new Label();
            {
                etiqueta.setWrapText(true);
                etiqueta.getStyleClass().add("texto-productos-historial");
            }
            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                if (empty || texto == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    etiqueta.setText(texto);
                    setGraphic(etiqueta);
                    setText(null);
                }
            }
        });

        // Ordenamiento numérico real en la columna TOTAL (siempre incluyendo impuestos y envío)
        colTotal.setCellValueFactory(dato -> new ReadOnlyObjectWrapper<>(dato.getValue().getTotalConImpuestosYEnvio()));
        colTotal.getStyleClass().add("celda-moneda");
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : Formato.moneda(valor));
            }
        });

        // Insignias de color para estados (decisión visual aprobada)
        colEstado.setCellValueFactory(dato -> new SimpleStringProperty(dato.getValue().getEstado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(estado.toUpperCase());
                if ("Entregado".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-entregado");
                } else if ("Pendiente".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-pendiente");
                } else if ("Cancelado".equalsIgnoreCase(estado)) {
                    badge.getStyleClass().add("badge-estado-cancelado");
                } else {
                    badge.getStyleClass().add("badge-estado-no-entregado");
                }
                setGraphic(badge);
                setAlignment(Pos.CENTER);
                setText(null);
            }
        });

        // Actualizar botones de acción según selección y abrir detalle con doble clic
        tablaHistorial.getSelectionModel().selectedItemProperty().addListener((obs, oldV, nueva) -> {
            if (btnVerDetalle != null) {
                btnVerDetalle.setDisable(nueva == null);
            }
            if (btnCancelarOrden != null) {
                btnCancelarOrden.setDisable(nueva == null || !"Pendiente".equalsIgnoreCase(nueva.getEstado()));
            }
        });

        tablaHistorial.setRowFactory(tv -> {
            TableRow<Orden> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY
                        && evento.getClickCount() == 2
                        && !fila.isEmpty()) {
                    mostrarDetalleOrden(fila.getItem());
                }
            });
            return fila;
        });

        Label placeholder = new Label("Aún no has realizado ninguna compra.");
        placeholder.getStyleClass().add("placeholder-tabla");
        tablaHistorial.setPlaceholder(placeholder);
    }

    private void cargarHistorial() {
        Usuario actual = SesionActual.getUsuarioLogueado();
        if (actual == null) {
            tablaHistorial.setItems(FXCollections.observableArrayList());
            lblContadorOrdenes.setText("Mostrando 0 órdenes");
            return;
        }
        List<Orden> ordenes = gestorOrdenes.listarPorCliente(actual.getNumeroIdentificacion());
        tablaHistorial.setItems(FXCollections.observableArrayList(ordenes));
        lblContadorOrdenes.setText("Mostrando " + ordenes.size()
                + " orden(es) de compra · Haz doble clic en una fila para ver el detalle o guardar comprobante.");
        if (btnVerDetalle != null) btnVerDetalle.setDisable(true);
        if (btnCancelarOrden != null) btnCancelarOrden.setDisable(true);
    }

    private void mostrarDetalleOrden(Orden orden) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Detalle de Orden #" + orden.getId() + " - Belle Store");
        dialogo.setHeaderText("Orden de compra #" + orden.getId() + " (" + orden.getEstado() + ")");
        Window dueno = obtenerVentana();
        if (dueno != null) {
            dialogo.initOwner(dueno);
        }
        dialogo.getDialogPane().getStylesheets().add(
                App.class.getResource("css/estilos.css").toExternalForm()
        );

        ButtonType btnComprobante = new ButtonType("Guardar comprobante (.txt)", ButtonBar.ButtonData.OTHER);
        dialogo.getDialogPane().getButtonTypes().addAll(btnComprobante, BTN_CERRAR);

        VBox contenido = new VBox(12);
        contenido.setPadding(new Insets(16, 22, 12, 22));
        contenido.setPrefWidth(460);

        GridPane cabecera = new GridPane();
        cabecera.setHgap(14);
        cabecera.setVgap(6);
        cabecera.add(new Label("Fecha:"), 0, 0);
        cabecera.add(new Label(orden.getFecha() != null ? orden.getFecha() : "—"), 1, 0);
        cabecera.add(new Label("Dirección de entrega:"), 0, 1);
        cabecera.add(new Label(orden.getDireccion() != null ? orden.getDireccion() : "—"), 1, 1);
        cabecera.add(new Label("Total artículos:"), 0, 2);
        cabecera.add(new Label(String.valueOf(orden.getTotalArticulos())), 1, 2);

        Label lblArticulosTitulo = new Label("Artículos:");
        lblArticulosTitulo.getStyleClass().add("etiqueta-estado-dialogo");
        VBox listaArticulos = new VBox(4);
        for (ItemOrden item : orden.getArticulos()) {
            Label linea = new Label("• " + item.getNombreProducto()
                    + " × " + item.getCantidad()
                    + " (" + Formato.moneda(item.getPrecioUnitario()) + " c/u) = "
                    + Formato.moneda(item.getSubtotal()));
            linea.setWrapText(true);
            listaArticulos.getChildren().add(linea);
        }

        GridPane totales = new GridPane();
        totales.setHgap(14);
        totales.setVgap(6);
        totales.add(new Label("Subtotal artículos:"), 0, 0);
        totales.add(new Label(Formato.moneda(orden.getSubtotalArticulos())), 1, 0);
        totales.add(new Label("Envío:"), 0, 1);
        totales.add(new Label(orden.getCostoEnvioEfectivo() <= 0
                ? "Gratis ($0 COP)" : Formato.moneda(orden.getCostoEnvioEfectivo())), 1, 1);
        totales.add(new Label("Impuestos (IVA 19%):"), 0, 2);
        totales.add(new Label(Formato.moneda(orden.getImpuestosEfectivos())), 1, 2);

        Label lblTotalEtq = new Label("TOTAL:");
        lblTotalEtq.getStyleClass().add("etiqueta-estado-dialogo");
        Label lblTotalVal = new Label(orden.getTotalConImpuestosYEnvioFormateado());
        lblTotalVal.getStyleClass().add("etiqueta-estado-dialogo");
        totales.add(lblTotalEtq, 0, 3);
        totales.add(lblTotalVal, 1, 3);

        contenido.getChildren().addAll(cabecera, new Separator(), lblArticulosTitulo, listaArticulos,
                new Separator(), totales);
        dialogo.getDialogPane().setContent(contenido);

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnComprobante) {
            guardarComprobanteConSelector(orden, dueno);
        }
    }

    private void guardarComprobanteConSelector(Orden orden, Window dueno) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar comprobante de compra");
        selector.setInitialDirectory(ConfiguracionRutas.obtenerDirectorioRecibos());
        selector.setInitialFileName("comprobante_orden_" + orden.getId() + ".txt");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt"));
        File destino = selector.showSaveDialog(dueno);
        if (destino != null) {
            try {
                GeneradorComprobante.guardarEnArchivo(orden, SesionActual.getUsuarioLogueado(), destino);
                mostrarMensaje("Comprobante guardado en: " + destino.getAbsolutePath(), false);
            } catch (IOException ex) {
                mostrarMensaje("No fue posible guardar el comprobante: " + ex.getMessage(), true);
            }
        }
    }

    private void mostrarMensaje(String texto, boolean esError) {
        if (lblMensajeHistorial == null) return;
        lblMensajeHistorial.setText(texto);
        lblMensajeHistorial.getStyleClass().setAll("label", esError ? "mensaje-error" : "mensaje-exito");
        lblMensajeHistorial.setVisible(true);
        lblMensajeHistorial.setManaged(true);
    }

    private Window obtenerVentana() {
        return tablaHistorial != null && tablaHistorial.getScene() != null
                ? tablaHistorial.getScene().getWindow() : null;
    }
}
