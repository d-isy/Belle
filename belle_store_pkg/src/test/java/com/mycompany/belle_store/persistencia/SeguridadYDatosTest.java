package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.seguridad.HashContrasena;
import com.mycompany.belle_store.validacion.Validaciones;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la auditoría: aislamiento de datos, hash de contraseñas, imágenes y validaciones.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeguridadYDatosTest {

    @BeforeAll
    public static void setUp() {
        new SeedDatos().inicializarSiEsNecesario();
    }

    @Test
    @Order(1)
    public void testLasPruebasNoTocanLosDatosReales() {
        String ruta = ConfiguracionRutas.obtenerDirectorioDatos().getPath().replace('\\', '/');
        assertTrue(ruta.contains("datos-pruebas"),
                "Las pruebas deben usar la carpeta temporal y no 'datos/' real. Ruta: " + ruta);
    }

    @Test
    @Order(2)
    public void testHashConSaltConocidoEsReproducible() {
        byte[] salt = new byte[16]; // salt de ceros: solo para la prueba
        String h1 = HashContrasena.generarHash("admin123", salt, HashContrasena.ITERACIONES);
        String h2 = HashContrasena.generarHash("admin123", salt, HashContrasena.ITERACIONES);
        assertEquals(h1, h2, "Mismo salt + misma clave => mismo hash");
        assertTrue(h1.startsWith("pbkdf2$65536$" + Base64.getEncoder().encodeToString(salt) + "$"));
        assertTrue(HashContrasena.verificar("admin123", h1));
        assertFalse(HashContrasena.verificar("admin124", h1));
    }

    @Test
    @Order(3)
    public void testSaltAleatorioProduceHashesDistintos() {
        String a = HashContrasena.generarHash("clave");
        String b = HashContrasena.generarHash("clave");
        assertNotEquals(a, b, "Cada hash debe usar un salt aleatorio distinto");
        assertTrue(HashContrasena.esHash(a));
        assertFalse(HashContrasena.esHash("clave"));
        assertFalse(HashContrasena.verificar("clave", "pbkdf2$basura"));
    }

    @Test
    @Order(4)
    public void testUsuariosDelSeedSeGuardanConHash() {
        GestorUsuarios g = new GestorUsuarios();
        for (Usuario u : g.listar()) {
            assertTrue(HashContrasena.esHash(u.getContrasena()),
                    "La contraseña de " + u.getCorreo() + " debe estar hasheada");
        }
        assertNotNull(g.autenticar("admin", "admin123"));
        assertNull(g.autenticar("admin", "admin123 "), "Los espacios finales ya no se recortan");
    }

    @Test
    @Order(5)
    public void testRegistroHasheaYMigraContrasenaEnTextoPlano() {
        GestorUsuarios g = new GestorUsuarios();
        Usuario u = new Usuario(null, "Usuario Hash", "5550001", "hash.test@correo.com",
                "3000000099", "Calle 1 # 1", "secreta1", Usuario.ROL_CLIENTE);
        if (g.existeCorreo("hash.test@correo.com")) {
            g.eliminar(g.buscarPorCorreo("hash.test@correo.com").getId());
        }
        assertTrue(g.guardar(u));
        assertTrue(HashContrasena.esHash(g.buscarPorCorreo("hash.test@correo.com").getContrasena()));
        assertNotNull(g.autenticar("hash.test@correo.com", "secreta1"));

        // Usuario añadido "a mano" con clave en texto plano: se acepta y se migra a hash
        List<Usuario> lista = g.listar();
        Usuario manual = new Usuario("USR-MANUAL", "Manual", "5550002", "manual.test@correo.com",
                "3000000098", "Calle 2", "plano123", Usuario.ROL_CLIENTE);
        lista.add(manual);
        g.guardarTodos(lista);
        assertNotNull(g.autenticar("manual.test@correo.com", "plano123"));
        assertTrue(HashContrasena.esHash(g.buscarPorCorreo("manual.test@correo.com").getContrasena()),
                "Tras el primer login la clave en texto plano debe quedar hasheada");
        assertNotNull(g.autenticar("manual.test@correo.com", "plano123"), "Debe seguir funcionando con el hash");

        g.eliminar(g.buscarPorCorreo("hash.test@correo.com").getId());
        g.eliminar("USR-MANUAL");
    }

    @Test
    @Order(6)
    public void testImportarYResolverImagenes(@TempDir Path temporal) throws Exception {
        // Recurso del proyecto (imagen de Figma) y genérica
        assertNotNull(GestorImagenes.resolverUrl("img/camisa_azul.png"));
        assertNotNull(GestorImagenes.resolverUrl(GestorImagenes.PLACEHOLDER));
        assertNull(GestorImagenes.resolverUrl("img/no_existe.png"));
        assertNotNull(GestorImagenes.resolverUrlOPlaceholder("img/no_existe.png"), "Debe caer en la imagen genérica");
        assertEquals("https://ejemplo.com/a.png", GestorImagenes.resolverUrl("https://ejemplo.com/a.png"));

        // Limpiar para asegurar idempotencia si se corre consecutivamente sin 'mvn clean'
        File destinoBase = new File(ConfiguracionRutas.obtenerDirectorioImagenes(), "mi_foto.png");
        if (destinoBase.exists()) {
            destinoBase.delete();
        }

        // Importar una imagen del equipo
        File origen = Files.write(temporal.resolve("mi foto.png"), new byte[]{1, 2, 3}).toFile();
        String ref1 = GestorImagenes.importarImagen(origen);
        String ref2 = GestorImagenes.importarImagen(origen);
        assertEquals("imagenes/mi_foto.png", ref1);
        assertNotEquals(ref1, ref2, "No debe sobrescribir una imagen ya importada");
        assertNotNull(GestorImagenes.resolverUrl(ref1));

        // Rechaza archivos que no son imagen
        File texto = Files.write(temporal.resolve("notas.txt"), new byte[]{1}).toFile();
        assertThrows(java.io.IOException.class, () -> GestorImagenes.importarImagen(texto));
    }

    @Test
    @Order(7)
    public void testValidacionDeProductoYEstadoPorStock() {
        assertNull(Validaciones.validarProducto("Camisa roja", "5", "90000"));
        assertNotNull(Validaciones.validarProducto("Ab", "5", "90000"), "Nombre muy corto");
        assertNotNull(Validaciones.validarProducto("Camisa roja", "-1", "90000"), "Stock negativo");
        assertNotNull(Validaciones.validarProducto("Camisa roja", "abc", "90000"), "Stock no numérico");
        assertNotNull(Validaciones.validarProducto("Camisa roja", "5", "0"), "Precio cero");
        assertNotNull(Validaciones.validarProducto("Camisa roja", "5", "-10"), "Precio negativo");
        assertNotNull(Validaciones.validarProducto("Camisa roja", "5", "x"), "Precio no numérico");
        assertEquals("AGOTADO", Validaciones.estadoPorStock(0));
        assertEquals("DISPONIBLE", Validaciones.estadoPorStock(3));
    }

    @Test
    @Order(8)
    public void testNoHayOrdenesHuerfanasDeProductosDelSeed() {
        GestorProductos gp = new GestorProductos();
        GestorOrdenes go = new GestorOrdenes();
        for (Orden o : go.listar()) {
            for (ItemOrden i : o.getArticulos()) {
                assertNotNull(gp.buscarPorId(i.getProductoId()),
                        "La orden " + o.getId() + " referencia un producto inexistente: " + i.getProductoId());
            }
        }
        List<ItemOrden> items = new ArrayList<>();
        assertTrue(items.isEmpty());
    }
}
