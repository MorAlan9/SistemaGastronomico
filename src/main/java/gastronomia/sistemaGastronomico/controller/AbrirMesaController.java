package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
// SIN IMPORTS DE ALERTAHELPER
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class AbrirMesaController extends BaseController { // <--- 1. HERENCIA APLICADA

    private final MozoRepository mozoRepo;
    private final PedidoRepository pedidoRepo;
    private final ApplicationContext context;
    private static Mozo ultimoMozoSeleccionado = null;
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
        spinnerPersonas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 2));
        comboMozos.getItems().setAll(mozoRepo.findAll());
        if (ultimoMozoSeleccionado != null && comboMozos.getItems().contains(ultimoMozoSeleccionado)) {
            comboMozos.getSelectionModel().select(ultimoMozoSeleccionado);
        } else if (!comboMozos.getItems().isEmpty()) {
            comboMozos.getSelectionModel().selectFirst();
        }
    }

    public void setMesa(Mesa mesa) {
        this.mesaSeleccionada = mesa;
        if (lblTituloMesa != null) lblTituloMesa.setText("MESA " + mesa.getNumero());
    }

    @FXML public void set1Persona() { spinnerPersonas.getValueFactory().setValue(1); }
    @FXML public void set2Personas() { spinnerPersonas.getValueFactory().setValue(2); }
    @FXML public void set4Personas() { spinnerPersonas.getValueFactory().setValue(4); }
    @FXML public void set6Personas() { spinnerPersonas.getValueFactory().setValue(6); }

    @FXML
    public void confirmarApertura() {
        try {
            Mozo mozo = comboMozos.getValue();
            if (mozo == null) {
                advertencia("Atención", "Seleccione un camarero.");
                return;
            }

            ultimoMozoSeleccionado = mozo;

            Pedido pedido = new Pedido();
            pedido.setMesa(mesaSeleccionada);
            pedido.setMozo(mozo);
            pedido.setComensales(spinnerPersonas.getValue());

            pedido.setFecha(LocalDate.now());
            pedido.setHora(LocalTime.now());
            pedido.setEstado("ABIERTO");
            pedido.setTotal(BigDecimal.ZERO);

            pedidoRepo.save(pedido);

            if (lblTituloMesa.getScene() != null) {
                ((Stage) lblTituloMesa.getScene().getWindow()).close();
            }

            abrirPantallaPedido(mesaSeleccionada);

        } catch (Exception e) {
            e.printStackTrace();
            error("Error", "Error al abrir mesa: " + e.getMessage());
        }
    }

    private void abrirPantallaPedido(Mesa mesa) {
        try {
            URL url = getClass().getResource("/Views/pedido.fxml");
            if (url == null) throw new RuntimeException("Falta /Views/pedido.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            TomaPedidoController controller = loader.getController();
            if (controller != null) controller.setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Pedido - Mesa " + mesa.getNumero());

            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm()); }
            catch (Exception ignored) {}

            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            error("Error", "No se pudo cargar la vista de pedidos.");
        }
    }
}