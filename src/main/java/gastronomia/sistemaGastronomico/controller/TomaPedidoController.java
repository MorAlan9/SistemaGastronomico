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
    @FXML private ListView<String> listaItems;
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
        cargarProductos(productoRepo.findAll());
    }

    // --- ACCIONES DE COBRO Y SALIDA ---

    @FXML
    public void accionVolver() {
        // Solo cierra la ventana, la mesa queda ocupada (roja)
        cerrarVentana();
    }

    @FXML
    public void accionCobrar() {
        // 1. Preguntar método de pago
        ChoiceDialog<String> dialogo = new ChoiceDialog<>("Efectivo", "Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "QR / MP");
        dialogo.setTitle("Cobrar Mesa " + mesaActual.getNumero());
        dialogo.setHeaderText("Total a pagar: " + lblTotal.getText());
        dialogo.setContentText("Forma de pago:");

        Optional<String> resultado = dialogo.showAndWait();

        if (resultado.isPresent()) {
            String formaPago = resultado.get();

            // 2. Guardar el pago en el pedido
            pedidoActual.setMetodoPago(formaPago);
            pedidoRepo.save(pedidoActual);

            // 3. Cerrar mesa en el sistema (Service)
            pedidoService.cerrarMesa(pedidoActual.getId());

            mostrarAlerta("Cobro Exitoso", "Se registró el pago con: " + formaPago);

            // 4. Salir
            cerrarVentana();
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }

    // --- LÓGICA DE CARGA Y PRODUCTOS (Igual que antes) ---

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("Mesa " + mesa.getNumero());

        // Buscar pedido abierto
        this.pedidoActual = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO").orElse(null);

        // Si falló algo y no hay pedido, creamos uno de emergencia
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
        if (pedidoActual == null) return;
        pedidoService.agregarProducto(pedidoActual.getId(), prod.getId(), 1);
        actualizarVistaPedido();
    }

    private void actualizarVistaPedido() {
        listaItems.getItems().clear();
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        for (DetallePedido det : detalles) {
            listaItems.getItems().add(det.getCantidad() + "x " + det.getProducto().getNombre() + " ($" + det.getPrecioUnitario() + ")");
        }
        Pedido p = pedidoRepo.findById(pedidoActual.getId()).orElseThrow();
        lblTotal.setText("Total: $" + p.getTotal());
    }

    private void cargarProductos(List<Producto> lista) {
        contenedorProductos.getChildren().clear();
        for (Producto prod : lista) {
            Button btn = new Button(prod.getNombre() + "\n$" + prod.getPrecioActual());
            btn.setPrefSize(110, 80);
            btn.setStyle("-fx-font-size: 11px; text-align: center;");
            btn.setOnAction(e -> agregarProductoAlPedido(prod));
            contenedorProductos.getChildren().add(btn);
        }
    }

    private void generarBotonesFiltros() {
        contenedorFiltros.getChildren().clear();
        Button btnTodo = new Button("TODO");
        btnTodo.setOnAction(e -> cargarProductos(productoRepo.findAll()));
        contenedorFiltros.getChildren().add(btnTodo);

        List<Categoria> categorias = categoriaRepo.findAll();
        String[] colores = {"#FFA726", "#29B6F6", "#66BB6A", "#AB47BC", "#EF5350"};
        int i = 0;
        for (Categoria cat : categorias) {
            Button btn = new Button(cat.getNombre().toUpperCase());
            String color = colores[i % colores.length];
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold;");
            btn.setOnAction(e -> cargarProductos(productoRepo.findByCategoria(cat)));
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