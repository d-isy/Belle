package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;
import com.mycompany.belle_store.util.Formato;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las mejoras posteriores a la auditoría (paso 1):
 * formato monetario, orden numérico de IDs y generación de IDs de usuario sin repetición.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormatoYUsuariosTest {

    private static final String[] CORREOS = {
        "id.prueba1@correo.com", "id.prueba2@correo.com", "id.prueba3@correo.com", "id.prueba4@correo.com"
    };

    @BeforeAll
    public static void setUp() {
        new SeedDatos().inicializarSiEsNecesario();
        limpiarUsuariosDePrueba();
    }

    @AfterAll
    public static void limpiar() {
        limpiarUsuariosDePrueba();
    }

    private static void limpiarUsuariosDePrueba() {
        GestorUsuarios g = new GestorUsuarios();
        for (String correo : CORREOS) {
            Usuario u = g.buscarPorCorreo(correo);
            if (u != null) {
                g.eliminar(u.getId());
            }
        }
    }

    @Test
    @Order(1)
    public void testFormatoMonedaColombiano() {
        assertEquals("$200.000 COP", Formato.moneda(200000.0));
        assertEquals("$1.000 COP", Formato.moneda(1000));
        assertEquals("$0 COP", Formato.moneda(0));
        assertEquals("$1.250.000 COP", Formato.moneda(1250000.9), "Se trunca a pesos enteros");
        // Los modelos delegan en la misma utilidad
        assertEquals("$180.000 COP", new Producto("1", "img/x.png", "X", "CAMISAS", 1, 180000.0).getPrecioFormateado());
    }

    @Test
    @Order(2)
    public void testComparadorNumericoDeIds() {
        assertTrue(Formato.compararNumericoOTexto("234", "1000") < 0, "234 va antes que 1000 (no alfabético)");
        assertTrue(Formato.compararNumericoOTexto("1000", "234") > 0);
        assertEquals(0, Formato.compararNumericoOTexto("111", "111"));
        assertTrue(Formato.compararNumericoOTexto("TEST-1", "TEST-2") < 0, "Texto: orden alfabético");
        assertTrue(Formato.compararNumericoOTexto("abc", "ABD") < 0, "Sin distinguir mayúsculas");
        assertTrue(Formato.compararNumericoOTexto(null, "1") < 0);
        assertEquals(0, Formato.compararNumericoOTexto(null, null));

        List<String> ids = new ArrayList<>(List.of("1000", "234", "99", "111"));
        ids.sort(Formato::compararNumericoOTexto);
        assertEquals(List.of("99", "111", "234", "1000"), ids);
    }

    @Test
    @Order(3)
    public void testIdsDeUsuarioNoSeRepitenTrasEliminar() {
        GestorUsuarios g = new GestorUsuarios();
        Set<String> idsVistos = new HashSet<>();
        for (Usuario existente : g.listar()) {
            idsVistos.add(existente.getId());
        }

        for (int i = 0; i < 3; i++) {
            assertTrue(g.guardar(nuevoUsuario(i)), "Debe guardar el usuario " + i);
        }
        Usuario segundo = g.buscarPorCorreo(CORREOS[1]);
        assertNotNull(segundo);

        // Se elimina un usuario "del medio" y se registra otro: el ID nuevo no puede repetir ninguno existente
        assertTrue(g.eliminar(segundo.getId()));
        assertTrue(g.guardar(nuevoUsuario(3)));

        List<String> ids = new ArrayList<>();
        for (Usuario u : g.listar()) {
            ids.add(u.getId());
        }
        assertEquals(new HashSet<>(ids).size(), ids.size(), "No debe haber IDs de usuario repetidos: " + ids);

        Usuario cuarto = g.buscarPorCorreo(CORREOS[3]);
        assertNotNull(cuarto);
        assertTrue(cuarto.getId().matches("USR-\\d+"), "Formato USR-n: " + cuarto.getId());
        assertNotEquals(g.buscarPorCorreo(CORREOS[2]).getId(), cuarto.getId());
    }

    private static Usuario nuevoUsuario(int indice) {
        return new Usuario(null, "Usuario Prueba", "910000" + indice, CORREOS[indice],
                "300000010" + indice, "Calle " + indice, "clave1234", Usuario.ROL_CLIENTE);
    }
}
