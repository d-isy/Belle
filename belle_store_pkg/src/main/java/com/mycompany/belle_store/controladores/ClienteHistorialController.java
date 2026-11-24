package com.mycompany.belle_store.controladores;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.persistencia.GestorOrdenes;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.util.Formato;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.List;

/**
 * Controlador de la vista de Historial de Compras ({@code cliente_historial.fxml}).
 * Replica el frame {@code 86:261} de Figma mostrando únicamente las órdenes del cliente autenticado,
 * con ordenamiento numérico en código/total e insignias de color por estado logístico.
 */
public class ClienteHistorialController {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML)
    // =========================================================================
    @FXML
    private ClienteHeaderController headerController;

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
    private final GestorOrdenes gestorOrdenes = new GestorOrdenes();

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

        // Ordenamiento numérico real en la columna TOTAL
        colTotal.setCellValueFactory(dato -> new ReadOnlyObjectWrapper<>(dato.getValue().getValorTotal()));
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
                } else {
                    badge.getStyleClass().add("badge-estado-no-entregado");
                }
                setGraphic(badge);
                setAlignment(Pos.CENTER);
                setText(null);
            }
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
        lblContadorOrdenes.setText("Mostrando " + ordenes.size() + " orden(es) de compra");
    }
}
