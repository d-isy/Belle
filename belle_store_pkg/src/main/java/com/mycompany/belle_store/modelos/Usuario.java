package com.mycompany.belle_store.modelos;

/**
 * Modelo de datos representativo de un Usuario en la plataforma Belle Store.
 * Cumple con fines pedagógicos y es serializado/deserializado con Gson.
 */
public class Usuario {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private String id;
    private String nombreCompleto;
    private String numeroIdentificacion;
    private String correo;
    private String celular;
    private String direccion;
    private String contrasena;
    private String rol; // "ADMIN" o "CLIENTE"

    // Constantes pedagógicas de roles
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_CLIENTE = "CLIENTE";

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructores)
    // =========================================================================
    /**
     * Constructor por defecto requerido para la deserialización con Gson.
     */
    public Usuario() {
    }

    /**
     * Constructor completo para registrar o instanciar usuarios.
     */
    public Usuario(String id, String nombreCompleto, String numeroIdentificacion,
                   String correo, String celular, String direccion,
                   String contrasena, String rol) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.numeroIdentificacion = numeroIdentificacion;
        this.correo = correo;
        this.celular = celular;
        this.direccion = direccion;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica en clases de modelo
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Getters, Setters y Validaciones)
    // =========================================================================
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(String numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean esAdministrador() {
        return ROL_ADMIN.equalsIgnoreCase(this.rol);
    }

    public boolean esCliente() {
        return ROL_CLIENTE.equalsIgnoreCase(this.rol);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id='" + id + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", correo='" + correo + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
