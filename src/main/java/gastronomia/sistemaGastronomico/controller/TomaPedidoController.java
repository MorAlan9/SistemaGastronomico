package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.*;
import gastronomia.sistemaGastronomico.model.*;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
public class TomaPedidoController extends BaseController {

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
        Label placeholder = new Label("🛒 Seleccione productos...\n(F5 para Marchar)");
        placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-text-alignment: CENTER;");
        listaItems.setPlaceholder(placeholder);

        Platform.runLater(() -> {
            if (btnComandar.getScene() != null) {
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::accionComandar);
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F4), this::accionCobrar);
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::accionVolver);
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F3), this::accionFocoBuscador);
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F2), this::accionBuscarMesa);
            }
        });

        listaItems.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) editarObservacion();
        });

        if (txtBuscarProducto != null) {
            txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> filtrarProductos(newVal));
        }
    }

    @FXML public void accionFocoBuscador() { if(txtBuscarProducto != null) { txtBuscarProducto.requestFocus(); txtBuscarProducto.selectAll(); } }

    @FXML
    public void accionBuscarMesa() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cambiar Mesa");
        dialog.setHeaderText("Saltar a otra mesa");
        dialog.setContentText("Ingrese N° de Mesa:");
        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);
                if (mesaOpt.isPresent()) {
                    setMesa(mesaOpt.get());
                    toast("🔀 Cambiado a Mesa " + numero, lblTituloMesa);
                } else {
                    error("Error", "La mesa " + numero + " no existe.");
                }
            } catch (NumberFormatException e) {
                error("Error", "Ingrese un número válido.");
            }
        });
    }

    private void filtrarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProductos(productoRepo.findByActivoTrue());
            return;
        }
        String busqueda = texto.toLowerCase();
        List<Producto> filtrados = productoRepo.findByActivoTrue().stream()
                .filter(p -> p.getNombre().toLowerCase().contains(busqueda))
                .collect(Collectors.toList());
        cargarProductos(filtrados);
    }

    private void editarObservacion() {
        DetallePedido item = listaItems.getSelectionModel().getSelectedItem();
        if (item == null) return;
        TextInputDialog dialog = new TextInputDialog(item.getObservacion() != null ? item.getObservacion() : "");
        dialog.setTitle("Nota");
        dialog.setHeaderText("Observación para: " + item.getProducto().getNombre());
        dialog.showAndWait().ifPresent(nota -> {
            item.setObservacion(nota);
            detalleRepo.save(item);
            actualizarVistaPedido();
        });
    }

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("MESA " + mesa.getNumero());
        this.pedidoActual = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO").orElse(null);
        if (this.pedidoActual == null) crearNuevoPedidoEmergencia();
        actualizarEstadoBotonComandar();
        actualizarVistaPedido();
        if(txtBuscarProducto != null) txtBuscarProducto.clear();
    }

    private void crearNuevoPedidoEmergencia() {
        Mozo mozo = mozoRepo.findAll().stream().findFirst().orElse(null);
        if(mozo == null) return;
        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesaActual, mozo);
        this.pedidoActual = pedidoRepo.save(nuevo);
    }

    @FXML
    public void accionComandar() {
        if (pedidoActual == null || btnComandar.isDisabled()) return;
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        List<DetallePedido> nuevos = detalles.stream().filter(d -> d.getHoraMarchar() == null).collect(Collectors.toList());

        if (nuevos.isEmpty()) {
            toast("Nada nuevo para marchar", btnComandar);
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
            error("Error", "No se pudo comandar: " + e.getMessage());
        }
    }

    private void actualizarEstadoBotonComandar() {
        if (pedidoActual == null) return;
        boolean hayNuevos = detalleRepo.findByPedido(pedidoActual).stream().anyMatch(d -> d.getHoraMarchar() == null);
        if (hayNuevos) {
            btnComandar.setDisable(false);
            btnComandar.setText("♨ MARCHAR NUEVOS (F5)");
            btnComandar.setStyle("");
            btnComandar.getStyleClass().add("btn-comandar");
        } else {
            btnComandar.setDisable(true);
            btnComandar.setText("PEDIDO EN CURSO");
            btnComandar.setStyle("");
        }
    }

    @FXML
    public void accionEliminarItem() {
        DetallePedido seleccionado = listaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        if (seleccionado.getHoraMarchar() != null) {
            advertencia("Bloqueado", "Este ítem ya fue marchado.");
            return;
        }
        pedidoService.quitarProducto(seleccionado.getId());
        actualizarVistaPedido();
        actualizarEstadoBotonComandar();
    }

    private void agregarProductoAlPedido(Producto prod) {
        if (prod.getStock() <= 0) { toast("🚫 Sin Stock", contenedorProductos); return; }
        if (pedidoActual == null) return;
        pedidoService.agregarProducto(pedidoActual.getId(), prod.getId(), 1);
        actualizarVistaPedido();
        actualizarEstadoBotonComandar();
    }

    // --- AQUÍ ESTÁ LA CLAVE: ABRE LA NUEVA VENTANA DE COBRO ---
    @FXML
    public void accionCobrar() {
        if (pedidoActual == null) return;
        actualizarVistaPedido();

        try {
            // Cargar la vista Cobrar.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Cobrar.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            // Pasar el pedido al controlador
            CobrarController controller = loader.getController();
            controller.setPedido(pedidoActual);

            Stage stage = new Stage();
            stage.setTitle("Cobrar Mesa " + mesaActual.getNumero());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            // Al cerrar, verificar si se pagó
            stage.setOnHidden(e -> {
                pedidoRepo.findById(pedidoActual.getId()).ifPresent(p -> {
                    if ("CERRADO".equals(p.getEstado())) {
                        cerrarVentana(); // Cerrar toma pedido si se cobró
                    } else {
                        actualizarVistaPedido();
                    }
                });
            });
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            error("Error", "No se pudo abrir ventana cobro: " + e.getMessage());
        }
    }

    private void actualizarVistaPedido() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        detalles.sort(Comparator.comparing(DetallePedido::getHoraMarchar, Comparator.nullsLast(Comparator.naturalOrder())));
        listaItems.getItems().setAll(detalles);

        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (DetallePedido d : detalles) {
            BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
            totalCalculado = totalCalculado.add(subtotal);
        }
        lblTotal.setText("TOTAL: $" + totalCalculado);
        if (pedidoActual != null) {
            pedidoActual.setTotal(totalCalculado);
            pedidoRepo.save(pedidoActual);
        }

        // Renderizado simple de celda
        listaItems.setCellFactory(param -> new ListCell<DetallePedido>() {
            @Override
            protected void updateItem(DetallePedido item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    String nombre = item.getProducto().getNombre();
                    String cant = item.getCantidad() + "x";
                    String estado = item.getHoraMarchar() == null ? " (Nuevo)" : "";
                    setText(cant + " " + nombre + estado + " - $" + item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad())));
                    if (item.getHoraMarchar() == null) setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
                    else setStyle("-fx-text-fill: gray;");
                }
            }
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

    @FXML public void accionVolver() { cerrarVentana(); }
    private void cerrarVentana() { ((Stage) lblTotal.getScene().getWindow()).close(); }
}