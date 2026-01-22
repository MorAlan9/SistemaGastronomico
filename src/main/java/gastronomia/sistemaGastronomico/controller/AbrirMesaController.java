package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo; // <--- USAMOS MOZO
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.utils.SesionGlobal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal; // <--- IMPORTANTE
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class AbrirMesaController extends BaseController {

    private final MozoRepository mozoRepo;
    private final PedidoRepository pedidoRepo;
    private final ApplicationContext context;

    private Mesa mesaSeleccionada;

    @FXML private Label lblTituloMesa;
    @FXML private Spinner<Integer> spinnerPersonas;
    @FXML private ComboBox<Mozo> comboMozos; // <--- COMBO DE MOZOS
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

        if (SesionGlobal.mozoActual != null) {
            comboMozos.getSelectionModel().select(SesionGlobal.mozoActual);
            comboMozos.setDisable(true);
        } else {
            if (!comboMozos.getItems().isEmpty()) comboMozos.getSelectionModel().selectFirst();
        }
    }

    public void setMesa(Mesa mesa) {
        this.mesaSeleccionada = mesa;
        if (lblTituloMesa != null) lblTituloMesa.setText("MESA " + mesa.getNumero());
    }

    @FXML
    public void confirmarApertura() {
        try {
            Mozo mozo = comboMozos.getValue();
            if (mozo == null) {
                advertencia("Error", "Seleccione un mozo.");
                return;
            }

            Pedido pedido = new Pedido();
            pedido.setMesa(mesaSeleccionada);
            pedido.setMozo(mozo); // <--- AHORA COINCIDE (Mozo con Mozo)
            pedido.setComensales(spinnerPersonas.getValue());

            // CORRECCIÓN DE FECHAS
            pedido.setFecha(LocalDate.now());
            pedido.setHora(LocalTime.now());
            pedido.setEstado("ABIERTO");

            // CORRECCIÓN DE DINERO (BigDecimal)
            pedido.setTotal(BigDecimal.ZERO);

            // Si tu Pedido.java no tiene setObservaciones, comenta esta línea:
            // pedido.setObservaciones(txtComentario.getText());

            pedidoRepo.save(pedido);

            SesionGlobal.mozoActual = null;
            ((Stage) lblTituloMesa.getScene().getWindow()).close();
            abrirPantallaPedido(mesaSeleccionada);

        } catch (Exception e) {
            e.printStackTrace();
            error("Error", "Error al abrir mesa.");
        }
    }

    // ... agrega aquí los métodos set1Persona, set2Personas, etc ...
    @FXML public void set1Persona() { spinnerPersonas.getValueFactory().setValue(1); }
    @FXML public void set2Personas() { spinnerPersonas.getValueFactory().setValue(2); }
    @FXML public void set4Personas() { spinnerPersonas.getValueFactory().setValue(4); }
    @FXML public void set6Personas() { spinnerPersonas.getValueFactory().setValue(6); }

    private void abrirPantallaPedido(Mesa mesa) {
        try {
            URL url = getClass().getResource("/Views/pedido.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Object controller = loader.getController();
            try { controller.getClass().getMethod("setMesa", Mesa.class).invoke(controller, mesa); } catch(Exception ignored){}
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm()); } catch(Exception ignored){}
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}