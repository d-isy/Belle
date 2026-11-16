package com.mycompany.belle_store.persistencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.seguridad.HashContrasena;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en formato JSON para la entidad Usuario.
 * Emplea la librería Google Gson con formato legible (pretty printing).
 */
public class GestorUsuarios {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private static final String NOMBRE_ARCHIVO = "Usuarios.json";
    private final Gson gson;
    private final File archivo;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public GestorUsuarios() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.archivo = ConfiguracionRutas.obtenerArchivo(NOMBRE_ARCHIVO);
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Lee y retorna la lista completa de usuarios almacenados en Usuarios.json.
     */
    public synchronized List<Usuario> listar() {
        if (!archivo.exists() || archivo.length() == 0) {
            return new ArrayList<>();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            Type tipoLista = new TypeToken<List<Usuario>>() {}.getType();
            List<Usuario> usuarios = gson.fromJson(reader, tipoLista);
            return (usuarios != null) ? usuarios : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda la lista completa de usuarios sobrescribiendo el archivo Usuarios.json.
     */
    public synchronized boolean guardarTodos(List<Usuario> usuarios) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            gson.toJson(usuarios, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Error al escribir en " + NOMBRE_ARCHIVO + ": " + e.getMessage());
            return false;
        }
    }

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

        // Asignar ID pedagógico si no tiene uno
        if (nuevoUsuario.getId() == null || nuevoUsuario.getId().trim().isEmpty()) {
            nuevoUsuario.setId("USR-" + (lista.size() + 1));
        }

        // La contraseña SIEMPRE se almacena como hash PBKDF2 (nunca en texto plano)
        if (!HashContrasena.esHash(nuevoUsuario.getContrasena())) {
            nuevoUsuario.setContrasena(HashContrasena.generarHash(nuevoUsuario.getContrasena()));
        }

        lista.add(nuevoUsuario);
        return guardarTodos(lista);
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
        if (removido) {
            return guardarTodos(lista);
        }
        return false;
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
