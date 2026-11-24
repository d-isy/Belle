package com.mycompany.belle_store.persistencia;

import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.DeseadosCliente;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en JSON para la lista de productos deseados por cliente ({@code Deseados.json}).
 * Hereda la escritura atómica de {@link GestorJson}.
 */
public class GestorDeseados extends GestorJson<DeseadosCliente> {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Deseados.json";
    private static final Type TIPO_LISTA = new TypeToken<List<DeseadosCliente>>() {}.getType();

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorDeseados() {
        super(NOMBRE_ARCHIVO, TIPO_LISTA);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Retorna una copia de los IDs de productos marcados como deseados por el cliente.
     */
    public List<String> obtenerProductosIds(String clienteIdentificacion) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cedula = clienteIdentificacion.trim();
        for (DeseadosCliente registro : listar()) {
            if (cedula.equalsIgnoreCase(registro.getClienteIdentificacion())) {
                return new ArrayList<>(registro.getProductosIds());
            }
        }
        return new ArrayList<>();
    }

    /**
     * Cuenta cuántos productos tiene el cliente en su lista de deseados.
     */
    public int contarDeseados(String clienteIdentificacion) {
        return obtenerProductosIds(clienteIdentificacion).size();
    }

    /**
     * Indica si un producto está actualmente en la lista de deseados del cliente.
     */
    public boolean esDeseado(String clienteIdentificacion, String productoId) {
        if (productoId == null) {
            return false;
        }
        String idProd = productoId.trim();
        for (String id : obtenerProductosIds(clienteIdentificacion)) {
            if (idProd.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Agrega el producto a deseados si no estaba, o lo quita si ya estaba.
     *
     * @return {@code true} si el producto quedó agregado a deseados; {@code false} si fue retirado.
     */
    public synchronized boolean alternarDeseado(String clienteIdentificacion, String productoId) {
        if (clienteIdentificacion == null || clienteIdentificacion.trim().isEmpty()
                || productoId == null || productoId.trim().isEmpty()) {
            return false;
        }
        String cedula = clienteIdentificacion.trim();
        String idProd = productoId.trim();
        List<DeseadosCliente> todos = listar();
        DeseadosCliente registro = buscarOCrear(todos, cedula);

        boolean removido = registro.getProductosIds().removeIf(id -> idProd.equalsIgnoreCase(id));
        if (removido) {
            guardarTodos(todos);
            return false;
        }

        registro.getProductosIds().add(idProd);
        guardarTodos(todos);
        return true;
    }

    /**
     * Quita explícitamente un producto de la lista de deseados del cliente.
     */
    public synchronized boolean eliminarDeseado(String clienteIdentificacion, String productoId) {
        if (clienteIdentificacion == null || productoId == null) {
            return false;
        }
        String cedula = clienteIdentificacion.trim();
        String idProd = productoId.trim();
        List<DeseadosCliente> todos = listar();

        for (DeseadosCliente registro : todos) {
            if (cedula.equalsIgnoreCase(registro.getClienteIdentificacion())) {
                boolean removido = registro.getProductosIds().removeIf(id -> idProd.equalsIgnoreCase(id));
                return removido && guardarTodos(todos);
            }
        }
        return false;
    }

    private DeseadosCliente buscarOCrear(List<DeseadosCliente> todos, String cedula) {
        for (DeseadosCliente registro : todos) {
            if (cedula.equalsIgnoreCase(registro.getClienteIdentificacion())) {
                return registro;
            }
        }
        DeseadosCliente nuevo = new DeseadosCliente(cedula, new ArrayList<>());
        todos.add(nuevo);
        return nuevo;
    }
}
