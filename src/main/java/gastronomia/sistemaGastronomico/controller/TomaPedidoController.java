package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.*;
import gastronomia.sistemaGastronomico.model.*;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
    private final MesaRepository mesaRepo; // <-- NUEVA DEPENDENCIA

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
                                MesaRepository mesaRepo) { // <-- INYECTAMOS EN CONSTRUCTOR
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.mozoRepo = mozoRepo;
        this.categoriaRepo = categoriaRepo;
        this.pedidoService = pedidoService;
        this.mesaRepo = mesaRepo;
    }

    @FXML
    public void initialize() {
        generarBotonesFiltros();
        cargarProductos(productoRepo.findByActivoTrue());

        Label placeholder = new Label("🛒 Seleccione productos...\n(F5 para Marchar)");
        placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-text-alignment: CENTER;");
        listaItems.setPlaceholder(placeholder);

        // CONFIGURACIÓN DE TECLAS (F2, F3, F4, F5, ESC)
        Platform.runLater(() -> {
            if (btnComandar.getScene() != null) {
                // F5: Comandar
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::accionComandar);
                // F4: Cobrar
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F4), this::accionCobrar);
                // ESC: Volver
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::accionVolver);

                // NUEVO -> F3: Ir al Buscador de Productos
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F3), this::accionFocoBuscador);

                // NUEVO -> F2: Buscar/Cambiar Mesa
                btnComandar.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F2), this::accionBuscarMesa);
            }
        });

        // 1. DOBLE CLIC NOTAS
        listaItems.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) editarObservacion();
        });

        // 2. BUSCADOR TIEMPO REAL
        if (txtBuscarProducto != null) {
            txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> {
                filtrarProductos(newVal);
            });
        }
    }

    // --- ACCIÓN: FOCO AL BUSCADOR (F3) ---
    @FXML
    public void accionFocoBuscador() {
        txtBuscarProducto.requestFocus();
        txtBuscarProducto.selectAll();
    }

    // --- ACCIÓN: BUSCAR / CAMBIAR MESA (F2) ---
    @FXML
    public void accionBuscarMesa() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cambiar Mesa");
        dialog.setHeaderText("Saltar a otra mesa");
        dialog.setContentText("Ingrese N° de Mesa:");

        // Estilo del diálogo
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);

                if (mesaOpt.isPresent()) {
                    setMesa(mesaOpt.get()); // ¡Recarga todo con la nueva mesa!
                    toast("🔀 Cambiado a Mesa " + numero, lblTituloMesa);
                } else {
                    error("Error", "La mesa " + numero + " no existe.");
                }
            } catch (NumberFormatException e) {
                error("Error", "Ingrese un número válido.");
            }
        });
    }

    // --- LÓGICA DE FILTRADO ---
    private void filtrarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProductos(productoRepo.findByActivoTrue());
            return;
        }
        String busqueda = texto.toLowerCase();
        List<Producto> todos = productoRepo.findByActivoTrue();

        List<Producto> filtrados = todos.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(busqueda))
                .collect(Collectors.toList());

        cargarProductos(filtrados);
    }

    private void editarObservacion() {
        DetallePedido item = listaItems.getSelectionModel().getSelectedItem();
        if (item == null) return;
        TextInputDialog dialog = new TextInputDialog(item.getObservacion() != null ? item.getObservacion() : "");
        dialog.setTitle("Nota para Cocina/Barra");
        dialog.setHeaderText("Agregar observación a: " + item.getProducto().getNombre());
        dialog.setContentText("Nota (ej: Sin sal, A punto):");
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
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
        List<DetallePedido> nuevos = detalles.stream()
                .filter(d -> d.getHoraMarchar() == null)
                .collect(Collectors.toList());

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

            if (hayCocina) {
                pedidoActual.setHoraComanda(ahora);
                toast("♨ Marchando a Cocina", btnComandar);
            } else {
                toast("🍺 Marchando a Barra", btnComandar);
            }

            pedidoActual.setHoraUltimoProducto(ahora);
            pedidoRepo.save(pedidoActual);

            actualizarEstadoBotonComandar();
            actualizarVistaPedido();

        } catch (Exception e) {
            error("Error", "No se pudo comandar: " + e.getMessage());
        }
    }

    private void actualizarEstadoBotonComandar() {
        if (pedidoActual == null) return;
        boolean hayNuevos = detalleRepo.findByPedido(pedidoActual).stream()
                .anyMatch(d -> d.getHoraMarchar() == null);

        if (hayNuevos) {
            btnComandar.setDisable(false);
            btnComandar.setText("♨ MARCHAR NUEVOS (F5)");
            btnComandar.setStyle("");
            btnComandar.getStyleClass().add("btn-comandar");
        } else {
            btnComandar.setDisable(true);
            boolean cocinaTrabajando = false;
            if (pedidoActual.getHoraComanda() != null) {
                if (pedidoActual.getHoraEntrega() == null) cocinaTrabajando = true;
                else if (pedidoActual.getHoraComanda().isAfter(pedidoActual.getHoraEntrega())) cocinaTrabajando = true;
            }

            if (cocinaTrabajando) {
                btnComandar.setText("🕒 EN COCINA");
                btnComandar.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
            } else if (pedidoActual.getHoraEntrega() != null) {
                btnComandar.setText("🍽 MESA COMIENDO");
                btnComandar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
            } else {
                btnComandar.setText("ESPERANDO PEDIDO");
                btnComandar.setStyle("");
            }
        }
    }

    @FXML
    public void accionEliminarItem() {
        DetallePedido seleccionado = listaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            toast("Selecciona un producto", listaItems);
            return;
        }
        if (seleccionado.getHoraMarchar() != null) {
            advertencia("Bloqueado", "Este ítem ya fue marchado.\nNo se puede eliminar.");
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

    private void actualizarVistaPedido() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedidoActual);
        detalles.sort(Comparator.comparing(DetallePedido::getHoraMarchar, Comparator.nullsLast(Comparator.naturalOrder())));

        listaItems.getItems().setAll(detalles);
        listaItems.getStyleClass().add("ticket-list");

        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (DetallePedido d : detalles) {
            BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
            totalCalculado = totalCalculado.add(subtotal);
        }
        lblTotal.setText("TOTAL: $" + totalCalculado);
        if (pedidoActual != null) pedidoActual.setTotal(totalCalculado);

        listaItems.setCellFactory(param -> new ListCell<DetallePedido>() {
            @Override
            protected void updateItem(DetallePedido item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    HBox fila = new HBox(10); fila.setAlignment(Pos.CENTER_LEFT);

                    boolean esNuevo = item.getHoraMarchar() == null;
                    boolean esCocina = item.getProducto().isEsCocina();

                    String icono = esNuevo ? "🆕" : (esCocina ? "♨️" : "🍸");
                    Label lblIcono = new Label(icono);
                    lblIcono.setMinWidth(25);

                    Label lblCant = new Label(item.getCantidad() + "x");
                    lblCant.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-min-width: 25px;");

                    Label lblNom = new Label(item.getProducto().getNombre());
                    VBox vBoxNombre = new VBox(lblNom);
                    vBoxNombre.setAlignment(Pos.CENTER_LEFT);

                    if (esNuevo) {
                        lblNom.setStyle("-fx-text-fill: #000; -fx-font-weight: bold; -fx-font-size: 13px;");
                    } else {
                        lblNom.setStyle("-fx-text-fill: #7f8c8d;");
                    }

                    if (item.getObservacion() != null && !item.getObservacion().trim().isEmpty()) {
                        Label lblNota = new Label("📝 " + item.getObservacion());
                        lblNota.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 11px; -fx-font-style: italic;");
                        vBoxNombre.getChildren().add(lblNota);
                    }

                    Label lblHora = new Label();
                    if (!esNuevo && item.getHoraMarchar() != null) {
                        String horaStr = item.getHoraMarchar().format(DateTimeFormatter.ofPattern("HH:mm"));
                        lblHora.setText(horaStr);
                        lblHora.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
                    }

                    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                    String subtotal = "$" + item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad()));
                    Label lblPrecio = new Label(subtotal);
                    lblPrecio.setStyle("-fx-font-weight: bold;");

                    fila.getChildren().addAll(lblIcono, lblCant, vBoxNombre, spacer, lblHora, lblPrecio);
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

    @FXML
    public void accionCobrar() {
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
                String totalStr = lblTotal.getText().replace("TOTAL: $", "").trim();
                BigDecimal totalFinal = new BigDecimal(totalStr);

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
        ((Stage) lblTotal.getScene().getWindow()).close();
    }
}