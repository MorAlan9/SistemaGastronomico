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

    @FXML
    public void accionVolver() {
        cerrarVentana();
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

            // 1. Guardar pago
            pedidoActual.setMetodoPago(formaPago);
            pedidoRepo.save(pedidoActual);

            // 2. DESCONTAR STOCK (Lógica Segura)
            List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);

            for (DetallePedido det : detalles) {
                // Buscamos la versión más fresca del producto en BD
                Producto prodReal = productoRepo.findById(det.getProducto().getId()).orElse(null);

                if (prodReal != null) {
                    int nuevoStock = prodReal.getStock() - det.getCantidad();
                    prodReal.setStock(nuevoStock);
                    productoRepo.save(prodReal);
                    System.out.println("Venta: Stock descontado a " + prodReal.getNombre() + ". Quedan: " + nuevoStock);
                }
            }

            // 3. Cerrar mesa
            pedidoService.cerrarMesa(pedidoActual.getId());

            mostrarAlerta("Cobro Exitoso", "Pago registrado y Stock actualizado.");
            cerrarVentana();
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }

    public void setMesa(Mesa mesa) {
        this.mesaActual = mesa;
        lblTituloMesa.setText("Mesa " + mesa.getNumero());

        // AUTO-RELOAD: Recargamos los productos para ver el stock actualizado al entrar
        cargarProductos(productoRepo.findAll());

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

        // Botón Manual de Reload
        Button btnReload = new Button("🔄");
        btnReload.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReload.setOnAction(e -> cargarProductos(productoRepo.findAll()));
        contenedorFiltros.getChildren().add(btnReload);

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