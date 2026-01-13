package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.service.PedidoService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReporteMozoController extends BaseController {

    private final MozoRepository mozoRepo;
    private final PedidoService pedidoService;

    @FXML private ComboBox<Mozo> comboMozos;
    @FXML private TextArea txtResultado;

    public ReporteMozoController(MozoRepository mozoRepo, PedidoService pedidoService) {
        this.mozoRepo = mozoRepo;
        this.pedidoService = pedidoService;
    }

    @FXML
    public void initialize() {
        cargarMozos();

        // Convertidor para que el combo muestre solo el nombre
        comboMozos.setConverter(new StringConverter<Mozo>() {
            @Override
            public String toString(Mozo m) {
                return m != null ? m.getNombre() : "";
            }
            @Override
            public Mozo fromString(String string) { return null; }
        });
    }

    private void cargarMozos() {
        List<Mozo> lista = mozoRepo.findAll();
        comboMozos.getItems().setAll(lista);
        if (!lista.isEmpty()) comboMozos.getSelectionModel().selectFirst();
    }

    @FXML
    public void generarReporte() {
        Mozo seleccionado = comboMozos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            txtResultado.setText("Por favor seleccione un mozo.");
            return;
        }

        try {
            String reporte = pedidoService.generarReporteMozo(seleccionado);
            txtResultado.setText(reporte);
        } catch (Exception e) {
            txtResultado.setText("Error al calcular: " + e.getMessage());
        }
    }

    @FXML
    public void cerrar() {
        ((Stage) txtResultado.getScene().getWindow()).close();
    }
}