package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.DetallePedidoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.DetallePedido;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class CobrarController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;

    // Elementos FXML
    @FXML private Label lblTitulo;
    @FXML private Label lblTotal;
    @FXML private ToggleGroup grupoPago;
    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTarjeta;
    @FXML private ToggleButton btnQR;
    @FXML private TextField txtDescuentoPorc;
    @FXML private Label lblMontoDescuento;
    @FXML private TextField txtAbonaCon;
    @FXML private Label lblVuelto;

    // Variables Internas
    private Pedido pedidoActual;
    private List<DetallePedido> itemsAPagar;
    private BigDecimal totalOriginal = BigDecimal.ZERO;
    private BigDecimal totalFinal = BigDecimal.ZERO;

    // Constructor con Inyección de Dependencias
    public CobrarController(PedidoService pedidoService,
                            PedidoRepository pedidoRepo,
                            DetallePedidoRepository detalleRepo) {
        this.pedidoService = pedidoService;
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
    }

    @FXML
    public void initialize() {
        // Listeners para cálculos en tiempo real
        if (txtAbonaCon != null) txtAbonaCon.textProperty().addListener((obs, oldVal, newVal) -> calcularVuelto());
        if (txtDescuentoPorc != null) txtDescuentoPorc.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotal());

        // Listener para cambio de método de pago (Bloquea "Abona con" si es Tarjeta/QR)
        grupoPago.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ToggleButton btn = (ToggleButton) newVal;

            if (btn == btnTarjeta || btn == btnQR) {
                txtAbonaCon.setText(totalFinal.toString());
                txtAbonaCon.setEditable(false);
                lblTotal.requestFocus();
            } else {
                txtAbonaCon.setText("");
                txtAbonaCon.setEditable(true);
                Platform.runLater(() -> txtAbonaCon.requestFocus());
            }
        });
    }

    // --- MÉTODO QUE RECIBE LOS DATOS ---
    public void iniciarCobro(Pedido pedido, List<DetallePedido> itemsSeleccionados) {
        this.pedidoActual = pedido;
        this.itemsAPagar = itemsSeleccionados;

        // 1. Calcular suma SOLAMENTE de los items recibidos
        this.totalOriginal = BigDecimal.ZERO;

        if (itemsAPagar != null && !itemsAPagar.isEmpty()) {
            for (DetallePedido d : itemsAPagar) {
                BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
                this.totalOriginal = this.totalOriginal.add(subtotal);
            }
        } else {
            // Fallback por seguridad: Si no llega lista, usamos el total del pedido
            this.totalOriginal = pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO;
        }

        // 2. Configurar Vista
        lblTitulo.setText("MESA " + pedido.getMesa().getNumero());
        txtDescuentoPorc.setText("0");
        recalcularTotal();
        Platform.runLater(() -> txtAbonaCon.requestFocus());

        // 3. Configurar Eventos de Teclado (Enter y Escape)
        Platform.runLater(() -> {
            if (lblTitulo.getScene() != null) {
                lblTitulo.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        finalizarVenta();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        cerrar();
                        event.consume();
                    }
                });
            }
        });
    }

    private void recalcularTotal() {
        BigDecimal porcentaje = BigDecimal.ZERO;
        try {
            String txt = txtDescuentoPorc.getText().trim();
            if (!txt.isEmpty()) porcentaje = new BigDecimal(txt);
        } catch (NumberFormatException e) { }

        if (porcentaje.compareTo(new BigDecimal("100")) > 0) porcentaje = new BigDecimal("100");

        BigDecimal descuento = totalOriginal.multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        totalFinal = totalOriginal.subtract(descuento);

        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            lblMontoDescuento.setText("(-$" + descuento.setScale(0, RoundingMode.HALF_UP) + ")");
            lblMontoDescuento.setVisible(true);
        } else {
            lblMontoDescuento.setVisible(false);
        }

        lblTotal.setText("$" + totalFinal.setScale(0, RoundingMode.HALF_UP));

        // Si es tarjeta/QR actualizamos el campo de abono automáticamente
        if (grupoPago.getSelectedToggle() == btnTarjeta || grupoPago.getSelectedToggle() == btnQR) {
            txtAbonaCon.setText(totalFinal.setScale(0, RoundingMode.HALF_UP).toString());
        }
        calcularVuelto();
    }

    private void calcularVuelto() {
        try {
            String textoPago = txtAbonaCon.getText();
            if (textoPago == null || textoPago.trim().isEmpty()) {
                lblVuelto.setText("$0.00");
                return;
            }
            BigDecimal abonaCon = new BigDecimal(textoPago.trim());
            BigDecimal vuelto = abonaCon.subtract(totalFinal);
            lblVuelto.setText("$" + vuelto.setScale(0, RoundingMode.HALF_UP));

            if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
                lblVuelto.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else {
                lblVuelto.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            }
        } catch (NumberFormatException e) {
            lblVuelto.setText("---");
        }
    }

    @FXML
    public void finalizarVenta() {
        try {
            ToggleButton seleccionado = (ToggleButton) grupoPago.getSelectedToggle();
            String textoBoton = seleccionado != null ? seleccionado.getText() : "Efectivo";

            // Limpiar texto para guardar en BD
            String metodoLimpio = "Efectivo";
            if (textoBoton.toLowerCase().contains("tarjeta")) metodoLimpio = "Tarjeta";
            else if (textoBoton.toLowerCase().contains("qr")) metodoLimpio = "QR / MP";

            // --- LÓGICA DE COBRO INTELIGENTE ---

            // 1. Averiguar si estamos pagando TODO el pedido o solo una PARTE
            // (Requiere que hayas agregado countByPedido en DetallePedidoRepository)
            long totalItemsEnMesa = detalleRepo.countByPedido(pedidoActual);

            // Si la lista es nula o tiene la misma cantidad de items que la mesa, es total.
            boolean esPagoTotal = (itemsAPagar == null) || (itemsAPagar.size() >= totalItemsEnMesa);

            if (esPagoTotal) {
                // CASO 1: Pago Total -> Cerramos el pedido actual normalmente
                System.out.println("Pago Total detectado.");
                pedidoService.cobrarPedido(pedidoActual.getId(), metodoLimpio, totalFinal);

            } else {
                // CASO 2: Pago Parcial (Split)
                System.out.println("Pago Parcial detectado. Dividiendo items...");

                // A) Crear un nuevo pedido "Factura" que nace ya cerrado
                Pedido pedidoFactura = new Pedido();
                pedidoFactura.setMesa(pedidoActual.getMesa());
                pedidoFactura.setMozo(pedidoActual.getMozo()); // Mismo mozo
                pedidoFactura.setFecha(LocalDate.now());
                pedidoFactura.setHora(LocalTime.now());
                pedidoFactura.setEstado("CERRADO"); // Nace cerrado
                pedidoFactura.setTotal(totalFinal);
                pedidoFactura.setMetodoPago(metodoLimpio); // Asegurate de tener este campo en Pedido

                // OPCIONAL: Si agregaste idPedidoPadre en el Modelo, descomenta esto:
                 pedidoFactura.setIdPedidoPadre(pedidoActual.getId());

                // Guardamos el pedido nuevo para tener ID
                Pedido pedidoGuardado = pedidoRepo.save(pedidoFactura);

                // B) Mover los items seleccionados del pedido Viejo al Nuevo
                for (DetallePedido item : itemsAPagar) {
                    item.setPedido(pedidoGuardado); // Cambiamos de dueño
                    detalleRepo.save(item);
                }
            }

            cerrar();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR, "Error al cobrar: " + e.getMessage());
            error.show();
        }
    }

    @FXML
    public void cerrar() {
        try {
            ((Stage) lblTotal.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}