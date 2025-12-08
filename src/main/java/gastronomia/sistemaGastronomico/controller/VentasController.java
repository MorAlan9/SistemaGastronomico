package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VentasController {

    private final PedidoRepository pedidoRepo;
    private List<Pedido> listaMaestra; // Aquí guardamos TODO el historial

    // Filtros
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    // Tabla
    @FXML private TableView<Pedido> tablaVentas;
    @FXML private TableColumn<Pedido, Long> colId;
    @FXML private TableColumn<Pedido, String> colFecha;
    @FXML private TableColumn<Pedido, String> colMesa; // Mostrará el ID/Numero
    @FXML private TableColumn<Pedido, String> colMozo;
    @FXML private TableColumn<Pedido, String> colMetodo;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;

    // Total
    @FXML private Label lblTotalDia;

    public VentasController(PedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    @FXML
    public void initialize() {
        configurarColumnas();

        // Configuramos fechas iniciales (Hoy)
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        cargarDatosBase(); // Trae todo de la BD
        filtrar(); // Aplica el filtro de "Hoy" al iniciar
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));

        // Fecha y Hora formateada
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getFecha() + " " + cell.getValue().getHora().toString().substring(0, 5)));

        // ID DE MESA (Lo que pediste)
        colMesa.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getMesa().getNumero()) // Muestra solo el número (Ej: "8")
        ));

        colMozo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getMozo().getNombre()));
    }

    private void cargarDatosBase() {
        // Traemos solo los cerrados, ordenados por el más reciente
        listaMaestra = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO");
    }

    @FXML
    public void filtrar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        // Filtramos la lista maestra sin molestar a la base de datos
        List<Pedido> resultadosFiltrados = listaMaestra.stream()
                .filter(p -> {
                    LocalDate fechaPedido = p.getFecha();
                    // Lógica: Fecha >= Desde Y Fecha <= Hasta
                    return (fechaPedido.isEqual(desde) || fechaPedido.isAfter(desde)) &&
                            (fechaPedido.isEqual(hasta) || fechaPedido.isBefore(hasta));
                })
                .collect(Collectors.toList());

        actualizarTablaYTotal(resultadosFiltrados);
    }

    @FXML
    public void limpiarFiltros() {
        // Reiniciar fechas y mostrar todo el historial
        dpDesde.setValue(null);
        dpHasta.setValue(null);

        // Si no hay fechas, mostramos todo
        actualizarTablaYTotal(listaMaestra);
    }

    private void actualizarTablaYTotal(List<Pedido> lista) {
        tablaVentas.getItems().setAll(lista);

        BigDecimal sumaTotal = lista.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalDia.setText("$ " + sumaTotal);
    }
}