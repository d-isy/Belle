package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas técnicas automatizadas del Dashboard de Administrador (Fase 4).
 * Valida la persistencia, filtrado reactivo, renderizado monetario y operaciones CRUD
 * para las entidades Producto y Orden.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminDashboardFlowTest {

    private static GestorProductos gestorProductos;
    private static GestorOrdenes gestorOrdenes;

    @BeforeAll
    public static void setUp() {
        SeedDatos seed = new SeedDatos();
        seed.inicializarSiEsNecesario();
        gestorProductos = new GestorProductos();
        gestorOrdenes = new GestorOrdenes();

        // Limpieza de datos de prueba si existieran
        Producto pPrueba = gestorProductos.buscarPorId("TEST-999");
        if (pPrueba != null) {
            gestorProductos.eliminar("TEST-999");
        }
        Orden oPrueba = gestorOrdenes.buscarPorId("TEST-ORD-999");
        if (oPrueba != null) {
            gestorOrdenes.eliminar("TEST-ORD-999");
        }
    }

    @Test
    @Order(1)
    public void testFiltroProductosPorTerminoDeBusqueda() {
        // Búsqueda por ID existente en seed ("234")
        List<Producto> porId = gestorProductos.filtrar("234", "TODAS", "TODOS");
        assertFalse(porId.isEmpty(), "Debe encontrar el producto con ID 234");
        assertEquals("CAMISA AZUL", porId.get(0).getNombre());

        // Búsqueda por Nombre ("PANTALÓN")
        List<Producto> porNombre = gestorProductos.filtrar("pantalón", "TODAS", "TODOS");
        assertFalse(porNombre.isEmpty(), "Debe encontrar el producto por coincidencia de nombre");

        // Búsqueda por Categoría ("ACCESORIOS")
        List<Producto> porCat = gestorProductos.filtrar("accesorios", "TODAS", "TODOS");
        assertFalse(porCat.isEmpty(), "Debe encontrar productos por coincidencia de categoría");
    }

    @Test
    @Order(2)
    public void testFiltroProductosPorCategoriaYEstado() {
        // Filtrar por categoría CAMISAS
        List<Producto> camisas = gestorProductos.filtrar("", "CAMISAS", "TODOS");
        for (Producto p : camisas) {
            assertEquals("CAMISAS", p.getCategoria(), "Todos los productos filtrados deben ser de categoría CAMISAS");
        }

        // Filtrar por estado AGOTADO
        List<Producto> agotados = gestorProductos.filtrar("", "TODAS", "AGOTADO");
        for (Producto p : agotados) {
            assertEquals("AGOTADO", p.getEstado(), "Todos los productos deben estar en estado AGOTADO");
        }
    }

    @Test
    @Order(3)
    public void testFiltroOrdenesPorClienteYEstado() {
        // Filtrar por cliente existente en seed ("28483")
        List<Orden> ordenesCliente = gestorOrdenes.filtrar("28483", "TODOS");
        assertFalse(ordenesCliente.isEmpty(), "Debe encontrar órdenes para el cliente 28483");

        // Filtrar por estado de orden: No Entregado
        List<Orden> ordenesNoEntregadas = gestorOrdenes.filtrar("", "No Entregado");
        assertFalse(ordenesNoEntregadas.isEmpty(), "Debe existir al menos una orden en estado 'No Entregado' en el seed");
        for (Orden o : ordenesNoEntregadas) {
            assertEquals("No Entregado", o.getEstado(), "Las órdenes deben estar en estado 'No Entregado'");
        }

        // Filtrar por estado de orden: Entregado
        List<Orden> ordenesEntregadas = gestorOrdenes.filtrar("", "Entregado");
        assertFalse(ordenesEntregadas.isEmpty(), "Debe existir al menos una orden en estado 'Entregado' en el seed");
        for (Orden o : ordenesEntregadas) {
            assertEquals("Entregado", o.getEstado(), "Las órdenes deben estar en estado 'Entregado'");
        }

        // Filtrar por estado de orden: Pendiente (orden semilla 113)
        List<Orden> ordenesPendientes = gestorOrdenes.filtrar("", "Pendiente");
        assertFalse(ordenesPendientes.isEmpty(), "Debe existir al menos una orden en estado 'Pendiente' en el seed");
        for (Orden o : ordenesPendientes) {
            assertEquals("Pendiente", o.getEstado(), "Las órdenes deben estar en estado 'Pendiente'");
        }
        Orden ordenPendienteSeed = gestorOrdenes.buscarPorId("113");
        assertNotNull(ordenPendienteSeed, "La orden semilla 113 en estado 'Pendiente' debe existir");
        assertEquals("Pendiente", ordenPendienteSeed.getEstado());
        assertEquals(2, ordenPendienteSeed.getTotalArticulos());
    }

    @Test
    @Order(4)
    public void testOperacionesCrudProducto() {
        // 1. Crear nuevo producto de prueba (estado derivado del stock: 10 => DISPONIBLE)
        Producto nuevo = new Producto(
                "TEST-999",
                "img/placeholder_producto.png",
                "PRODUCTO DE PRUEBA",
                "ACCESORIOS",
                10,
                85000.0
        );

        boolean agregado = gestorProductos.agregar(nuevo);
        assertTrue(agregado, "Debe agregar el nuevo producto exitosamente");

        Producto recuperado = gestorProductos.buscarPorId("TEST-999");
        assertNotNull(recuperado, "El producto debe existir en Productos.json");
        assertEquals("PRODUCTO DE PRUEBA", recuperado.getNombre());
        assertEquals("DISPONIBLE", recuperado.getEstado());

        // 2. Actualizar producto: stock 0 deriva automáticamente el estado AGOTADO
        recuperado.setStock(0);
        recuperado.setPrecio(90000.0);
        boolean actualizado = gestorProductos.actualizar(recuperado);
        assertTrue(actualizado, "Debe actualizar el producto en la persistencia");

        Producto actualizadoRecuperado = gestorProductos.buscarPorId("TEST-999");
        assertEquals(0, actualizadoRecuperado.getStock());
        assertEquals(90000.0, actualizadoRecuperado.getPrecio());
        assertEquals("AGOTADO", actualizadoRecuperado.getEstado());

        // Volver a tener stock > 0 restaura DISPONIBLE automáticamente
        actualizadoRecuperado.setStock(15);
        assertTrue(gestorProductos.actualizar(actualizadoRecuperado));
        assertEquals("DISPONIBLE", gestorProductos.buscarPorId("TEST-999").getEstado());

        // 3. Eliminar producto
        boolean eliminado = gestorProductos.eliminar("TEST-999");
        assertTrue(eliminado, "Debe eliminar el producto de Productos.json");
        assertNull(gestorProductos.buscarPorId("TEST-999"), "El producto ya no debe existir");
    }

    @Test
    @Order(5)
    public void testOperacionesCrudOrden() {
        // 1. Crear orden de prueba (totalArticulos=2 y valorTotal=400000 se calculan desde items)
        List<ItemOrden> items = new ArrayList<>();
        items.add(new ItemOrden("234", "Camisa Azul", 2, 200000.0));

        Orden nueva = new Orden(
                "TEST-ORD-999",
                "99999",
                items,
                "Calle Falsa 123",
                "Pendiente",
                "15/11/2026 12:00:00"
        );

        boolean agregada = gestorOrdenes.agregar(nueva);
        assertTrue(agregada, "Debe agregar la nueva orden a Ordenes.json");

        Orden recuperada = gestorOrdenes.buscarPorId("TEST-ORD-999");
        assertNotNull(recuperada);
        assertEquals("Calle Falsa 123", recuperada.getDireccion());
        assertEquals(2, recuperada.getTotalArticulos());
        assertEquals(400000.0, recuperada.getValorTotal());

        // 2. Actualizar estado
        recuperada.setEstado("Entregado");
        recuperada.setDireccion("Avenida Siempre Viva 742");
        boolean actualizada = gestorOrdenes.actualizar(recuperada);
        assertTrue(actualizada);

        Orden actualizadaRec = gestorOrdenes.buscarPorId("TEST-ORD-999");
        assertEquals("Entregado", actualizadaRec.getEstado());
        assertEquals("Avenida Siempre Viva 742", actualizadaRec.getDireccion());

        // 3. Eliminar orden
        boolean eliminada = gestorOrdenes.eliminar("TEST-ORD-999");
        assertTrue(eliminada);
        assertNull(gestorOrdenes.buscarPorId("TEST-ORD-999"));
    }

    @Test
    @Order(6)
    public void testFormatoMonetarioYMultilinea() {
        Producto p = new Producto("234", "img/camisa.png", "CAMISA", "CAMISAS", 2, 200000.0);
        assertEquals("$200.000 COP", p.getPrecioFormateado(), "El precio formateado debe coincidir con el diseño de Figma");

        List<ItemOrden> items = new ArrayList<>();
        items.add(new ItemOrden("234", "Camisa", 1, 200000.0));
        items.add(new ItemOrden("235", "Pantalón", 2, 150000.0));

        Orden o = new Orden("111", "28483", items, "Cll 1 # 2", "Entregado", "15/11/2026");
        assertEquals("$500.000 COP", o.getValorTotalFormateado(), "El valor total formateado debe coincidir con Figma");
        assertEquals(3, o.getTotalArticulos(), "El total de artículos se deriva de las cantidades");

        String articulosFormateados = o.getArticulosFormateados();
        assertTrue(articulosFormateados.contains("• Camisa - 1"), "Debe contener viñeta del primer artículo");
        assertTrue(articulosFormateados.contains("• Pantalón - 2"), "Debe contener viñeta del segundo artículo");
    }
}
