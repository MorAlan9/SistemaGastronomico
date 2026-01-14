package gastronomia.sistemaGastronomico.controller;

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

@Component
public class CobrarController {

    private final PedidoService pedidoService;

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

    private Pedido pedidoActual;
    private BigDecimal totalOriginal = BigDecimal.ZERO;
    private BigDecimal totalFinal = BigDecimal.ZERO;

    public CobrarController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @FXML
    public void initialize() {
        if (txtAbonaCon != null) txtAbonaCon.textProperty().addListener((obs, oldVal, newVal) -> calcularVuelto());
        if (txtDescuentoPorc != null) txtDescuentoPorc.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotal());

        // UX INTELIGENTE
        grupoPago.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ToggleButton btn = (ToggleButton) newVal;

            if (btn == btnTarjeta || btn == btnQR) {
                txtAbonaCon.setText(totalFinal.toString());
                txtAbonaCon.setEditable(false);
                // IMPORTANTE: Quitamos el foco del botón para que no interfiera
                lblTotal.requestFocus();
            } else {
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
        txtDescuentoPorc.setText("0");
        recalcularTotal();
        Platform.runLater(() -> txtAbonaCon.requestFocus());

        // --- SOLUCIÓN GLOBAL PARA TECLA ENTER ---
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
                return;
            }
            BigDecimal abonaCon = new BigDecimal(textoPago.trim());
            BigDecimal vuelto = abonaCon.subtract(totalFinal);
            lblVuelto.setText("$" + vuelto.setScale(0, RoundingMode.HALF_UP));

            if (vuelto.compareTo(BigDecimal.ZERO) < 0) lblVuelto.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            else lblVuelto.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } catch (NumberFormatException e) {
            lblVuelto.setText("---");
        }
    }

    @FXML
    public void finalizarVenta() {
        try {
            ToggleButton seleccionado = (ToggleButton) grupoPago.getSelectedToggle();
            String textoBoton = seleccionado != null ? seleccionado.getText() : "Efectivo";
            String metodoLimpio = "Efectivo";
            if (textoBoton.toLowerCase().contains("tarjeta")) metodoLimpio = "Tarjeta";
            else if (textoBoton.toLowerCase().contains("qr")) metodoLimpio = "QR / MP";

            pedidoService.cobrarPedido(pedidoActual.getId(), metodoLimpio, totalFinal);
            cerrar();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
            error.show();
        }
    }

    @FXML
    public void cerrar() {
        ((Stage) lblTotal.getScene().getWindow()).close();
    }
}