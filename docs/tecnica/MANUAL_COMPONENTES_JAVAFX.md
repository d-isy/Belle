# Manual Pedagógico de Componentes JavaFX - Belle Store

Este manual didáctico explica el funcionamiento, configuración y mejores prácticas de los componentes clave de JavaFX utilizados a lo largo de las distintas fases del proyecto **Belle Store**.

---

## 1. Renderizado de Miniaturas en `TableView` (`CellFactory` con `ImageView`)

En la tabla del Dashboard de Administrador (columna **IMAGEN**), no mostramos el texto plano de la ruta, sino una imagen gráfica real reducida (thumbnail) con dimensiones uniformes.

### ¿Cómo funciona un `CellFactory` en JavaFX?
Por defecto, las celdas de una tabla en JavaFX usan una `TableCell` básica que renderiza el método `toString()` del objeto. Cuando queremos que una celda dibuje un componente visual personalizado (como un `ImageView`, un botón o un icono), debemos definir una "fábrica de celdas" (`CellFactory`):

```java
columnaImagen.setCellFactory(col -> new TableCell<Producto, String>() {
    private final ImageView imageView = new ImageView();

    {
        // 1. Configurar dimensiones fijas y aspect ratio
        imageView.setFitWidth(45);
        imageView.setFitHeight(45);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
    }

    @Override
    protected void updateItem(String rutaOUrl, boolean empty) {
        super.updateItem(rutaOUrl, empty);

        // Si la celda está vacía o el valor es nulo, no mostrar nada
        if (empty || rutaOUrl == null || rutaOUrl.trim().isEmpty()) {
            setGraphic(null);
            setText(null);
            return;
        }

        try {
            Image imagen;
            // 2. Soporte dual: URL remota (http/https) o recurso local del proyecto
            if (rutaOUrl.startsWith("http://") || rutaOUrl.startsWith("https://")) {
                // backgroundLoading = true para no congelar la interfaz al descargar
                imagen = new Image(rutaOUrl, 45, 45, true, true, true);
            } else {
                // Carga desde los recursos del classpath
                InputStream is = getClass().getResourceAsStream("/com/mycompany/belle_store/" + rutaOUrl);
                if (is != null) {
                    imagen = new Image(is, 45, 45, true, true);
                } else {
                    // Fallback: Si no se encuentra el recurso local, usar placeholder
                    imagen = obtenerImagenPlaceholder();
                }
            }

            imageView.setImage(imagen);
            setGraphic(imageView);
            setText(null);
        } catch (Exception e) {
            // Manejo defensivo ante fallos de conexión o URLs caídas
            imageView.setImage(obtenerImagenPlaceholder());
            setGraphic(imageView);
            setText(null);
        }
    }
});
```

### Puntos Didácticos Clave:
1. **Reutilización de celdas (Virtualización):** JavaFX no crea un `TableCell` para cada fila de la lista, sino solo las necesarias para la pantalla visible. El método `updateItem(...)` se invoca continuamente conforme el usuario hace scroll, por lo que debe ser rápido y limpiar el gráfico cuando `empty` sea verdadero.
2. **Carga en segundo plano (`backgroundLoading`):** Al pasar `true` en el último parámetro del constructor de `Image`, JavaFX descarga la imagen en un hilo secundario sin congelar los botones de la interfaz.

---

## 2. Simulación de Espera Asíncrona con `PauseTransition`

En la vista de Inicio de Sesión, para brindar una experiencia de usuario moderna y realista, simulamos un breve proceso de autenticación mostrando un indicador o texto de **"Cargando..."** antes de redirigir al usuario:

```java
// 1. Mostrar retroalimentación visual inmediata
indicadorCargando.setVisible(true);
botonIngresar.setDisable(true); // Evitar doble clic

// 2. Crear una pausa no bloqueante de 1.2 segundos
PauseTransition pausa = new PauseTransition(Duration.seconds(1.2));

// 3. Ejecutar la acción al culminar la pausa
pausa.setOnFinished(evento -> {
    indicadorCargando.setVisible(false);
    botonIngresar.setDisable(false);

    // Ejecutar navegación a la siguiente vista
    try {
        if (usuarioAutenticado.esAdministrador()) {
            App.setRoot("admin_dashboard");
        } else {
            App.setRoot("cliente_placeholder");
        }
    } catch (IOException e) {
        mostrarError("Error al cargar la siguiente vista.");
    }
});

// 4. Iniciar la transición
pausa.play();
```

