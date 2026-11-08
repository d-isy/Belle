package com.mycompany.belle_store.persistencia;

import com.mycompany.belle_store.modelos.ItemOrden;
import com.mycompany.belle_store.modelos.Orden;
import com.mycompany.belle_store.modelos.Producto;
import com.mycompany.belle_store.modelos.Usuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Clase encargada de sembrar los datos iniciales (Seed) en los archivos JSON
 * para que el proyecto pueda ser ejecutado y probado de inmediato con información realista
 * y coincidente con los diseños de Figma.
 */
public class SeedDatos {

    // =========================================================================
    // 1. COMPONENTES FXML (@FXML) - No aplica
    // =========================================================================

    // =========================================================================
    // 2. ATRIBUTOS, SERVICIOS Y ESTADO
    // =========================================================================
    private final GestorUsuarios gestorUsuarios;
    private final GestorProductos gestorProductos;
    private final GestorOrdenes gestorOrdenes;

    // =========================================================================
    // 3. INICIALIZACIÓN (Constructor)
    // =========================================================================
    public SeedDatos() {
        this.gestorUsuarios = new GestorUsuarios();
        this.gestorProductos = new GestorProductos();
        this.gestorOrdenes = new GestorOrdenes();
    }

    // =========================================================================
    // 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN - No aplica
    // =========================================================================

    // =========================================================================
    // 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Inicializa todos los archivos JSON con datos de prueba si están vacíos.
     */
    public void inicializarSiEsNecesario() {
        inicializarUsuarios();
        inicializarProductos();
        inicializarOrdenes();
    }

    private void inicializarUsuarios() {
        List<Usuario> usuarios = gestorUsuarios.listar();
        if (usuarios.isEmpty()) {
            List<Usuario> seed = new ArrayList<>();

            // 1. Administrador (acceso con 'admin' o con su correo 'admin@belle.com')
            seed.add(new Usuario(
                    "USR-ADMIN",
                    "Administrador Belle",
                    "1000000001",
                    "admin@belle.com",
                    "3000000000",
                    "Oficina Principal Belle",
                    "admin123",
                    Usuario.ROL_ADMIN
            ));

            // 2. Cliente de prueba (ID 28483 y Dirección Cll 1 # 2 presentes en Figma)
            seed.add(new Usuario(
                    "USR-001",
                    "María López",
                    "28483",
                    "maria@correo.com",
                    "3001234567",
                    "Cll 1 # 2",
                    "cliente123",
                    Usuario.ROL_CLIENTE
            ));

            gestorUsuarios.guardarTodos(seed);
            System.out.println("[SeedDatos] Usuarios.json inicializado exitosamente.");
        }
    }

    private void inicializarProductos() {
        List<Producto> productos = gestorProductos.listar();
        if (productos.isEmpty()) {
            List<Producto> seed = new ArrayList<>();

            // Producto 1: Presente textualmente en el Figma de Administrador
            seed.add(new Producto(
                    "234",
                    "img/camisa_azul.png",
                    "CAMISA AZUL",
                    "CAMISAS",
                    2,
                    "DISPONIBLE",
                    200000.0
            ));

            // Producto 2: Con URL externa para comprobar el soporte de imágenes remotas
            seed.add(new Producto(
                    "235",
                    "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=200",
                    "PANTALÓN NEGRO",
                    "PANTALONES",
                    5,
                    "DISPONIBLE",
                    150000.0
            ));

            // Producto 3: Producto agotado de la categoría Accesorios
            seed.add(new Producto(
                    "236",
                    "img/bolso_cuero.png",
                    "BOLSO DE CUERO",
                    "ACCESORIOS",
                    0,
                    "AGOTADO",
                    120000.0
            ));

            // Producto 4: Camisa Blanca
            seed.add(new Producto(
                    "237",
                    "img/camisa_blanca.png",
                    "CAMISA BLANCA",
                    "CAMISAS",
                    4,
                    "DISPONIBLE",
                    180000.0
            ));

            gestorProductos.guardarTodos(seed);
            System.out.println("[SeedDatos] Productos.json inicializado exitosamente.");
        }
    }

    private void inicializarOrdenes() {
        List<Orden> ordenes = gestorOrdenes.listar();
        if (ordenes.isEmpty()) {
            List<Orden> seed = new ArrayList<>();

            // Orden 1: Presente en el Figma de Admin Ordenes (ID 111, Cliente 28483, Camisa - 1, $200.000 COP)
            List<ItemOrden> articulos1 = new ArrayList<>();
            articulos1.add(new ItemOrden("234", "Camisa", 1, 200000.0));

            seed.add(new Orden(
                    "111",
                    "28483",
                    articulos1,
                    "Cll 1 # 2",
                    "No Entregado",
                    200000.0,
                    "05/10/2026 05:31:01"
            ));

            // Orden 2: Orden entregada con otro producto
            List<ItemOrden> articulos2 = new ArrayList<>();
            articulos2.add(new ItemOrden("235", "Pantalón", 1, 150000.0));

            seed.add(new Orden(
                    "112",
                    "28483",
                    articulos2,
                    "Cll 1 # 2",
                    "Entregado",
                    150000.0,
                    "06/10/2026 10:15:30"
            ));

            gestorOrdenes.guardarTodos(seed);
            System.out.println("[SeedDatos] Ordenes.json inicializado exitosamente.");
        }
    }
}
