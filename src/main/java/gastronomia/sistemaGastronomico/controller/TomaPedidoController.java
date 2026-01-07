package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.*;
import gastronomia.sistemaGastronomico.model.*;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class TomaPedidoController extends BaseController {

    private final ProductoRepository productoRepo;
    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;
    private final MozoRepository mozoRepo;
    private final CategoriaRepository categoriaRepo;
    private final PedidoService pedidoService;

    private Mesa mesaActual;
    private Pedido pedidoActual;

    @FXML private HBox contenedorFiltros;
    @FXML private FlowPane contenedorProductos;
    @FXML private Label lblTituloMesa;
    @FXML private ListView<DetallePedido> listaItems;
    @FXML private Label lblTotal;
    @FXML private Button btnComandar;

    public TomaPedidoController(ProductoRepository productoRepo, PedidoRepository pedidoRepo,
                                DetallePedidoRepository detalleRepo, MozoRepository mozoRepo,
                                CategoriaRepository categoriaRepo, PedidoService pedidoService) {
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.mozoRepo = mozoRepo;
        this.categoriaRepo = categoriaRepo;
        this.pedidoService = pedidoService;
    }

    @FXML
    public void initialize() {
        generarBotonesFiltros();
        cargarProductos(productoRepo.findByActivoTrue());

        Label placeholder = new Label("🛒 Seleccione productos del menú\npara comenzar el pedido.");
        placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-text-alignment: CENTER;");
        listaItems.setPlaceholder(placeholder);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemRestar = new MenuItem("🗑 Quitar ítem");
        itemRestar.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        itemRestar.setOnAction(e -> accionEliminarItem());
        contextMenu.getItems().add(itemRestar);
        listaItems.setContextMenu(contextMenu);
    }

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("MESA " + mesa.getNumero());
        this.pedidoActual = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO").orElse(null);
        if (this.pedidoActual == null) crearNuevoPedidoEmergencia();

        actualizarEstadoBotonComandar();
        actualizarVistaPedido();
    }

    private void crearNuevoPedidoEmergencia() {
        Mozo mozo = mozoRepo.findAll().stream().findFirst().orElse(null);
        if(mozo == null) return;
        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesaActual, mozo);
        this.pedidoActual = pedidoRepo.save(nuevo);
    }

    // --- ACCIÓN COMANDAR ---
    @FXML
    public void accionComandar() {
        if (pedidoActual == null) return;

        if (pedidoActual.getHoraComanda() != null) {
            advertencia("Ya enviado", "Este pedido ya está en cocina.");
            return;
        }

        try {
            pedidoActual.setHoraComanda(LocalDateTime.now());
            pedidoRepo.save(pedidoActual);

            toast("✅ Pedido marchado a cocina", btnComandar);
            actualizarEstadoBotonComandar();
            actualizarVistaPedido();
        } catch (Exception e) {
            error("Error", "No se pudo comandar: " + e.getMessage());
        }
    }

    private void actualizarEstadoBotonComandar() {
        if (pedidoActual != null && pedidoActual.getHoraComanda() != null) {
            btnComandar.setDisable(true);
            btnComandar.setText("🕒 EN COCINA");
            btnComandar.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-opacity: 0.8;");
        } else {
            btnComandar.setDisable(false);
            btnComandar.setText("♨ COMANDAR");
            btnComandar.setStyle("");
            btnComandar.getStyleClass().add("btn-comandar");
        }
    }

    @FXML
    public void accionEliminarItem() {
        if (pedidoActual != null && pedidoActual.getHoraComanda() != null) {
            advertencia("Pedido en Cocina", "No se pueden eliminar ítems que ya marcharon.");
            return;
        }
        DetallePedido seleccionado = listaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            toast("Selecciona un producto", listaItems);
            return;
        }
        pedidoService.quitarProducto(seleccionado.getId());
        actualizarVistaPedido();
    }

    private void agregarProductoAlPedido(Producto prod) {
        if (prod.getStock() <= 0) {
            toast("🚫 Sin Stock", contenedorProductos);
            return;
        }
        if (pedidoActual == null) return;

        pedidoService.agregarProducto(pedidoActual.getId(), prod.getId(), 1);
        pedidoActual.setHoraUltimoProducto(LocalDateTime.now());
        pedidoRepo.save(pedidoActual);

        actualizarVistaPedido();
    }

    private void actualizarVistaPedido() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        listaItems.getItems().setAll(detalles);
        listaItems.getStyleClass().add("ticket-list");

        boolean enCocina = (pedidoActual != null && pedidoActual.getHoraComanda() != null);

        // Calcular total en vivo
        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (DetallePedido d : detalles) {
            BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
            totalCalculado = totalCalculado.add(subtotal);
        }
        lblTotal.setText("TOTAL: $" + totalCalculado);

        // Actualizamos objeto en memoria (por si acaso)
        if (pedidoActual != null) pedidoActual.setTotal(totalCalculado);

        listaItems.setCellFactory(param -> new ListCell<DetallePedido>() {
            @Override
            protected void updateItem(DetallePedido item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    HBox fila = new HBox(); fila.setSpacing(10);

                    String prefix = enCocina ? "✅ " : "";
                    Label lblCant = new Label(prefix + item.getCantidad() + "x");
                    lblCant.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-min-width: 25px;");

                    Label lblNom = new Label(item.getProducto().getNombre());
                    if (enCocina) {
                        lblNom.setStyle("-fx-text-fill: #7f8c8d;");
                        lblCant.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
                    } else {
                        lblNom.setStyle("-fx-text-fill: #333;");
                    }

                    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                    String subtotal = "$" + item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad()));
                    Label lblPrecio = new Label(subtotal);
                    lblPrecio.setStyle("-fx-font-weight: bold;");

                    fila.getChildren().addAll(lblCant, lblNom, spacer, lblPrecio);
                    setGraphic(fila);
                }
            }
        });
    }

    private void cargarProductos(List<Producto> lista) {
        contenedorProductos.getChildren().clear();
        for (Producto prod : lista) {
            String nombre = prod.getNombre();
            String precio = String.format("$%.0f", prod.getPrecioActual());
            String stockInfo = "Stock: " + prod.getStock();

            Button btn = new Button(nombre + "\n" + precio);
            btn.setPrefSize(130, 90);
            btn.getStyleClass().add("product-btn");
            btn.setTooltip(new Tooltip(stockInfo));

            if (prod.getStock() <= 0) {
                btn.setText(nombre + "\nAGOTADO");
                btn.getStyleClass().add("product-btn-no-stock");
                btn.setDisable(true);
            } else if (prod.getStock() < 10) {
                btn.getStyleClass().add("product-btn-low-stock");
            }
            btn.setOnAction(e -> agregarProductoAlPedido(prod));
            contenedorProductos.getChildren().add(btn);
        }
    }

    private void generarBotonesFiltros() {
        contenedorFiltros.getChildren().clear();
        Button btnTodo = new Button("TODOS");
        btnTodo.getStyleClass().add("filter-btn");
        btnTodo.setOnAction(e -> cargarProductos(productoRepo.findByActivoTrue()));
        contenedorFiltros.getChildren().add(btnTodo);

        List<Categoria> categorias = categoriaRepo.findAll();
        for (Categoria cat : categorias) {
            Button btn = new Button(cat.getNombre());
            btn.getStyleClass().add("filter-btn");
            btn.setOnAction(e -> cargarProductos(productoRepo.findByCategoriaAndActivoTrue(cat)));
            contenedorFiltros.getChildren().add(btn);
        }
    }

    // --- ACCIÓN COBRAR ARREGLADA ---
    @FXML
    public void accionCobrar() {
        // 1. Aseguramos el cálculo del total
        actualizarVistaPedido();

        ChoiceDialog<String> dialogo = new ChoiceDialog<>("Efectivo", "Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "QR / MP");
        dialogo.setTitle("Cobrar");
        dialogo.setHeaderText("Mesa " + mesaActual.getNumero() + " - " + lblTotal.getText());
        dialogo.setContentText("Forma de pago:");
        estilizar(dialogo);

        Optional<String> resultado = dialogo.showAndWait();
        if (resultado.isPresent()) {
            try {
                String metodoPago = resultado.get();
                // Limpiar string del total
                String totalStr = lblTotal.getText().replace("TOTAL: $", "").trim();
                BigDecimal totalFinal = new BigDecimal(totalStr);

                // 2. LLAMAR AL SERVICIO PARA QUE HAGA LA MAGIA (Guardar en Historial)
                pedidoService.cobrarPedido(pedidoActual.getId(), metodoPago, totalFinal);

                toast("💵 Venta registrada correctamente", lblTotal);
                cerrarVentana();

            } catch (Exception e) {
                e.printStackTrace();
                error("Error al cobrar", "No se pudo registrar la venta.\n" + e.getMessage());
            }
        }
    }

    @FXML public void accionVolver() { cerrarVentana(); }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }
}