# Persistencia en JSON y Diccionario de Modelos - Belle Store

Este documento explica didácticamente cómo funciona el almacenamiento de datos en archivos JSON utilizando la biblioteca **Google Gson**, así como el diccionario de datos de las entidades del dominio.

---

## 1. ¿Cómo funciona la Persistencia con Google Gson?
**Gson** es una biblioteca desarrollada por Google para convertir objetos Java en su representación textual en formato JSON (serialización) y viceversa (deserialización).

### Ventajas en el contexto pedagógico
* **Sin necesidad de instalar motores de bases de datos:** No requiere configurar MySQL, PostgreSQL ni drivers JDBC complejos.
* **Inspección directa:** Cualquier estudiante puede abrir la carpeta `datos/` y ver los archivos `.json` en cualquier editor de texto.
* **Formato legible (Pretty Printing):** Los gestores configuran Gson mediante:
  ```java
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  ```
  Esto asegura que el archivo JSON tenga saltos de línea e identaciones limpias, en lugar de una sola línea comprimida.

### Flujo de Lectura y Escritura
1. **Escritura (Serialización):**
   ```java
   try (Writer writer = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
       gson.toJson(lista, writer);
   }
   ```
2. **Lectura (Deserialización de listas genéricas con TypeToken):**
   Como Java borra la información de tipos genéricos en tiempo de ejecución (Type Erasure), Gson requiere `TypeToken` para reconstruir una `List<Entidad>`:
   ```java
   Type tipoLista = new TypeToken<List<Usuario>>() {}.getType();
   List<Usuario> usuarios = gson.fromJson(reader, tipoLista);
   ```

---

## 2. Diccionario de Datos de los Modelos

### 2.1 Modelo `Usuario`
Representa a los usuarios del sistema (tanto Administradores como Clientes registrados). Se persiste en `Usuarios.json`.

| Atributo | Tipo de Dato | Descripción y Reglas de Negocio |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único pedagógico (ej. `"USR-ADMIN"`, `"USR-001"`). |
| `nombreCompleto` | `String` | Nombre y apellido del usuario (ej. `"María López"`). |
| `numeroIdentificacion` | `String` | Cédula o documento de identidad (ej. `"28483"`). Debe ser único en el registro. |
| `correo` | `String` | Correo electrónico del usuario (ej. `"maria@correo.com"`). **Se utiliza como el identificador de inicio de sesión.** |
| `celular` | `String` | Número de teléfono de contacto (ej. `"3001234567"`). |
| `direccion` | `String` | Dirección física de residencia o despacho (ej. `"Cll 1 # 2"`). |
| `contrasena` | `String` | Clave secreta en texto plano (enfoque didáctico para esta etapa). |
| `rol` | `String` | Rol de permisos: `"ADMIN"` o `"CLIENTE"`. |

#### Regla de Autenticación
En `GestorUsuarios.autenticar(identificador, contrasena)`:
* Si el `identificador` es `"admin"`, se valida contra la contraseña del Administrador.
* Si el `identificador` es un correo registrado, se valida contra la contraseña de dicho usuario.
* Cualquier otro identificador (como cédula o nombre) es rechazado.

---

### 2.2 Modelo `Producto`
Representa los artículos comercializados por la tienda. Se persiste en `Productos.json`.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Código identificador del producto visible en Figma (ej. `"234"`). |
| `imagen` | `String` | Ruta relativa a una imagen local (ej. `"img/camisa_azul.png"`) o una URL remota (`"https://..."`). |
| `nombre` | `String` | Nombre comercial del artículo (ej. `"CAMISA AZUL"`). |
| `categoria` | `String` | Categoría textil (ej. `"CAMISAS"`, `"PANTALONES"`, `"ACCESORIOS"`). |
| `stock` | `int` | Unidades físicas disponibles en inventario (ej. `2`). |
| `estado` | `String` | Disponibilidad para venta (ej. `"DISPONIBLE"`, `"AGOTADO"`). |
| `precio` | `double` | Valor unitario en pesos colombianos (ej. `200000.0`). |

* **Método auxiliar `getPrecioFormateado()`:** Retorna la cadena monetaria estándar de Figma: `"$200.000 COP"`.

---

### 2.3 Modelo `ItemOrden`
Representa una línea de detalle dentro de una orden de compra.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `productoId` | `String` | Identificador del producto comprado. |
| `nombreProducto` | `String` | Nombre del producto. |
| `cantidad` | `int` | Cantidad de unidades adquiridas. |
| `precioUnitario` | `double` | Precio por unidad al momento de la compra. |

* **Método auxiliar `getDescripcionFormateada()`:** Retorna la cadena visible en la tabla de Figma: `"Camisa - 1"` (Nombre y cantidad).

---

### 2.4 Modelo `Orden`
Representa un pedido o compra realizada por un cliente. Se persiste en `Ordenes.json`.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Número de orden visible en Figma (ej. `"111"`). |
| `clienteId` | `String` | Identificación del cliente que compró (ej. `"28483"`). |
| `articulos` | `List<ItemOrden>` | Lista de artículos pertenecientes al pedido. |
| `totalArticulos` | `int` | Suma de unidades de todos los artículos de la orden. |
| `direccion` | `String` | Dirección de entrega (ej. `"Cll 1 # 2"`). |
| `estado` | `String` | Estado logístico (ej. `"No Entregado"`, `"Entregado"`, `"Pendiente"`). |
| `valorTotal` | `double` | Importe total liquidado de la orden (ej. `200000.0`). |
| `fecha` | `String` | Fecha y hora de creación (ej. `"05/10/2026 05:31:01"`). |

* **Método auxiliar `getArticulosFormateados()`:** Genera una lista con viñetas multilínea para la celda de la tabla (`"• Camisa - 1"`).
* **Método auxiliar `getValorTotalFormateado()`:** Retorna el valor en formato `"$200.000 COP"`.
