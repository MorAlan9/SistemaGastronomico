package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CobrarController {

    private final PedidoService pedidoService;

    @FXML private Label lblTitulo;
    @FXML private Label lblTotal;

    @FXML private ToggleGroup grupoPago;
    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTarjeta;
    @FXML private ToggleButton btnQR;

    // Cambiamos CheckBox por TextField
    @FXML private TextField txtDescuentoPorc;
    @FXML private Label lblMontoDescuento;

    @FXML private TextField txtAbonaCon;
    @FXML private Label lblVuelto;

    private Pedido pedidoActual;
    private BigDecimal totalOriginal = BigDecimal.ZERO;
    private BigDecimal totalFinal = BigDecimal.ZERO;

    public CobrarController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @FXML
    public void initialize() {
        // 1. Calcular Vuelto al escribir monto
        if (txtAbonaCon != null) {
            txtAbonaCon.textProperty().addListener((obs, oldVal, newVal) -> calcularVuelto());
        }

        // 2. Recalcular Total al escribir porcentaje de descuento
        if (txtDescuentoPorc != null) {
            txtDescuentoPorc.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotal());
        }

        // 3. UX INTELIGENTE: Si selecciona Tarjeta/QR, autocompletar el monto exacto
        grupoPago.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ToggleButton btn = (ToggleButton) newVal;

            if (btn == btnTarjeta || btn == btnQR) {
                // Pago electrónico: asumimos pago exacto
                txtAbonaCon.setText(totalFinal.toString());
                txtAbonaCon.setEditable(false); // Bloqueamos para evitar errores
            } else {
                // Efectivo: limpiamos para que calcule vuelto
                txtAbonaCon.setText("");
                txtAbonaCon.setEditable(true);
                Platform.runLater(() -> txtAbonaCon.requestFocus());
            }
        });
    }

    public void setPedido(Pedido pedido) {
        this.pedidoActual = pedido;
        this.totalOriginal = pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO;

        lblTitulo.setText("MESA " + pedido.getMesa().getNumero());

        // Inicializar
        txtDescuentoPorc.setText("0"); // 0% por defecto
        recalcularTotal();

        // Foco inicial
        Platform.runLater(() -> txtAbonaCon.requestFocus());
    }

    private void recalcularTotal() {
        // Obtener porcentaje del campo de texto
        BigDecimal porcentaje = BigDecimal.ZERO;
        try {
            String txt = txtDescuentoPorc.getText().trim();
            if (!txt.isEmpty()) {
                porcentaje = new BigDecimal(txt);
            }
        } catch (NumberFormatException e) {
            // Si escribe letras, ignoramos
        }

        // Validar que no sea mayor a 100%
        if (porcentaje.compareTo(new BigDecimal("100")) > 0) porcentaje = new BigDecimal("100");

        // Calcular descuento
        BigDecimal descuento = totalOriginal.multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        totalFinal = totalOriginal.subtract(descuento);

        // Mostrar
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            lblMontoDescuento.setText("(-$" + descuento.setScale(0, RoundingMode.HALF_UP) + ")");
            lblMontoDescuento.setVisible(true);
        } else {
            lblMontoDescuento.setVisible(false);
        }

        lblTotal.setText("$" + totalFinal.setScale(0, RoundingMode.HALF_UP));

        // Si estaba en modo Tarjeta, actualizar el monto de pago también
        if (btnTarjeta.isSelected() || btnQR.isSelected()) {
            txtAbonaCon.setText(totalFinal.setScale(0, RoundingMode.HALF_UP).toString());
        }

        calcularVuelto();
    }

    private void calcularVuelto() {
        try {
            String textoPago = txtAbonaCon.getText();
            if (textoPago == null || textoPago.trim().isEmpty()) {
                lblVuelto.setText("$0.00");
                lblVuelto.setStyle("-fx-text-fill: black;");
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
        // Validación extra antes de cerrar
        try {
            String textoPago = txtAbonaCon.getText();
            BigDecimal abonaCon = (textoPago.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(textoPago);

            // Si paga con menos de lo que debe, advertir
            if (abonaCon.compareTo(totalFinal) < 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Pago Incompleto");
                alert.setHeaderText("El monto ingresado es menor al total.");
                alert.setContentText("¿Desea cerrar la mesa como 'Pago Parcial' o corregir?");

                ButtonType btnCorregir = new ButtonType("Corregir");
                ButtonType btnCerrarIgual = new ButtonType("Cerrar Igual");
                alert.getButtonTypes().setAll(btnCorregir, btnCerrarIgual);

                if (alert.showAndWait().get() == btnCorregir) {
                    return; // Cancela el cierre
                }
            }

            // Proceso normal
            ToggleButton seleccionado = (ToggleButton) grupoPago.getSelectedToggle();
            String textoBoton = seleccionado != null ? seleccionado.getText() : "Efectivo";
            String metodoLimpio = "Efectivo";
            if (textoBoton.contains("Tarjeta")) metodoLimpio = "Tarjeta";
            else if (textoBoton.contains("QR")) metodoLimpio = "QR / MP";

            pedidoService.cobrarPedido(pedidoActual.getId(), metodoLimpio, totalFinal);

            cerrar();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setContentText("Error: " + e.getMessage());
            error.show();
        }
    }

    @FXML
    public void cerrar() {
        ((Stage) lblTotal.getScene().getWindow()).close();
    }
}