### ¿Por qué `PauseTransition` y no `Thread.sleep(...)`?
* `Thread.sleep(...)` detiene el hilo principal de JavaFX (*JavaFX Application Thread*), congelando la ventana por completo (la ventana deja de responder y no se redibuja la animación de carga).
* `PauseTransition` utiliza el temporizador interno de la escena de JavaFX, manteniendo la interfaz viva y reactiva.

---

## 3. Navegación Dinámica entre Escenas (`App.setRoot`)

En lugar de abrir múltiples ventanas modales separadas que saturan el escritorio, Belle Store utiliza el patrón de navegación en una única escena principal mediante reemplazo de raíz (`setRoot`):

```java
public class App extends Application {
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Carga la primera vista (Login)
        scene = new Scene(loadFXML("login"), 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Belle Store");
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
}
```

### Ventajas didácticas:
1. **Transición suave:** Al llamar `App.setRoot("registro")` o `App.setRoot("admin_dashboard")`, la ventana actual cambia su contenido al instante sin parpadear ni abrir nuevas instancias del Stage.
2. **Centralización:** Toda la lógica de carga de recursos FXML se concentra en un único método `loadFXML`.

---

## 4. Restricción de Entrada de Caracteres con `TextFormatter`

En formularios donde los campos requieren tipos de datos estrictos (por ejemplo, nombres que no deben aceptar números, o números de celular e identificación que no deben aceptar letras), JavaFX ofrece `TextFormatter`.

### ¿Por qué usar `TextFormatter` en lugar de validar después de escribir?
* Evita que caracteres no permitidos aparezcan en pantalla o se almacenen temporalmente en el buffer.
* Ofrece retroalimentación inmediata al usuario al pulsar una tecla inválida.

### Ejemplo didáctico:
```java
// 1. Restringir 'Nombre Completo' solo a letras y espacios (incluye tildes y ñ):
UnaryOperator<TextFormatter.Change> filtroLetras = cambio -> {
    String nuevoTexto = cambio.getControlNewText();
    if (nuevoTexto.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$")) {
        return cambio; // Permite el cambio
    }
    return null; // Rechaza el cambio (no se escribe el dígito o símbolo)
};
campoNombre.setTextFormatter(new TextFormatter<>(filtroLetras));

// 2. Restringir 'Número de Celular' solo a dígitos numéricos y máximo 10 dígitos:
UnaryOperator<TextFormatter.Change> filtroCelular = cambio -> {
    String nuevoTexto = cambio.getControlNewText();
    if (nuevoTexto.matches("^\\d*$") && nuevoTexto.length() <= 10) {
        return cambio;
    }
    return null; // Rechaza letras o si excede los 10 dígitos
};
campoCelular.setTextFormatter(new TextFormatter<>(filtroCelular));
```

---

## 5. Validación Reactiva en Tiempo Real y CSS Dinámico

Para una experiencia de usuario fluida, las validaciones no deben esperar a que se presione el botón de envío.

### Comparación de Contraseñas y Limpieza en Tiempo Real
Usando escuchadores (`ChangeListener`) sobre la propiedad `textProperty()` de los campos de texto:

```java
// Escuchar cuando el usuario escribe en 'Confirmar Contraseña'
campoConfirmarContrasena.textProperty().addListener((obs, oldV, newV) -> {
    validarContrasenasEnTiempoReal();
});

private void validarContrasenasEnTiempoReal() {
    String p1 = campoContrasena.getText();
    String p2 = campoConfirmarContrasena.getText();

    if (p2.isEmpty()) {
        limpiarError(campoConfirmarContrasena, lblConfirmarContrasena);
        return;
    }

    if (p1.equals(p2)) {
        // Coinciden: aplicamos estilo verde de éxito
        marcarValido(campoContrasena);
        marcarValido(campoConfirmarContrasena);
    } else {
        // No coinciden: aplicamos borde rojo y etiqueta roja
        marcarError(campoConfirmarContrasena, lblConfirmarContrasena);
    }
}
```

