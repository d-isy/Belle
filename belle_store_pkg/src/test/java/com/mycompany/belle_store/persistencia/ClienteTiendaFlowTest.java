package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.App;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.ResumenCompra;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.sesion.SesionActual;
import com.mycompany.belle_store.util.GeneradorComprobante;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas automatizadas de la Fase 5 (Experiencia de Cliente):
 * cálculo de IVA 19% y envío escalonado, persistencia de carrito y deseados por cliente,
 * flujo completo de compra con descuento de inventario, validación de stock insuficiente,
 * historial filtrado por cliente y guardia de rutas para las 4 vistas de cliente.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClienteTiendaFlowTest {

    private static final String CLIENTE_A = "990011";
    private static final String CLIENTE_B = "990022";

    @BeforeAll
    public static void setUp() {
        new SeedDatos().inicializarSiEsNecesario();
    }

    @AfterEach
    public void limpiarEstado() {
        SesionActual.cerrarSesion();
        GestorCarrito gc = new GestorCarrito();
        gc.vaciarCarrito(CLIENTE_A);
        gc.vaciarCarrito(CLIENTE_B);
    }

    @Test
    @Order(1)
    public void testCalculoImpuestosYEnvioEscalonado() {
        // Carrito vacío
        ResumenCompra vacio = ResumenCompra.calcular(0.0);
        assertEquals(0.0, vacio.getSubtotal());
        assertEquals(0.0, vacio.getCostoEnvio());
        assertEquals(0.0, vacio.getImpuestos());
        assertEquals(0.0, vacio.getTotal());

        // Compra menor a $300.000 COP: envío $10.000 + IVA 19%
        ResumenCompra menor = ResumenCompra.calcular(200000.0);
        assertEquals(10000.0, menor.getCostoEnvio());
        assertEquals(38000.0, menor.getImpuestos());
        assertEquals(248000.0, menor.getTotal());
        assertFalse(menor.esEnvioGratis());

        // Compra igual a $300.000 COP: envío gratis ($0) + IVA 19%
        ResumenCompra umbral = ResumenCompra.calcular(300000.0);
        assertEquals(0.0, umbral.getCostoEnvio());
        assertEquals(57000.0, umbral.getImpuestos());
        assertEquals(357000.0, umbral.getTotal());
        assertTrue(umbral.esEnvioGratis());
    }

    @Test
    @Order(2)
    public void testPersistenciaCarritoYControlDeStock() {
        GestorCarrito gc = new GestorCarrito();

        // Agregar dentro del stock disponible (stock = 2)
        assertTrue(gc.agregarProducto(CLIENTE_A, "234", 1, 2));
        assertTrue(gc.agregarProducto(CLIENTE_A, "234", 1, 2));
        assertEquals(2, gc.contarTotalUnidades(CLIENTE_A));

        // Intentar superar el stock disponible debe rechazarse
        assertFalse(gc.agregarProducto(CLIENTE_A, "234", 1, 2));
        assertFalse(gc.actualizarCantidad(CLIENTE_A, "234", 3, 2));
        assertEquals(2, gc.contarTotalUnidades(CLIENTE_A));

        // Aislamiento entre clientes
        assertEquals(0, gc.contarTotalUnidades(CLIENTE_B));

        // Actualizar a 1 unidad y luego a 0 (elimina el ítem)
        assertTrue(gc.actualizarCantidad(CLIENTE_A, "234", 1, 2));
        assertEquals(1, gc.contarTotalUnidades(CLIENTE_A));
        assertTrue(gc.actualizarCantidad(CLIENTE_A, "234", 0, 2));
        assertEquals(0, gc.contarTotalUnidades(CLIENTE_A));
    }

    @Test
    @Order(3)
    public void testPersistenciaDeseadosTogglePorCliente() {
        GestorDeseados gd = new GestorDeseados();

        // Asegurar estado limpio inicial
        gd.eliminarDeseado(CLIENTE_A, "234");
        gd.eliminarDeseado(CLIENTE_B, "234");

        // Toggle: agrega y luego quita
        assertTrue(gd.alternarDeseado(CLIENTE_A, "234"));
        assertTrue(gd.esDeseado(CLIENTE_A, "234"));
        assertEquals(1, gd.contarDeseados(CLIENTE_A));
        assertFalse(gd.esDeseado(CLIENTE_B, "234"));

        assertFalse(gd.alternarDeseado(CLIENTE_A, "234"));
        assertFalse(gd.esDeseado(CLIENTE_A, "234"));
        assertEquals(0, gd.contarDeseados(CLIENTE_A));
    }

    @Test
    @Order(4)
    public void testProcesarCompraDescuentaStockCreaOrdenYVaciaCarrito() {
        GestorProductos gp = new GestorProductos();
        GestorCarrito gc = new GestorCarrito();
        GestorOrdenes go = new GestorOrdenes();

        String idProd = "950";
        gp.eliminar(idProd);
        Producto temporal = new Producto(idProd, "img/camisa_azul.png", "CAMISA PRUEBA F5", "CAMISAS", 2, 100000.0);
        assertTrue(gp.agregar(temporal));

        Usuario cliente = new Usuario("USR-F5", "Cliente Prueba", CLIENTE_A,
                "f5@correo.com", "3001112233", "Calle 10 # 20-30", "hash", Usuario.ROL_CLIENTE);

        assertTrue(gc.agregarProducto(CLIENTE_A, idProd, 2, 2));

        Orden orden = go.procesarCompra(cliente, "Calle 10 # 20-30", gc, gp);
        try {
            assertNotNull(orden);
            assertEquals(CLIENTE_A, orden.getClienteIdentificacion());
            assertEquals("Pendiente", orden.getEstado());
            assertEquals("ORDEN DE COMPRA", orden.getTipo());
            assertEquals(200000.0, orden.getSubtotalArticulos());
            assertEquals(10000.0, orden.getCostoEnvio());
            assertEquals(38000.0, orden.getImpuestos());
            assertEquals(248000.0, orden.getValorTotal());

            // El stock debe haber bajado a 0 => AGOTADO
            Producto actualizado = gp.buscarPorId(idProd);
            assertNotNull(actualizado);
            assertEquals(0, actualizado.getStock());
            assertEquals("AGOTADO", actualizado.getEstado());

            // El carrito quedó vacío tras comprar
            assertEquals(0, gc.contarTotalUnidades(CLIENTE_A));
        } finally {
            go.eliminar(orden.getId());
            gp.eliminar(idProd);
        }
    }

    @Test
    @Order(5)
    public void testProcesarCompraRechazaStockInsuficienteSinModificarInventario() {
        GestorProductos gp = new GestorProductos();
        GestorCarrito gc = new GestorCarrito();
        GestorOrdenes go = new GestorOrdenes();

        String idProdA = "951";
        String idProdB = "952";
        gp.eliminar(idProdA);
        gp.eliminar(idProdB);
        assertTrue(gp.agregar(new Producto(idProdA, "img/camisa_azul.png", "PROD OK", "CAMISAS", 5, 100000.0)));
        assertTrue(gp.agregar(new Producto(idProdB, "img/camisa_blanca.png", "PROD SIN STOCK", "CAMISAS", 2, 100000.0)));

        // El cliente agrega ambos al carrito y luego el admin reduce el stock del segundo a 1
        assertTrue(gc.agregarProducto(CLIENTE_B, idProdA, 2, 5));
        assertTrue(gc.agregarProducto(CLIENTE_B, idProdB, 2, 2));

        Producto reducido = gp.buscarPorId(idProdB);
        reducido.setStock(1);
        assertTrue(gp.actualizar(reducido));

        Usuario cliente = new Usuario("USR-F5B", "Cliente Stock", CLIENTE_B,
                "f5b@correo.com", "3002223344", "Cra 5 # 1-2", "hash", Usuario.ROL_CLIENTE);

        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> go.procesarCompra(cliente, "Cra 5 # 1-2", gc, gp));
            assertTrue(ex.getMessage().toLowerCase().contains("stock insuficiente"));

            // Ningún producto debió sufrir descuento parcial
            assertEquals(5, gp.buscarPorId(idProdA).getStock());
            assertEquals(1, gp.buscarPorId(idProdB).getStock());
        } finally {
            gp.eliminar(idProdA);
            gp.eliminar(idProdB);
        }
    }

    @Test
    @Order(6)
    public void testHistorialFiltradoPorClienteYFormatoResumen() {
        GestorOrdenes go = new GestorOrdenes();
        List<Orden> ordenesMaria = go.listarPorCliente("28483");
        assertTrue(ordenesMaria.size() >= 3, "María López debe tener las 3 órdenes semilla en su historial");
        assertTrue(ordenesMaria.stream().allMatch(o -> "28483".equals(o.getClienteIdentificacion())));

        Orden orden113 = go.buscarPorId("113");
        assertNotNull(orden113);
        assertEquals("ORDEN DE COMPRA", orden113.getTipo());
        assertEquals("Camisa Blanca, Bolso de Cuero.", orden113.getResumenProductos());

        assertTrue(go.listarPorCliente("00000000").isEmpty());
    }

    @Test
    @Order(7)
    public void testGuardiaDeRutasParaLasCuatroVistasDeCliente() {
        String[] vistasCliente = {"cliente_catalogo", "cliente_carrito", "cliente_deseados", "cliente_historial"};

        // Sin sesión: todas redirigen a login
        SesionActual.cerrarSesion();
        for (String vista : vistasCliente) {
            assertEquals("login", App.resolverVistaPermitida(vista),
                    "Sin sesión, " + vista + " debe redirigir a login");
        }

        // Con sesión de cliente: todas permitidas, pero admin_dashboard bloqueado
        Usuario cliente = new Usuario("USR-C5", "Ana", "55555", "ana@correo.com",
                "3000000000", "Dir 1", "hash", Usuario.ROL_CLIENTE);
        SesionActual.iniciarSesion(cliente);
        for (String vista : vistasCliente) {
            assertEquals(vista, App.resolverVistaPermitida(vista),
                    "Con sesión de cliente, " + vista + " debe estar permitida");
        }
        assertEquals("login", App.resolverVistaPermitida("admin_dashboard"));
    }

    @Test
    @Order(8)
    public void testAtributoSexoYFiltradoHombreMujerUnisex() {
        // 1. Retrocompatibilidad: constructor de 6 args o sexo nulo => UNISEX
        Producto sinSexo = new Producto("960", "img/placeholder_producto.png", "PRENDA BASE", "CAMISAS", 3, 90000.0);
        assertEquals(Producto.SEXO_UNISEX, sinSexo.getSexo());
        sinSexo.setSexo(null);
        assertEquals(Producto.SEXO_UNISEX, sinSexo.getSexo());

        // 2. Persistencia CRUD con sexo explícito y filtrado exacto (Admin) vs con Unisex (Cliente)
        GestorProductos gp = new GestorProductos();
        gp.eliminar("961");
        gp.eliminar("962");
        gp.eliminar("963");
        try {
            assertTrue(gp.agregar(new Producto("961", "img/camisa_azul.png", "TEST HOMBRE", "CAMISAS", Producto.SEXO_HOMBRE, 2, 100000.0)));
            assertTrue(gp.agregar(new Producto("962", "img/camisa_blanca.png", "TEST MUJER", "CAMISAS", Producto.SEXO_MUJER, 2, 100000.0)));
            assertTrue(gp.agregar(new Producto("963", "img/placeholder_producto.png", "TEST UNISEX", "CAMISAS", Producto.SEXO_UNISEX, 2, 100000.0)));

            assertEquals(Producto.SEXO_MUJER, gp.buscarPorId("962").getSexo());

            // Filtro exacto (Admin): HOMBRE no incluye UNISEX
            List<Producto> soloHombre = gp.filtrar("TEST", "CAMISAS", "TODOS", Producto.SEXO_HOMBRE);
            assertEquals(1, soloHombre.size());
            assertEquals("961", soloHombre.get(0).getId());

            List<Producto> soloMujer = gp.filtrar("TEST", "CAMISAS", "TODOS", Producto.SEXO_MUJER);
            assertEquals(1, soloMujer.size());
            assertEquals("962", soloMujer.get(0).getId());

            List<Producto> soloUnisex = gp.filtrar("TEST", "CAMISAS", "TODOS", Producto.SEXO_UNISEX);
            assertEquals(1, soloUnisex.size());
            assertEquals("963", soloUnisex.get(0).getId());

            // Filtro de catálogo cliente (incluirUnisex = true): MUJER incluye MUJER + UNISEX
            List<Producto> mujerConUnisex = gp.filtrar("TEST", "CAMISAS", "TODOS", Producto.SEXO_MUJER, true);
            assertEquals(2, mujerConUnisex.size());
            assertTrue(mujerConUnisex.stream().anyMatch(p -> "962".equals(p.getId())));
            assertTrue(mujerConUnisex.stream().anyMatch(p -> "963".equals(p.getId())));
        } finally {
            gp.eliminar("961");
            gp.eliminar("962");
            gp.eliminar("963");
        }
    }

    @Test
    @Order(9)
    public void testCancelarOrdenPendienteRestauraStockYRechazaNoPendientes() {
        GestorProductos gp = new GestorProductos();
        GestorCarrito gc = new GestorCarrito();
        GestorOrdenes go = new GestorOrdenes();

        String idProd = "970";
        gp.eliminar(idProd);
        assertTrue(gp.agregar(new Producto(idProd, "img/camisa_azul.png", "PROD CANCELAR", "CAMISAS", Producto.SEXO_UNISEX, 3, 120000.0)));

        Usuario cliente = new Usuario("USR-F6", "Cliente Cancelar", CLIENTE_A,
                "f6@correo.com", "3004445566", "Calle 20 # 5-10", "hash", Usuario.ROL_CLIENTE);
        assertTrue(gc.agregarProducto(CLIENTE_A, idProd, 2, 3));

        Orden orden = go.procesarCompra(cliente, "Calle 20 # 5-10", gc, gp);
        try {
            // Tras comprar, stock bajó de 3 a 1
            assertEquals(1, gp.buscarPorId(idProd).getStock());

            // Cancelar orden pendiente devuelve las 2 unidades => stock vuelve a 3
            Orden cancelada = go.cancelarOrdenPendiente(orden.getId(), CLIENTE_A, gp);
            assertEquals("Cancelado", cancelada.getEstado());
            assertEquals(3, gp.buscarPorId(idProd).getStock());

            // Intentar cancelar de nuevo una orden ya cancelada debe fallar
            assertThrows(IllegalStateException.class,
                    () -> go.cancelarOrdenPendiente(orden.getId(), CLIENTE_A, gp));

            // Intentar cancelar una orden Entregada (#112) debe fallar
            assertThrows(IllegalStateException.class,
                    () -> go.cancelarOrdenPendiente("112", "28483", gp));
        } finally {
            go.eliminar(orden.getId());
            gp.eliminar(idProd);
        }
    }

    @Test
    @Order(10)
    public void testGenerarYGuardarComprobanteTxt() throws Exception {
        GestorOrdenes go = new GestorOrdenes();
        GestorUsuarios gu = new GestorUsuarios();
        Orden orden = go.buscarPorId("113");
        Usuario maria = gu.buscarPorIdentificacion("28483");
        assertNotNull(orden);
        assertNotNull(maria);

        File archivo = GeneradorComprobante.guardarEnRecibos(orden, maria);
        try {
            assertTrue(archivo.isFile(), "El archivo comprobante .txt debe haberse creado");
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            assertTrue(contenido.contains("BELLE STORE - COMPROBANTE"));
            assertTrue(contenido.contains("#113"));
            assertTrue(contenido.contains("María López"));
            assertTrue(contenido.contains("TOTAL PAGADO:"));
        } finally {
            archivo.delete();
        }
    }

    @Test
    @Order(11)
    public void testActualizarContactoUsuarioEnPerfil() {
        GestorUsuarios gu = new GestorUsuarios();
        Usuario maria = gu.buscarPorIdentificacion("28483");
        assertNotNull(maria);
        String celOriginal = maria.getCelular();
        String dirOriginal = maria.getDireccion();

        try {
            assertTrue(gu.actualizarContacto(maria.getId(), "3112223344", "Av Siempre Viva 742"));
            Usuario recargado = gu.buscarPorIdentificacion("28483");
            assertEquals("3112223344", recargado.getCelular());
            assertEquals("Av Siempre Viva 742", recargado.getDireccion());
        } finally {
            gu.actualizarContacto(maria.getId(), celOriginal, dirOriginal);
        }
    }

    @Test
    @Order(12)
    public void testTotalConImpuestosYEnvioParaOrdenesAntiguas() {
        // Orden semilla #111: subtotal $200.000 COP (sin impuestos persistidos) => envío $10.000 + IVA 19% ($38.000) = $248.000
        GestorOrdenes go = new GestorOrdenes();
        Orden orden111 = go.buscarPorId("111");
        assertNotNull(orden111);
        assertEquals(200000.0, orden111.getSubtotalArticulos());
        assertEquals(10000.0, orden111.getCostoEnvioEfectivo());
        assertEquals(38000.0, orden111.getImpuestosEfectivos());
        assertEquals(248000.0, orden111.getTotalConImpuestosYEnvio());

        // Orden semilla #113: subtotal $300.000 COP => envío gratis ($0) + IVA 19% ($57.000) = $357.000
        Orden orden113 = go.buscarPorId("113");
        assertNotNull(orden113);
        assertEquals(0.0, orden113.getCostoEnvioEfectivo());
        assertEquals(57000.0, orden113.getImpuestosEfectivos());
        assertEquals(357000.0, orden113.getTotalConImpuestosYEnvio());
    }
}
