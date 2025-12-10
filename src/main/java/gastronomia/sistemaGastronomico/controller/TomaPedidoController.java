package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.*;
import gastronomia.sistemaGastronomico.model.*;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class TomaPedidoController {

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
        // CAMBIO: Solo cargamos productos activos al iniciar
        cargarProductos(productoRepo.findByActivoTrue());

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemRestar = new MenuItem("➖ Quitar 1 unidad");
        itemRestar.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        itemRestar.setOnAction(e -> accionEliminarItem());
        contextMenu.getItems().add(itemRestar);
        listaItems.setContextMenu(contextMenu);
    }

    @FXML
    public void accionEliminarItem() {
        DetallePedido seleccionado = listaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Atención", "Selecciona un ítem de la lista para quitar.");
            return;
        }
        pedidoService.quitarProducto(seleccionado.getId());
        actualizarVistaPedido();
    }

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("Mesa " + mesa.getNumero());

        // AUTO-RELOAD: Solo traemos productos activos
        cargarProductos(productoRepo.findByActivoTrue());

        this.pedidoActual = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO").orElse(null);
        if (this.pedidoActual == null) crearNuevoPedidoEmergencia();
        actualizarVistaPedido();
    }

    private void crearNuevoPedidoEmergencia() {
        Mozo mozo = mozoRepo.findAll().stream().findFirst().orElse(null);
        if(mozo == null) return;
        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesaActual, mozo);
        this.pedidoActual = pedidoRepo.save(nuevo);
    }

    private void agregarProductoAlPedido(Producto prod) {
        if (prod.getStock() <= 0) {
            mostrarAlerta("Sin Stock", "No queda stock de " + prod.getNombre());
            return;
        }
        if (pedidoActual == null) return;
        pedidoService.agregarProducto(pedidoActual.getId(), prod.getId(), 1);
        actualizarVistaPedido();
    }

    private void actualizarVistaPedido() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        listaItems.getItems().setAll(detalles);

        listaItems.setCellFactory(param -> new ListCell<DetallePedido>() {
            @Override
            protected void updateItem(DetallePedido item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCantidad() + "x   " + item.getProducto().getNombre() +
                            "   ($" + item.getPrecioUnitario() + ")");
                    setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                }
            }
        });

        Pedido p = pedidoRepo.findById(pedidoActual.getId()).orElseThrow();
        lblTotal.setText("Total: $" + p.getTotal());
    }

    @FXML
    public void accionCobrar() {
        ChoiceDialog<String> dialogo = new ChoiceDialog<>("Efectivo", "Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "QR / MP");
        dialogo.setTitle("Cobrar Mesa " + mesaActual.getNumero());
        dialogo.setHeaderText("Total a pagar: " + lblTotal.getText());
        dialogo.setContentText("Forma de pago:");

        Optional<String> resultado = dialogo.showAndWait();

        if (resultado.isPresent()) {
            String formaPago = resultado.get();
            pedidoActual.setMetodoPago(formaPago);
            pedidoRepo.save(pedidoActual);

            List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
            for (DetallePedido det : detalles) {
                Producto prodReal = productoRepo.findById(det.getProducto().getId()).orElse(null);
                if (prodReal != null) {
                    int nuevoStock = prodReal.getStock() - det.getCantidad();
                    prodReal.setStock(nuevoStock);
                    productoRepo.save(prodReal);
                }
            }
            pedidoService.cerrarMesa(pedidoActual.getId());
            mostrarAlerta("Cobro Exitoso", "Pago registrado y stock actualizado.");
            cerrarVentana();
        }
    }

    @FXML
    public void accionVolver() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }

    private void cargarProductos(List<Producto> lista) {
        contenedorProductos.getChildren().clear();
        for (Producto prod : lista) {
            // Nota: Aquí no hace falta validar prod.getActivo() porque la consulta SQL ya lo filtró
            String texto = prod.getNombre() + "\n$" + prod.getPrecioActual() + "\n(Stock: " + prod.getStock() + ")";
            Button btn = new Button(texto);
            btn.setPrefSize(110, 80);

            if (prod.getStock() <= 0) {
                btn.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: red; -fx-font-size: 10px; text-align: center;");
                btn.setDisable(true);
            } else {
                btn.setStyle("-fx-font-size: 11px; text-align: center;");
            }
            btn.setOnAction(e -> agregarProductoAlPedido(prod));
            contenedorProductos.getChildren().add(btn);
        }
    }

    private void generarBotonesFiltros() {
        contenedorFiltros.getChildren().clear();

        Button btnReload = new Button("🔄");
        btnReload.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReload.setTooltip(new Tooltip("Recargar Productos Activos"));
        // CAMBIO: Usamos findByActivoTrue()
        btnReload.setOnAction(e -> cargarProductos(productoRepo.findByActivoTrue()));
        contenedorFiltros.getChildren().add(btnReload);

        Button btnTodo = new Button("TODO");
        // CAMBIO: Usamos findByActivoTrue()
        btnTodo.setOnAction(e -> cargarProductos(productoRepo.findByActivoTrue()));
        contenedorFiltros.getChildren().add(btnTodo);

        List<Categoria> categorias = categoriaRepo.findAll();
        String[] colores = {"#FFA726", "#29B6F6", "#66BB6A", "#AB47BC", "#EF5350"};
        int i = 0;
        for (Categoria cat : categorias) {
            Button btn = new Button(cat.getNombre().toUpperCase());
            String color = colores[i % colores.length];
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold;");
            // CAMBIO: Usamos findByCategoriaAndActivoTrue(cat)
            btn.setOnAction(e -> cargarProductos(productoRepo.findByCategoriaAndActivoTrue(cat)));
            contenedorFiltros.getChildren().add(btn);
            i++;
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sistema");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}