### Inyección de Clases CSS:
Para resaltar errores, se agregan o remueven dinámicamente clases CSS en la lista `getStyleClass()` del control y de su etiqueta asociada:
* `.campo-error`: Aplica `-fx-border-color: #D32F2F; -fx-border-width: 2px;`.
* `.etiqueta-campo-error`: Aplica `-fx-text-fill: #D32F2F;`.
* `.campo-valido`: Aplica `-fx-border-color: #2E7D32; -fx-border-width: 2px;`.

---

## 6. Columna de Acciones en `TableView` (`TableCell<T, Void>` con `HBox`)

En las tablas de administración de Figma (Productos y Órdenes), cada fila cuenta con dos acciones en la columna final: **Editar** (icono de lápiz) y **Eliminar** (icono de caneca/remove).

### ¿Cómo renderizar botones interactivos por fila en JavaFX?
Se crea una columna con tipo `Void` (`TableColumn<Producto, Void>`) y se define un `CellFactory` personalizado que instancie los botones y los asocie al objeto correspondiente a esa fila mediante `getIndex()`:

```java
colProdAcciones.setCellFactory(col -> new TableCell<Producto, Void>() {
    private final Button btnEditar = new Button();
    private final Button btnEliminar = new Button();
    private final HBox contenedor = new HBox(8, btnEditar, btnEliminar);

    {
        contenedor.setAlignment(Pos.CENTER);
        btnEditar.setGraphic(crearIcono("pencil.png"));
        btnEliminar.setGraphic(crearIcono("remove.png"));

        // Al presionar editar, se obtiene el ítem del modelo de esa fila
        btnEditar.setOnAction(e -> {
            Producto p = getTableView().getItems().get(getIndex());
            abrirModalEditarProducto(p);
        });

        btnEliminar.setOnAction(e -> {
            Producto p = getTableView().getItems().get(getIndex());
            abrirModalEliminarProducto(p);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(contenedor);
        }
    }
});
```

---

## 7. Búsqueda y Filtrado Reactivo en Tiempo Real

Para no requerir que el usuario presione un botón de "Buscar", JavaFX permite escuchar las modificaciones del texto del buscador en vivo:

```java
campoBusqueda.textProperty().addListener((obs, textoAnterior, textoNuevo) -> {
    aplicarFiltrosActuales();
});
```

Al dispararse el evento, se consulta el gestor de persistencia combinando:
1. El texto tecleado (`contains(toLowerCase())` contra ID, Nombre o Categoría).
2. La categoría seleccionada en el panel lateral de filtros.
3. El estado seleccionado (Disponible o Agotado).

La lista resultante se asigna a la tabla mediante `tablaProductos.setItems(FXCollections.observableArrayList(listaFiltrada))`, actualizando la vista y el contador de registros de manera instantánea.


---

## 8. Selector de archivos (`FileChooser`) y vista previa

El botón **Examinar...** del modal de producto abre el selector de archivos del sistema:

```java
FileChooser selector = new FileChooser();
selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
        "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
File elegido = selector.showOpenDialog(ventanaPropietaria);
if (elegido != null) {
    // Se COPIA a datos/imagenes/ para que el producto siga funcionando aunque el original se mueva
    txtImagen.setText(GestorImagenes.importarImagen(elegido)); // -> "imagenes/foto.png"
}
```

La vista previa es un `ImageView` que se actualiza con un *listener* sobre `txtImagen.textProperty()`. Si la imagen no existe o la carga falla (`Image.errorProperty()`), se muestra la imagen genérica.

## 9. Validar sin cerrar un `Dialog`

Para impedir que el botón *Guardar* cierre el diálogo cuando hay errores, se consume el evento antes de que el diálogo lo procese:

```java
Button ok = (Button) dialogo.getDialogPane().lookupButton(btnGuardar);
ok.addEventFilter(ActionEvent.ACTION, evento -> {
    if (hayErrores()) {
        mostrarErrorEnRojo();
        evento.consume(); // el diálogo permanece abierto
    }
});
```
