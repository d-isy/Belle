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

### Flujo de Lectura y Escritura (`GestorJson<T>`)
Los tres gestores (`GestorUsuarios`, `GestorProductos`, `GestorOrdenes`) heredan de la clase genérica `GestorJson<T>`, que centraliza el acceso a disco:
1. **Escritura atómica (Serialización):**
   Para evitar que un fallo a mitad de escritura deje un archivo JSON truncado o vacío, `guardarTodos` escribe primero en un archivo temporal (`*.json.tmp`) en la misma carpeta y luego reemplaza el archivo definitivo con `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
2. **Lectura (Deserialización de listas genéricas con TypeToken):**
   Como Java borra la información de tipos genéricos en tiempo de ejecución (Type Erasure), cada gestor pasa su `TypeToken` a `GestorJson<T>` para reconstruir `List<Entidad>`:
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
| `contrasena` | `String` | **Hash PBKDF2** de la clave (`pbkdf2$iteraciones$salt$hash`). Nunca se guarda en texto plano. Ver sección *Hash de contraseñas*. |
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
| `estado` *(derivado)* | `String` | No se persiste en JSON: `getEstado()` lo calcula siempre desde `stock` (`0 => "AGOTADO"`, `> 0 => "DISPONIBLE"`). |
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
| `clienteIdentificacion` | `String` | Número de identificación (cédula) del cliente (ej. `"28483"`). Anotado con `@SerializedName(value = "clienteIdentificacion", alternate = {"clienteId"})` para leer JSON antiguos sin romperse. |
| `articulos` | `List<ItemOrden>` | Lista de artículos pertenecientes al pedido (única fuente de verdad para totales). |
| `totalArticulos` *(derivado)* | `int` | No se persiste en JSON: `getTotalArticulos()` suma las cantidades de `articulos`. |
| `direccion` | `String` | Dirección de entrega (ej. `"Cll 1 # 2"`). |
| `estado` | `String` | Estado logístico (ej. `"No Entregado"`, `"Entregado"`, `"Pendiente"`). |
| `valorTotal` *(derivado)* | `double` | No se persiste en JSON: `getValorTotal()` suma `cantidad * precioUnitario` de `articulos`. |
| `fecha` | `String` | Fecha y hora de creación (ej. `"05/10/2026 05:31:01"`). |

* **Método auxiliar `getArticulosFormateados()`:** Genera una lista con viñetas multilínea para la celda de la tabla (`"• Camisa - 1"`).
* **Método auxiliar `getValorTotalFormateado()`:** Retorna el valor en formato `"$200.000 COP"`.


---

## 3. Hash de contraseñas (`seguridad/HashContrasena.java`)

Las contraseñas se almacenan con **PBKDF2** (incluido en el JDK, sin dependencias externas).

**Formato del campo `contrasena` en `Usuarios.json`:**

```
pbkdf2$ITERACIONES$SALT_BASE64$HASH_BASE64
pbkdf2$65536$e+2+Cz6KQIyqQF1vWP9HtQ==$rSlFLqVI+hMPLIJ6uETEFUqZuNKFY4P/hBf3RvxwLAc=
```

| Parámetro | Valor |
| :--- | :--- |
| Algoritmo | `PBKDF2WithHmacSHA256` |
| Iteraciones | `65536` (se leen del propio texto almacenado, por lo que pueden cambiar en el futuro sin romper hashes viejos) |
| Salt | 16 bytes aleatorios (`SecureRandom`), Base64 estándar |
| Hash | 32 bytes (256 bits), Base64 estándar |

**¿Para qué sirve el salt?** Es un valor aleatorio distinto por usuario que se mezcla con la clave antes de calcular el hash. Dos usuarios con la misma clave obtienen hashes diferentes, y se impiden los ataques con tablas precalculadas. El salt **no es secreto**: se guarda junto al hash.

**Generar un hash manualmente** (para añadir un usuario a mano en `Usuarios.json`), desde `belle_store_pkg/` tras compilar:

```bash
java -cp target/classes com.mycompany.belle_store.seguridad.HashContrasena MiClave123
```

La herramienta imprime la línea completa lista para pegar y sus partes (algoritmo, iteraciones, salt y hash).

**Verificación:** `HashContrasena.verificar(claveIngresada, almacenado)` recalcula PBKDF2 con el salt e iteraciones almacenados y compara en tiempo constante (`MessageDigest.isEqual`).

**Migración transparente:** si en `Usuarios.json` se escribe una clave en texto plano a mano, el primer inicio de sesión correcto la acepta y la reemplaza por su hash.

---

## 4. Ubicación de los datos y imágenes

* Los `.json` se guardan en `belle_store_pkg/datos/` (ignorado por Git; `SeedDatos` los genera al arrancar si no existen).
* Se puede cambiar con `-Dbelle.datos=/ruta` (las pruebas usan `target/datos-pruebas`, por lo que **nunca modifican los datos reales**).
* El campo `imagen` de un producto admite: `img/archivo.png` (recurso del proyecto), `imagenes/archivo.png` (copiada por el administrador a `datos/imagenes/` con el botón *Examinar...*) o una URL `https://...`.
