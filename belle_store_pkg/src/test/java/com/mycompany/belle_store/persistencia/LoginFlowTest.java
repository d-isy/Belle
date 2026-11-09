package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas técnicas del flujo de autenticación y sesión correspondientes a la Fase 2.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginFlowTest {

    private static GestorUsuarios gestorUsuarios;

    @BeforeAll
    public static void setUp() {
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();
        gestorUsuarios = new GestorUsuarios();
    }

    @BeforeEach
    public void limpiarSesion() {
        SesionActual.cerrarSesion();
    }

    @Test
    @Order(1)
    public void testFlujoLoginAdministrador() {
        Usuario admin = gestorUsuarios.autenticar("admin", "admin123");
        assertNotNull(admin, "Debe autenticar al administrador con 'admin'");
        assertTrue(admin.esAdministrador(), "El rol debe ser ADMIN");

        SesionActual.iniciarSesion(admin);
        assertTrue(SesionActual.haySesionIniciada(), "La sesión debe estar activa");
        assertTrue(SesionActual.esAdmin(), "Debe ser admin");
        assertFalse(SesionActual.esCliente(), "No debe ser cliente");
    }

    @Test
    @Order(2)
    public void testFlujoLoginClientePorCorreo() {
        Usuario cliente = gestorUsuarios.autenticar("maria@correo.com", "cliente123");
        assertNotNull(cliente, "Debe autenticar al cliente por su correo");
        assertTrue(cliente.esCliente(), "El rol debe ser CLIENTE");

        SesionActual.iniciarSesion(cliente);
        assertTrue(SesionActual.haySesionIniciada());
        assertTrue(SesionActual.esCliente());
        assertFalse(SesionActual.esAdmin());
        assertEquals("María López", SesionActual.getUsuarioLogueado().getNombreCompleto());
        assertEquals("Cll 1 # 2", SesionActual.getUsuarioLogueado().getDireccion());
    }

    @Test
    @Order(3)
    public void testCierreDeSesion() {
        Usuario cliente = gestorUsuarios.autenticar("maria@correo.com", "cliente123");
        assertNotNull(cliente);

        SesionActual.iniciarSesion(cliente);
        assertTrue(SesionActual.haySesionIniciada());

        SesionActual.cerrarSesion();
        assertFalse(SesionActual.haySesionIniciada(), "La sesión debe quedar cerrada");
        assertNull(SesionActual.getUsuarioLogueado(), "El usuario logueado debe ser nulo");
    }
}
