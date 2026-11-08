package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias automatizadas del Agente Tester para la Fase 1.
 * Valida la serialización/deserialización JSON, la regla de negocio de autenticación
 * por correo / 'admin', los filtros de productos y órdenes, y el manejo de SesionActual.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistenciaTest {

    private static GestorUsuarios gestorUsuarios;
    private static GestorProductos gestorProductos;
    private static GestorOrdenes gestorOrdenes;

    @BeforeAll
    public static void setUp() {
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();

        gestorUsuarios = new GestorUsuarios();
        gestorProductos = new GestorProductos();
        gestorOrdenes = new GestorOrdenes();
    }

    @Test
    @Order(1)
    public void testSeedInicialUsuarios() {
        List<Usuario> usuarios = gestorUsuarios.listar();
        assertNotNull(usuarios, "La lista de usuarios no debe ser nula");
        assertFalse(usuarios.isEmpty(), "La lista de usuarios debe contener al menos los del seed");

        boolean tieneAdmin = usuarios.stream().anyMatch(u -> "admin@belle.com".equalsIgnoreCase(u.getCorreo()) && u.esAdministrador());
        boolean tieneCliente = usuarios.stream().anyMatch(u -> "maria@correo.com".equalsIgnoreCase(u.getCorreo()) && u.esCliente());

        assertTrue(tieneAdmin, "Debe existir un usuario administrador en el seed");
        assertTrue(tieneCliente, "Debe existir un usuario cliente en el seed");
    }

    @Test
    @Order(2)
    public void testReglaAutenticacionUsuario() {
        // 1. Acceso del administrador con la palabra clave 'admin'
        Usuario adminPorAlias = gestorUsuarios.autenticar("admin", "admin123");
        assertNotNull(adminPorAlias, "El administrador debe poder autenticarse con 'admin'");
        assertTrue(adminPorAlias.esAdministrador(), "El rol debe ser ADMIN");

        // 2. Acceso del administrador con su correo
        Usuario adminPorCorreo = gestorUsuarios.autenticar("admin@belle.com", "admin123");
        assertNotNull(adminPorCorreo, "El administrador debe poder autenticarse con su correo");

        // 3. Acceso del cliente ÚNICAMENTE con su correo
        Usuario clienteValido = gestorUsuarios.autenticar("maria@correo.com", "cliente123");
        assertNotNull(clienteValido, "El cliente debe poder autenticarse con su correo registrado");
        assertTrue(clienteValido.esCliente(), "El rol debe ser CLIENTE");

        // 4. Intento fallido con número de identificación (debe fallar según la regla)
        Usuario clientePorId = gestorUsuarios.autenticar("28483", "cliente123");
        assertNull(clientePorId, "El Login no debe permitir identificadores que no sean correo (o 'admin')");

        // 5. Intento fallido con contraseña incorrecta
        Usuario passInvalida = gestorUsuarios.autenticar("maria@correo.com", "clave_erronea");
        assertNull(passInvalida, "Contraseña incorrecta debe retornar null");
    }

    @Test
    @Order(3)
    public void testGestionProductosYFiltros() {
        List<Producto> productos = gestorProductos.listar();
        assertFalse(productos.isEmpty(), "Deben existir productos en el seed");

        // Verificar producto de Figma
        Producto p234 = gestorProductos.buscarPorId("234");
        assertNotNull(p234, "El producto 234 (Camisa Azul) debe existir");
        assertEquals("CAMISAS", p234.getCategoria());
        assertEquals("DISPONIBLE", p234.getEstado());

        // Filtrado por categoría
        List<Producto> camisas = gestorProductos.filtrar(null, "CAMISAS", null);
        assertFalse(camisas.isEmpty(), "El filtro por categoría CAMISAS debe retornar resultados");
        assertTrue(camisas.stream().allMatch(p -> p.getCategoria().equalsIgnoreCase("CAMISAS")));

        // Filtrado por búsqueda de texto
        List<Producto> busqueda = gestorProductos.filtrar("azul", null, null);
        assertEquals(1, busqueda.size(), "Debe encontrar 'CAMISA AZUL'");
    }

    @Test
    @Order(4)
    public void testGestionOrdenesYFiltros() {
        List<Orden> ordenes = gestorOrdenes.listar();
        assertFalse(ordenes.isEmpty(), "Deben existir órdenes en el seed");

        // Verificar orden 111 de Figma
        Orden o111 = gestorOrdenes.buscarPorId("111");
        assertNotNull(o111, "La orden 111 debe existir");
        assertEquals("28483", o111.getClienteId());
        assertEquals("No Entregado", o111.getEstado());
        assertEquals(1, o111.getTotalArticulos());

        // Filtrado por estado
        List<Orden> noEntregadas = gestorOrdenes.filtrar(null, "No Entregado");
        assertFalse(noEntregadas.isEmpty());
        assertTrue(noEntregadas.stream().allMatch(o -> o.getEstado().equalsIgnoreCase("No Entregado")));
    }

    @Test
    @Order(5)
    public void testManejoSesionActual() {
        Usuario cliente = gestorUsuarios.buscarPorCorreo("maria@correo.com");
        assertNotNull(cliente);

        // Iniciar sesión
        SesionActual.iniciarSesion(cliente);
        assertTrue(SesionActual.haySesionIniciada());
        assertTrue(SesionActual.esCliente());
        assertFalse(SesionActual.esAdmin());
        assertEquals("María López", SesionActual.getUsuarioLogueado().getNombreCompleto());

        // Cerrar sesión
        SesionActual.cerrarSesion();
        assertFalse(SesionActual.haySesionIniciada());
        assertNull(SesionActual.getUsuarioLogueado());
    }
}
