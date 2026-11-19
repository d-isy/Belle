package com.mycompany.belle_store.persistencia;

import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.seguridad.HashContrasena;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de persistencia en formato JSON para la entidad Usuario.
 * Hereda lectura/escritura atómica de {@link GestorJson} y añade reglas de unicidad y autenticación.
 */
public class GestorUsuarios extends GestorJson<Usuario> {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Usuarios.json";
    private static final Type TIPO_LISTA = new TypeToken<List<Usuario>>() {}.getType();

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorUsuarios() {
        super(NOMBRE_ARCHIVO, TIPO_LISTA);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Agrega un nuevo usuario a la lista persistente si no existe duplicado.
     */
    public synchronized boolean guardar(Usuario nuevoUsuario) {
        if (nuevoUsuario == null) return false;
        List<Usuario> lista = listar();

        // Validar no duplicidad de correo ni de identificación
        if (existeCorreo(nuevoUsuario.getCorreo()) || existeIdentificacion(nuevoUsuario.getNumeroIdentificacion())) {
            return false;
        }

        // Asignar ID pedagógico si no tiene uno (máximo existente + 1: nunca repite IDs tras eliminar)
        if (nuevoUsuario.getId() == null || nuevoUsuario.getId().trim().isEmpty()) {
            nuevoUsuario.setId(generarSiguienteId(lista));
        }

        // La contraseña SIEMPRE se almacena como hash PBKDF2 (nunca en texto plano)
        if (!HashContrasena.esHash(nuevoUsuario.getContrasena())) {
            nuevoUsuario.setContrasena(HashContrasena.generarHash(nuevoUsuario.getContrasena()));
        }

        lista.add(nuevoUsuario);
        return guardarTodos(lista);
    }

    /**
     * Calcula el siguiente ID con formato {@code USR-n}: toma el mayor número usado por IDs de ese
     * formato (ej. {@code USR-001} cuenta como 1) y suma 1. IDs especiales como {@code USR-ADMIN}
     * no participan. Así, eliminar un usuario nunca provoca que el siguiente repita un ID existente.
     */
    private String generarSiguienteId(List<Usuario> lista) {
        long maximo = 0;
        for (Usuario u : lista) {
            String id = u.getId();
            if (id != null && id.toUpperCase().matches("USR-\\d+")) {
                maximo = Math.max(maximo, Long.parseLong(id.substring(4)));
            }
        }
        return "USR-" + (maximo + 1);
    }

    /**
     * Busca un usuario por su correo electrónico (ignorando mayúsculas/minúsculas).
     */
    public Usuario buscarPorCorreo(String correo) {
        if (correo == null) return null;
        for (Usuario u : listar()) {
            if (correo.trim().equalsIgnoreCase(u.getCorreo())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Busca un usuario por su número de identificación.
     */
    public Usuario buscarPorIdentificacion(String identificacion) {
        if (identificacion == null) return null;
        for (Usuario u : listar()) {
            if (identificacion.trim().equalsIgnoreCase(u.getNumeroIdentificacion())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Construye un mapa {@code numeroIdentificacion -> nombreCompleto} para mostrar y buscar
     * nombres de clientes sin releer el JSON por cada fila de la tabla de órdenes.
     */
    public Map<String, String> mapearNombresPorIdentificacion() {
        Map<String, String> mapa = new HashMap<>();
        for (Usuario u : listar()) {
            if (u.getNumeroIdentificacion() != null && u.getNombreCompleto() != null) {
                mapa.put(u.getNumeroIdentificacion(), u.getNombreCompleto());
            }
        }
        return mapa;
    }

    public boolean existeCorreo(String correo) {
        return buscarPorCorreo(correo) != null;
    }

    public boolean existeIdentificacion(String identificacion) {
        return buscarPorIdentificacion(identificacion) != null;
    }

    /**
     * Elimina un usuario por su identificador único.
     */
    public synchronized boolean eliminar(String id) {
        if (id == null) return false;
        List<Usuario> lista = listar();
        boolean removido = lista.removeIf(u -> id.equalsIgnoreCase(u.getId()));
        return removido && guardarTodos(lista);
    }

    /**
     * Valida credenciales según la regla del proyecto:
     * El campo 'Usuario' valida ÚNICAMENTE contra el Correo Electrónico registrado del usuario,
     * además de permitir el acceso directo con el usuario 'admin' configurado para el Administrador.
     *
     * @param identificador Correo del usuario o 'admin'
     * @param contrasena Contraseña en texto plano
     * @return El Usuario autenticado si las credenciales son válidas, o null si son incorrectas.
     */
    public Usuario autenticar(String identificador, String contrasena) {
        if (identificador == null || contrasena == null) {
            return null;
        }
        String idLimpio = identificador.trim();

        for (Usuario u : listar()) {
            boolean coincideIdentificador = false;

            // Caso especial del usuario 'admin'
            if ("admin".equalsIgnoreCase(idLimpio) && u.esAdministrador()) {
                coincideIdentificador = true;
            } else if (u.getCorreo() != null && u.getCorreo().equalsIgnoreCase(idLimpio)) {
                coincideIdentificador = true;
            }

            if (coincideIdentificador && verificarYMigrar(u, contrasena)) {
                return u;
            }
        }

        return null;
    }

    /**
     * Verifica la contraseña contra el hash almacenado. Si el registro aún tiene una contraseña
     * en texto plano (por ejemplo, añadida a mano en el JSON), se acepta una única vez y se
     * reemplaza de inmediato por su hash PBKDF2 (migración transparente).
     */
    private boolean verificarYMigrar(Usuario u, String contrasena) {
        String almacenada = u.getContrasena();
        if (almacenada == null) {
            return false;
        }
        if (HashContrasena.esHash(almacenada)) {
            return HashContrasena.verificar(contrasena, almacenada);
        }
        if (almacenada.equals(contrasena)) {
            synchronized (this) {
                List<Usuario> lista = listar();
                for (Usuario guardado : lista) {
                    if (u.getId() != null && u.getId().equals(guardado.getId())) {
                        guardado.setContrasena(HashContrasena.generarHash(contrasena));
                        u.setContrasena(guardado.getContrasena());
                        guardarTodos(lista);
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
