package com.mycompany.belle_store.persistencia;

import com.google.gson.Gson;
import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de los pasos 2 y 3 de la auditoría:
 * guardia de rutas por rol, escritura atómica sin temporales huérfanos,
 * campos derivados, retrocompatibilidad con JSON antiguo y filtro de órdenes por nombre de cliente.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RobustezYRefactorTest {

    @BeforeAll
    public static void setUp() {
        new SeedDatos().inicializarSiEsNecesario();
    }

    @AfterEach
    public void limpiarSesion() {
        SesionActual.cerrarSesion();
    }

    @Test
    @Order(1)
    public void testGuardiaDeAccesoPorRolAntesDeCargarFxml() {
        // 1. Sin sesión iniciada: las vistas protegidas redirigen a login
        SesionActual.cerrarSesion();
        assertEquals("login", App.resolverVistaPermitida("admin_dashboard"));
        assertEquals("login", App.resolverVistaPermitida("cliente_catalogo"));
        assertEquals("login", App.resolverVistaPermitida("login"));
        assertEquals("registro", App.resolverVistaPermitida("registro"));
        assertEquals("login", App.resolverVistaPermitida(null));

        // 2. Con sesión de CLIENTE: puede entrar a cliente_catalogo, pero NO a admin_dashboard
        Usuario cliente = new Usuario("USR-C", "Cliente", "12345", "c@correo.com",
                "3000000000", "Calle 1", "hash", Usuario.ROL_CLIENTE);
        SesionActual.iniciarSesion(cliente);
        assertEquals("cliente_catalogo", App.resolverVistaPermitida("cliente_catalogo"));
        assertEquals("login", App.resolverVistaPermitida("admin_dashboard"));

        // 3. Con sesión de ADMIN: puede entrar a admin_dashboard y cliente_catalogo
        Usuario admin = new Usuario("USR-A", "Admin", "10001", "a@belle.com",
                "3000000001", "Oficina", "hash", Usuario.ROL_ADMIN);
        SesionActual.iniciarSesion(admin);
        assertEquals("admin_dashboard", App.resolverVistaPermitida("admin_dashboard"));
        assertEquals("cliente_catalogo", App.resolverVistaPermitida("cliente_catalogo"));
    }

    @Test
    @Order(2)
    public void testEscrituraAtomicaNoDejaArchivosTemporales() {
        GestorProductos gp = new GestorProductos();
        List<Producto> actuales = gp.listar();
        assertTrue(gp.guardarTodos(actuales), "El guardado atómico debe completarse con éxito");

        File dir = ConfiguracionRutas.obtenerDirectorioDatos();
        File tmpProductos = new File(dir, "Productos.json.tmp");
        assertFalse(tmpProductos.exists(), "No debe quedar ningún archivo .tmp tras guardar");
        assertEquals(actuales.size(), gp.listar().size(), "Los datos deben leerse íntegros tras guardar");
    }

    @Test
    @Order(3)
    public void testFiltroOrdenesPorNombreDeClienteYReferenciasDeProducto() {
        GestorUsuarios gu = new GestorUsuarios();
        GestorOrdenes go = new GestorOrdenes();
        Map<String, String> nombres = gu.mapearNombresPorIdentificacion();

        assertEquals("María López", nombres.get("28483"));

        List<Orden> porNombre = go.filtrar("maría", "TODOS", nombres);
        assertFalse(porNombre.isEmpty(), "Debe encontrar las órdenes de María López buscando por nombre");
        assertTrue(porNombre.stream().allMatch(o -> "28483".equals(o.getClienteIdentificacion())));

        List<Orden> inexistente = go.filtrar("cliente_inexistente_xyz", "TODOS", nombres);
        assertTrue(inexistente.isEmpty());

        List<String> ordenesConCamisaAzul = go.buscarIdsOrdenesConProducto("234");
        assertTrue(ordenesConCamisaAzul.contains("111"), "La orden 111 debe aparecer vinculada al producto 234");
    }

    @Test
    @Order(4)
    public void testGenerarSiguienteIdProducto() {
        GestorProductos gp = new GestorProductos();
        String siguiente = gp.generarSiguienteId();
        assertEquals("238", siguiente, "Tras el seed (máximo 237), el siguiente ID libre debe ser 238");
    }

    @Test
    @Order(5)
    public void testCamposDerivadosYRetrocompatibilidadJsonAntiguo() {
        Gson gson = new Gson();

        // JSON con formato anterior: usa "clienteId" y valores redundantes deliberadamente erróneos
        String jsonOrdenAntigua = "{"
                + "\"id\":\"900\","
                + "\"clienteId\":\"28483\","
                + "\"articulos\":["
                + "  {\"productoId\":\"234\",\"nombreProducto\":\"Camisa\",\"cantidad\":2,\"precioUnitario\":100000.0},"
                + "  {\"productoId\":\"235\",\"nombreProducto\":\"Pantalón\",\"cantidad\":1,\"precioUnitario\":150000.0}"
                + "],"
                + "\"totalArticulos\":999,"
                + "\"direccion\":\"Cll 1 # 2\","
                + "\"estado\":\"Pendiente\","
                + "\"valorTotal\":1.0,"
                + "\"fecha\":\"19/11/2026\""
                + "}";

        Orden orden = gson.fromJson(jsonOrdenAntigua, Orden.class);
        assertEquals("28483", orden.getClienteIdentificacion(), "Debe leer el campo heredado clienteId");
        assertEquals(3, orden.getTotalArticulos(), "totalArticulos debe calcularse desde articulos, no del campo viejo");
        assertEquals(350000.0, orden.getValorTotal(), "valorTotal debe calcularse desde articulos, no del campo viejo");

        // Al volver a serializar, escribe el nuevo nombre y omite los campos derivados redundantes
        String jsonNuevo = gson.toJson(orden);
        assertTrue(jsonNuevo.contains("\"clienteIdentificacion\""));
        assertFalse(jsonNuevo.contains("\"clienteId\""));
        assertFalse(jsonNuevo.contains("\"valorTotal\""));
        assertFalse(jsonNuevo.contains("\"totalArticulos\""));

        // JSON antiguo de Producto con "estado" contradictorio respecto al stock
        String jsonProductoAntiguo = "{"
                + "\"id\":\"500\","
                + "\"imagen\":\"img/placeholder_producto.png\","
                + "\"nombre\":\"CAMISA TEST\","
                + "\"categoria\":\"CAMISAS\","
                + "\"stock\":7,"
                + "\"estado\":\"AGOTADO\","
                + "\"precio\":120000.0"
                + "}";
        Producto prod = gson.fromJson(jsonProductoAntiguo, Producto.class);
        assertEquals("DISPONIBLE", prod.getEstado(), "El estado siempre se deriva del stock (7 => DISPONIBLE)");
        prod.setStock(0);
        assertEquals("AGOTADO", prod.getEstado(), "Al quedar stock en 0 pasa a AGOTADO");
        assertFalse(gson.toJson(prod).contains("\"estado\""), "El estado derivado no se guarda redundante en JSON");

        // Verificar que modificar la lista de artículos recalcula al instante
        orden.setArticulos(List.of(new ItemOrden("234", "Camisa", 1, 200000.0)));
        assertEquals(1, orden.getTotalArticulos());
        assertEquals(200000.0, orden.getValorTotal());
    }
}
