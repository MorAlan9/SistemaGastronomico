package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.*;
import gastronomia.sistemaGastronomico.model.*;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node; // IMPORTANTE: Usar Node en lugar de Control
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TomaPedidoController {

    private final ProductoRepository productoRepo;
    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;
    private final MozoRepository mozoRepo;
    private final CategoriaRepository categoriaRepo;
    private final PedidoService pedidoService;
    private final MesaRepository mesaRepo;
    private final ApplicationContext context;

    private Mesa mesaActual;
    private Pedido pedidoActual;

    @FXML private HBox contenedorFiltros;
    @FXML private FlowPane contenedorProductos;
    @FXML private Label lblTituloMesa;
    @FXML private ListView<DetallePedido> listaItems;
    @FXML private Label lblTotal;
    @FXML private Button btnComandar;
    @FXML private TextField txtBuscarProducto;

    public TomaPedidoController(ProductoRepository productoRepo, PedidoRepository pedidoRepo,
                                DetallePedidoRepository detalleRepo, MozoRepository mozoRepo,
                                CategoriaRepository categoriaRepo, PedidoService pedidoService,
                                MesaRepository mesaRepo, ApplicationContext context) {
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.mozoRepo = mozoRepo;
        this.categoriaRepo = categoriaRepo;
        this.pedidoService = pedidoService;
        this.mesaRepo = mesaRepo;
        this.context = context;
    }

    @FXML
    public void initialize() {
        generarBotonesFiltros();
        cargarProductos(productoRepo.findByActivoTrue());

        listaItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Label placeholder = new Label("🛒 Seleccione productos...\n(F5 Marchar | F2 Cambiar Mesa)");
        placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-text-alignment: CENTER;");
        listaItems.setPlaceholder(placeholder);

        // --- CORRECCIÓN CRÍTICA PARA TECLAS (F2, F3, F4, F5) ---
        // Usamos un listener sobre la propiedad 'scene'. Esto garantiza que
        // el evento se registre apenas la ventana sea visible, evitando nulos.
        lblTituloMesa.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // addEventFilter captura la tecla ANTES que cualquier campo de texto
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F5) {
                        accionComandar();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F4) {
                        accionCobrar();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F2) {
                        accionBuscarMesa(); // <--- AQUÍ ESTÁ EL SALTO DE MESA
                        event.consume();
                    } else if (event.getCode() == KeyCode.F3) {
                        accionFocoBuscador();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        accionVolver();
                        event.consume();
                    }
                });
            }
        });

        // Configurar Lista (Doble click para notas)
        listaItems.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) editarObservacion();
        });

        if (txtBuscarProducto != null) {
            txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> filtrarProductos(newVal));
        }
    }

    // --- ACCIÓN F2: CAMBIAR MESA ---
    @FXML
    public void accionBuscarMesa() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cambiar Mesa");
        dialog.setHeaderText("Saltar a otra mesa (F2)");
        dialog.setContentText("Ingrese N° de Mesa:");

        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);
                if (mesaOpt.isPresent()) {
                    setMesa(mesaOpt.get());
                    toast("🔀 Cambiado a Mesa " + numero, lblTituloMesa);
                } else {
                    mostrarAlerta("Error", "La mesa " + numero + " no existe.");
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "Ingrese un número válido.");
            }
        });
    }

    // --- ACCIÓN F5: COMANDAR ---
    @FXML
    public void accionComandar() {
        if (pedidoActual == null) return;
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        List<DetallePedido> nuevos = detalles.stream().filter(d -> d.getHoraMarchar() == null).collect(Collectors.toList());

        if (nuevos.isEmpty()) {
            toast("✅ Nada nuevo para marchar", btnComandar);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        boolean hayCocina = nuevos.stream().anyMatch(d -> d.getProducto().isEsCocina());

        try {
            for (DetallePedido d : nuevos) {
                d.setHoraMarchar(ahora);
                detalleRepo.save(d);
            }
            if (hayCocina) pedidoActual.setHoraComanda(ahora);
            pedidoActual.setHoraUltimoProducto(ahora);
            pedidoRepo.save(pedidoActual);

            actualizarEstadoBotonComandar();
            actualizarVistaPedido();

            toast(hayCocina ? "♨ Marchando a Cocina" : "🍺 Marchando a Barra", btnComandar);

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo comandar: " + e.getMessage());
        }
    }

    // --- MÉTODO TOAST CORREGIDO (Usa Node en vez de Control) ---
    private void toast(String mensaje, Node nodo) {
        Platform.runLater(() -> {
            try {
                Popup popup = new Popup();
                Label label = new Label(mensaje);
                label.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-padding: 15px; -fx-background-radius: 10; -fx-font-size: 16px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");
                popup.getContent().add(label);
                popup.setAutoHide(true);

                if (nodo != null && nodo.getScene() != null && nodo.getScene().getWindow() != null) {
                    Bounds bounds = nodo.localToScreen(nodo.getBoundsInLocal());
                    // Ajuste para centrar el toast respecto al nodo
                    popup.show(nodo, bounds.getMinX(), bounds.getMinY() - 50);
                } else {
                    // Si el nodo no está listo, usar la ventana principal
                    if (lblTituloMesa.getScene() != null && lblTituloMesa.getScene().getWindow() != null) {
                        popup.show(lblTituloMesa.getScene().getWindow());
                    }
                }

                PauseTransition delay = new PauseTransition(Duration.seconds(2));
                delay.setOnFinished(e -> popup.hide());
                delay.play();
            } catch (Exception e) {
                System.out.println("Error mostrando toast: " + e.getMessage());
            }
        });
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.show();
    }

    // --- RESTO DE LA LÓGICA ---

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("MESA " + mesa.getNumero());
        this.pedidoActual = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO").orElse(null);
        if (this.pedidoActual == null) crearNuevoPedidoEmergencia();
        actualizarEstadoBotonComandar();
        actualizarVistaPedido();
        if(txtBuscarProducto != null) {
            txtBuscarProducto.clear();
            // txtBuscarProducto.requestFocus(); // Opcional: enfocar buscador al cambiar mesa
        }
    }

    private void crearNuevoPedidoEmergencia() {
        Mozo mozo = mozoRepo.findAll().stream().findFirst().orElse(null);
        if(mozo == null) return;
        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesaActual, mozo);
        this.pedidoActual = pedidoRepo.save(nuevo);
    }

    private void actualizarEstadoBotonComandar() {
        if (pedidoActual == null) return;
        boolean hayNuevos = detalleRepo.findByPedido(pedidoActual).stream().anyMatch(d -> d.getHoraMarchar() == null);
        if (hayNuevos) {
            btnComandar.setDisable(false);
            btnComandar.setText("♨ MARCHAR (F5)");
            btnComandar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        } else {
            btnComandar.setDisable(true);
            btnComandar.setText("EN CURSO");
            btnComandar.setStyle("");
        }
    }

    private void agregarProductoAlPedido(Producto prod) {
        if (prod.getStock() <= 0) {
            toast("🚫 Sin Stock", contenedorProductos);
            return;
        }
        if (pedidoActual == null) return;
        pedidoService.agregarProducto(pedidoActual.getId(), prod.getId(), 1);
        actualizarVistaPedido();
        actualizarEstadoBotonComandar();
    }

    @FXML
    public void accionCobrar() {
        if (pedidoActual == null) return;
        actualizarVistaPedido();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Cobrar.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            CobrarController controller = loader.getController();
            controller.setPedido(pedidoActual);

            Stage stage = new Stage();
            stage.setTitle("Cobrar Mesa " + mesaActual.getNumero());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setOnHidden(e -> {
                pedidoRepo.findById(pedidoActual.getId()).ifPresent(p -> {
                    if ("CERRADO".equals(p.getEstado())) cerrarVentana();
                    else actualizarVistaPedido();
                });
            });
            stage.showAndWait();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir cobro: " + e.getMessage());
        }
    }

    @FXML
    public void accionEliminarItem() {
        DetallePedido item = listaItems.getSelectionModel().getSelectedItem();
        if(item != null) {
            if (item.getHoraMarchar() != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar producto YA MARCHADO?", ButtonType.YES, ButtonType.NO);
                if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) return;
            }
            pedidoService.quitarProducto(item.getId());
            actualizarVistaPedido();
            actualizarEstadoBotonComandar();
        }
    }

    private void actualizarVistaPedido() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        detalles.sort(Comparator.comparing(DetallePedido::getHoraMarchar, Comparator.nullsLast(Comparator.naturalOrder())));
        listaItems.getItems().setAll(detalles);

        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (DetallePedido d : detalles) {
            totalCalculado = totalCalculado.add(d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad())));
        }
        lblTotal.setText("TOTAL: $" + totalCalculado);

        listaItems.setCellFactory(param -> new ListCell<DetallePedido>() {
            @Override
            protected void updateItem(DetallePedido item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    String nombre = item.getProducto().getNombre();
                    String cant = item.getCantidad() + "x";
                    String hora = item.getHoraMarchar() != null ?
                            " [" + item.getHoraMarchar().format(DateTimeFormatter.ofPattern("HH:mm")) + "]" : " (Nuevo)";

                    setText(cant + " " + nombre + hora + " - $" + item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad())));

                    if (item.getHoraMarchar() == null) setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
                    else setStyle("-fx-text-fill: gray;");

                    ContextMenu menu = new ContextMenu();
                    MenuItem itemEliminar = new MenuItem("Eliminar");
                    itemEliminar.setOnAction(e -> accionEliminarItem());
                    MenuItem itemNota = new MenuItem("Nota Cocina");
                    itemNota.setOnAction(e -> editarObservacion());
                    menu.getItems().addAll(itemNota, new SeparatorMenuItem(), itemEliminar);
                    setContextMenu(menu);
                }
            }
        });

        if (pedidoActual != null) {
            pedidoActual.setTotal(totalCalculado);
            pedidoRepo.save(pedidoActual);
        }
    }

    private void editarObservacion() {
        DetallePedido item = listaItems.getSelectionModel().getSelectedItem();
        if (item == null) return;
        TextInputDialog dialog = new TextInputDialog(item.getObservacion() != null ? item.getObservacion() : "");
        dialog.setTitle("Nota Cocina");
        dialog.setHeaderText("Observación para: " + item.getProducto().getNombre());
        dialog.showAndWait().ifPresent(nota -> {
            item.setObservacion(nota);
            detalleRepo.save(item);
            actualizarVistaPedido();
        });
    }

    private void cargarProductos(List<Producto> lista) {
        contenedorProductos.getChildren().clear();
        for (Producto prod : lista) {
            Button btn = new Button(prod.getNombre() + "\n$" + prod.getPrecioActual());
            btn.setPrefSize(130, 90);
            btn.getStyleClass().add("product-btn");
            if (prod.getStock() <= 0) btn.setDisable(true);
            btn.setOnAction(e -> agregarProductoAlPedido(prod));
            contenedorProductos.getChildren().add(btn);
        }
    }

    private void generarBotonesFiltros() {
        contenedorFiltros.getChildren().clear();
        Button btnTodo = new Button("TODOS");
        btnTodo.setOnAction(e -> cargarProductos(productoRepo.findByActivoTrue()));
        contenedorFiltros.getChildren().add(btnTodo);
        for (Categoria cat : categoriaRepo.findAll()) {
            Button btn = new Button(cat.getNombre());
            btn.setOnAction(e -> cargarProductos(productoRepo.findByCategoriaAndActivoTrue(cat)));
            contenedorFiltros.getChildren().add(btn);
        }
    }

    private void filtrarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProductos(productoRepo.findByActivoTrue());
            return;
        }
        List<Producto> filtrados = productoRepo.findByActivoTrue().stream()
                .filter(p -> p.getNombre().toLowerCase().contains(texto.toLowerCase()))
                .collect(Collectors.toList());
        cargarProductos(filtrados);
    }

    @FXML public void accionFocoBuscador() { if(txtBuscarProducto != null) { txtBuscarProducto.requestFocus(); txtBuscarProducto.selectAll(); } }
    @FXML public void accionVolver() { ((Stage) lblTotal.getScene().getWindow()).close(); }

    // Método auxiliar para cerrar la ventana (usado por otros métodos)
    private void cerrarVentana() {
        if (lblTotal.getScene() != null && lblTotal.getScene().getWindow() != null) {
            ((Stage) lblTotal.getScene().getWindow()).close();
        }
    }
}