package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class AbrirMesaController {

    private final MozoRepository mozoRepo;
    private final PedidoRepository pedidoRepo;
    private final ApplicationContext context; // Necesario para abrir la siguiente ventana

    private Mesa mesaSeleccionada;

    @FXML private Label lblTituloMesa;
    @FXML private Spinner<Integer> spinnerPersonas;
    @FXML private ComboBox<Mozo> comboMozos;
    @FXML private TextArea txtComentario;

    public AbrirMesaController(MozoRepository mozoRepo, PedidoRepository pedidoRepo, ApplicationContext context) {
        this.mozoRepo = mozoRepo;
        this.pedidoRepo = pedidoRepo;
        this.context = context;
    }

    @FXML
    public void initialize() {
        // 1. Configurar el contador de personas (Min 1, Max 20)
        spinnerPersonas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));

        // 2. Cargar lista de camareros
        comboMozos.getItems().setAll(mozoRepo.findAll());

        // Seleccionar el primero por defecto para agilizar
        if (!comboMozos.getItems().isEmpty()) {
            comboMozos.getSelectionModel().selectFirst();
        }
    }

    public void setMesa(Mesa mesa) {
        this.mesaSeleccionada = mesa;
        lblTituloMesa.setText("MESA " + mesa.getNumero());
    }

    /**
     * ACCIÓN DEL BOTÓN "ABRIR MESA"
     */
    @FXML
    public void confirmarApertura() {
        try {
            // A. Validar datos
            Mozo mozo = comboMozos.getValue();
            if (mozo == null) {
                mostrarAlerta("Faltan datos", "Por favor seleccione un camarero.");
                return;
            }

            // B. Crear el Pedido en la Base de Datos
            Pedido nuevoPedido = new Pedido();
            nuevoPedido.setMesa(mesaSeleccionada);
            nuevoPedido.setMozo(mozo);
            nuevoPedido.setComensales(spinnerPersonas.getValue());
            nuevoPedido.setComentarios(txtComentario.getText());
            nuevoPedido.setFecha(LocalDate.now());
            nuevoPedido.setHora(LocalTime.now());
            nuevoPedido.setEstado("ABIERTO");
            nuevoPedido.setTotal(BigDecimal.ZERO);

            pedidoRepo.save(nuevoPedido); // ¡GUARDADO! La mesa ahora figura ocupada

            // C. Cerrar esta ventanita pequeña
            Stage stageActual = (Stage) lblTituloMesa.getScene().getWindow();
            stageActual.close();

            // D. Abrir inmediatamente la pantalla grande de productos
            abrirPantallaPedido(mesaSeleccionada);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la mesa.");
        }
    }

    // Método privado para saltar a la siguiente pantalla
    private void abrirPantallaPedido(Mesa mesa) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/pedido.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            // Pasamos la mesa al siguiente controlador (TomaPedidoController)
            TomaPedidoController controller = loader.getController();
            controller.setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Pedido - Mesa " + mesa.getNumero());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atención");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}