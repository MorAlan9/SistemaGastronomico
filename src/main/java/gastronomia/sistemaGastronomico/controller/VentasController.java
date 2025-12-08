package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VentasController {

    private final PedidoRepository pedidoRepo;

    @FXML private TableView<Pedido> tablaVentas;
    @FXML private TableColumn<Pedido, Long> colId;
    @FXML private TableColumn<Pedido, String> colFecha;
    @FXML private TableColumn<Pedido, String> colMesa;
    @FXML private TableColumn<Pedido, String> colMozo;
    @FXML private TableColumn<Pedido, String> colMetodo;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;
    @FXML private Label lblTotalDia;

    public VentasController(PedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarDatos();
    }

    private void configurarColumnas() {
        // Datos simples
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));

        // Datos combinados (Fecha + Hora)
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getFecha() + " " + cell.getValue().getHora().toString().substring(0, 5)));

        // Datos de relaciones (Mesa y Mozo)
        colMesa.setCellValueFactory(cell -> new SimpleStringProperty(
                "Mesa " + cell.getValue().getMesa().getNumero()));

        colMozo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getMozo().getNombre()));
    }

    private void cargarDatos() {
        // 1. Buscamos solo los pedidos CERRADOS
        List<Pedido> ventas = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO");

        // 2. Llenamos la tabla
        tablaVentas.getItems().setAll(ventas);

        // 3. Sumamos el total
        BigDecimal sumaTotal = ventas.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalDia.setText("$ " + sumaTotal);
    }
}