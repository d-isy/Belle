package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas técnicas del flujo de Registro de Clientes correspondientes a la Fase 3.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegistroFlowTest {

    private static GestorUsuarios gestorUsuarios;

    @BeforeAll
    public static void setUp() {
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();
        gestorUsuarios = new GestorUsuarios();

        // Asegurar idempotencia: si el usuario de prueba ya existía de una ejecución previa, limpiarlo
        Usuario existente = gestorUsuarios.buscarPorCorreo("carlos.mendoza@correo.com");
        if (existente != null) {
            gestorUsuarios.eliminar(existente.getId());
        }
    }

    @BeforeEach
    public void limpiarSesion() {
        SesionActual.cerrarSesion();
    }

    @Test
    @Order(1)
    public void testRegistroNuevoClienteExitoso() {
        Usuario nuevo = new Usuario(
                null,
                "Carlos Mendoza",
                "9876543210",
                "carlos.mendoza@correo.com",
                "3109876543",
                "Carrera 7 # 72 - 10",
                "carlosPass123",
                Usuario.ROL_CLIENTE
        );

        boolean guardado = gestorUsuarios.guardar(nuevo);
        assertTrue(guardado, "El nuevo cliente debe guardarse exitosamente en Usuarios.json");

        Usuario obtenido = gestorUsuarios.buscarPorCorreo("carlos.mendoza@correo.com");
        assertNotNull(obtenido, "El usuario guardado debe ser consultable por correo");
        assertEquals("Carlos Mendoza", obtenido.getNombreCompleto());
        assertEquals("9876543210", obtenido.getNumeroIdentificacion());
        assertTrue(obtenido.esCliente(), "El rol debe ser CLIENTE");
    }

    @Test
    @Order(2)
    public void testRechazoDuplicadoCorreo() {
        // Intento de registrar con un correo ya existente
        Usuario duplicado = new Usuario(
                null,
                "Otro Usuario",
                "1122334455",
                "carlos.mendoza@correo.com", // Ya existe
                "3000000001",
                "Calle 100",
                "clave123",
                Usuario.ROL_CLIENTE
        );

        boolean guardado = gestorUsuarios.guardar(duplicado);
        assertFalse(guardado, "No debe permitir guardar usuarios con correo duplicado");
    }

    @Test
    @Order(3)
    public void testRechazoDuplicadoIdentificacion() {
        // Intento de registrar con un número de cédula ya existente
        Usuario duplicado = new Usuario(
                null,
                "Usuario Con Misma Cedula",
                "9876543210", // Ya existe
                "correo.distinto@correo.com",
                "3000000002",
                "Avenida 68",
                "clave123",
                Usuario.ROL_CLIENTE
        );

        boolean guardado = gestorUsuarios.guardar(duplicado);
        assertFalse(guardado, "No debe permitir guardar usuarios con número de identificación duplicado");
    }

    @Test
    @Order(4)
    public void testLoginInmediatoConUsuarioRegistrado() {
        // El cliente recién registrado debe poder iniciar sesión de inmediato
        Usuario loginValido = gestorUsuarios.autenticar("carlos.mendoza@correo.com", "carlosPass123");
        assertNotNull(loginValido, "El cliente recién registrado debe autenticarse exitosamente");
        assertEquals("Carlos Mendoza", loginValido.getNombreCompleto());

        // Debe mantenerse la sesión en memoria
        SesionActual.iniciarSesion(loginValido);
        assertTrue(SesionActual.haySesionIniciada());
        assertTrue(SesionActual.esCliente());
        assertEquals("Carrera 7 # 72 - 10", SesionActual.getUsuarioLogueado().getDireccion());
    }

    @Test
    @Order(5)
    public void testValidacionFiltrosTextoYPatrones() {
        // Validación de nombres: solo letras y espacios permitidos
        String patronNombre = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$";
        assertTrue("Carlos Mendoza".matches(patronNombre), "Nombre con letras válidas debe ser aceptado");
        assertTrue("María José Peña".matches(patronNombre), "Nombre con tildes y ñ debe ser aceptado");
        assertFalse("Carlos 123".matches(patronNombre), "Nombre con números no debe ser aceptado");
        assertFalse("Carlos@Mendoza".matches(patronNombre), "Nombre con símbolos no debe ser aceptado");

        // Validación de números de identificación y celular: solo dígitos
        String patronDigitos = "^\\d*$";
        assertTrue("1020304050".matches(patronDigitos), "Identificación numérica debe ser aceptada");
        assertFalse("102030405A".matches(patronDigitos), "Identificación con letras debe ser rechazada");
        assertTrue("3001234567".matches(patronDigitos), "Celular numérico debe ser aceptado");
        assertFalse("300123456X".matches(patronDigitos), "Celular con letras debe ser rechazado");

        // Validación de correo electrónico
        String patronCorreo = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        assertTrue("cliente@correo.com".matches(patronCorreo));
        assertFalse("correo-invalido.com".matches(patronCorreo));
        assertFalse("cliente@".matches(patronCorreo));
    }
}
