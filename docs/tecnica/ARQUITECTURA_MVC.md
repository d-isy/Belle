# Arquitectura de Software y Patrón MVC - Belle Store

Este documento técnico sirve como material de estudio didáctico para comprender la organización del código, la separación de responsabilidades y el flujo de datos en la aplicación de escritorio **Belle Store**.

---

## 1. Visión General de la Arquitectura
La aplicación sigue una variante práctica del patrón **MVC (Modelo - Vista - Controlador)** adaptada al ecosistema JavaFX:

```
                  +-----------------------------------+
                  |         VISTA (FXML + CSS)        |
                  | Interfaz gráfica declarativa XML  |
                  +-----------------------------------+
                                   |   ^
                    Eventos de UI  |   | Actualizaciones
                       (onAction)  v   | de componentes (@FXML)
                  +-----------------------------------+
                  |            CONTROLADOR            |
                  |     Lógica de vista y eventos     |
                  +-----------------------------------+
                       |                         |
       Consulta estado |                         | Consulta y
          y sesión     v                         v modificación
       +--------------------+          +--------------------+
       |    SesionActual    |          |    PERSISTENCIA    |
       |  (Estado en RAM)   |          | (Gestores + Gson)  |
       +--------------------+          +--------------------+
                                                 |
                                                 v
                                       +--------------------+
                                       |      MODELO        |
                                       | Clases de Dominio  |
                                       |   (Archivos JSON)  |
                                       +--------------------+
```

---

## 2. Organización de Paquetes
Dentro de la raíz del paquete `com.mycompany.belle_store` encontramos:

| Paquete | Responsabilidad | Ejemplo de Clases |
| :--- | :--- | :--- |
| `com.mycompany.belle_store` | Punto de entrada del ciclo de vida JavaFX y navegación global de escenas. | `App.java` |
| `com.mycompany.belle_store.modelos` | Clases POJO simples que representan los datos del negocio (sin lógica de UI). | `Usuario.java`, `Producto.java`, `Orden.java`, `ItemOrden.java` |
| `com.mycompany.belle_store.persistencia` | Lectura, escritura y filtrado de datos contra archivos JSON mediante Google Gson. | `ConfiguracionRutas.java`, `GestorUsuarios.java`, `GestorProductos.java`, `GestorOrdenes.java`, `SeedDatos.java` |
| `com.mycompany.belle_store.sesion` | Almacenamiento en memoria de la identidad del usuario actualmente autenticado. | `SesionActual.java` |
| `com.mycompany.belle_store.controladores` | Controladores de interfaz que enlazan los elementos visuales declarados en los archivos FXML con las operaciones del sistema. *(Se implementarán en las Fases 2, 3 y 4)* | `LoginController`, `RegistroController`, `AdminDashboardController`, `ClientePlaceholderController` |

---

## 3. Estructura Estándar de Secciones de Código
Para asegurar la máxima legibilidad didáctica, todas las clases controladoras y clases principales respetan estrictamente cinco secciones delimitadas:

```java
// =========================================================================
// 1. COMPONENTES FXML (@FXML)
// Elementos visuales inyectados desde el archivo .fxml (botones, tablas, cajas de texto).
// =========================================================================

// =========================================================================
// 2. ATRIBUTOS, SERVICIOS Y ESTADO
// Referencias a los gestores de persistencia, colecciones observables y variables de control.
// =========================================================================

// =========================================================================
// 3. INICIALIZACIÓN (initialize y configuración inicial)
// Métodos ejecutados al cargar la interfaz (configuración de columnas, listeners de eventos).
// =========================================================================

// =========================================================================
// 4. EVENTOS DE INTERFAZ Y NAVEGACIÓN (Acciones de botones y filtros)
// Métodos vinculados a los eventos onAction de los botones o selecciones de menús.
// =========================================================================

// =========================================================================
// 5. LÓGICA DE NEGOCIO Y MÉTODOS AUXILIARES (Validaciones, carga de tablas)
// Métodos privados para limpiar formularios, validar reglas de campos y refrescar datos.
// =========================================================================
```

---

## 4. Gestión de Sesión (`SesionActual`)
En aplicaciones de escritorio, es común necesitar saber quién está utilizando el sistema desde cualquier ventana. `SesionActual` proporciona un mecanismo estático en memoria:
* **Iniciar sesión:** `SesionActual.iniciarSesion(usuario)` guarda la referencia del usuario autenticado.
* **Comprobar rol:** `SesionActual.esAdmin()` o `SesionActual.esCliente()` permite condicionar qué ventanas o acciones están permitidas.
* **Cerrar sesión:** `SesionActual.cerrarSesion()` borra la referencia en memoria y permite retornar con seguridad al Login.
