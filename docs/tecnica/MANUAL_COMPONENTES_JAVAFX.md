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
