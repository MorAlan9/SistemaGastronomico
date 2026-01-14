package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MovimientoCajaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser; // IMPORTANTE: Para elegir dónde guardar
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class VentasController {

    private final PedidoRepository pedidoRepo;
    private final MovimientoCajaRepository cajaRepo;
    private final ApplicationContext context;

    // --- PESTAÑA 1: VENTAS ---
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TableView<Pedido> tablaVentas;
    @FXML private TableColumn<Pedido, Long> colId;
    @FXML private TableColumn<Pedido, String> colFecha;
    @FXML private TableColumn<Pedido, String> colMesa;
    @FXML private TableColumn<Pedido, String> colMozo;
    @FXML private TableColumn<Pedido, String> colMetodo;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;
    @FXML private Label lblTotalDia;
    @FXML private Label lblCantOperaciones;

    // --- PESTAÑA 2: CAJA ---
    @FXML private Label lblSaldoCaja;
    @FXML private Label lblTotalIngresos;
    @FXML private Label lblTotalEgresos;
    @FXML private TableView<MovimientoCaja> tablaMovimientos;
    @FXML private TableColumn<MovimientoCaja, String> colMovFecha;
    @FXML private TableColumn<MovimientoCaja, String> colMovTipo;
    @FXML private TableColumn<MovimientoCaja, String> colMovCat;
    @FXML private TableColumn<MovimientoCaja, String> colMovDesc;
    @FXML private TableColumn<MovimientoCaja, BigDecimal> colMovMonto;

    public VentasController(PedidoRepository pedidoRepo, MovimientoCajaRepository cajaRepo, ApplicationContext context) {
        this.pedidoRepo = pedidoRepo;
        this.cajaRepo = cajaRepo;
        this.context = context;
    }

    @FXML
    public void initialize() {
        configurarTablaVentas();
        configurarTablaMovimientos();

        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        filtrar();
        actualizarCaja();
    }

    private void configurarTablaVentas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getFecha() + " " + cell.getValue().getHora().toString().substring(0, 5)));
        colMesa.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getMesa().getNumero())));
        colMozo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMozo().getNombre()));

        // UX: BADGES DE COLORES
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colMetodo.setCellFactory(column -> new TableCell<Pedido, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Efectivo")) {
                        setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-alignment: CENTER; -fx-background-radius: 5; -fx-font-weight: bold;");
                    } else if (item.toLowerCase().contains("tarjeta")) {
                        setStyle("-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; -fx-alignment: CENTER; -fx-background-radius: 5; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #e2d9f3; -fx-text-fill: #512da8; -fx-alignment: CENTER; -fx-background-radius: 5; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    public void filtrar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) return;

        // 1. Consultar a la BD
        List<Pedido> todasLasVentas = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO");

        // 2. Filtrar
        List<Pedido> filtrados = todasLasVentas.stream()
                .filter(p -> (p.getFecha().isEqual(desde) || p.getFecha().isAfter(desde)) &&
                        (p.getFecha().isEqual(hasta) || p.getFecha().isBefore(hasta)))
                .collect(Collectors.toList());

        // 3. Actualizar UI
        tablaVentas.getItems().setAll(filtrados);

        BigDecimal suma = filtrados.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalDia.setText("$ " + suma);

        if (lblCantOperaciones != null) {
            lblCantOperaciones.setText(String.valueOf(filtrados.size()));
        }
    }

    @FXML
    public void limpiarFiltros() {
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());
        filtrar();
    }

    // --- REPORTE MOZOS ---
    @FXML
    public void abrirReporteMozo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ReporteMozo.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Reporte por Mozo");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- EXPORTAR EXCEL CON SELECTOR DE ARCHIVOS ---
    @FXML
    public void exportarExcel() {
        List<Pedido> datos = tablaVentas.getItems();
        if (datos.isEmpty()) {
            mostrarAlerta("Sin datos", "No hay registros para exportar en la vista actual.");
            return;
        }

        // 1. Configurar el FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte de Ventas");
        fileChooser.setInitialFileName("Ventas_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));

        // 2. Obtener la ventana actual para mostrar el diálogo
        Stage stage = (Stage) tablaVentas.getScene().getWindow();

        // 3. Mostrar diálogo "Guardar Como"
        File file = fileChooser.showSaveDialog(stage);

        // 4. Si el usuario seleccionó un archivo
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                // Encabezados
                pw.println("Nro;Fecha;Mesa;Mozo;Pago;Total");

                // Datos
                for (Pedido p : datos) {
                    pw.printf("%d;%s;%d;%s;%s;%.2f%n",
                            p.getId(), p.getFecha(), p.getMesa().getNumero(),
                            p.getMozo().getNombre(), p.getMetodoPago(), p.getTotal());
                }

                mostrarAlerta("Exportación Exitosa", "Reporte guardado en:\n" + file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlerta("Error", "No se pudo guardar el archivo: " + e.getMessage());
            }
        }
    }

    // Método auxiliar para alertas
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.show();
    }

    // --- LÓGICA DE CAJA ---
    private void configurarTablaMovimientos() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        colMovFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaHora().format(fmt)));
        colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMovCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMovDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMovMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        colMovMonto.setCellFactory(column -> new TableCell<MovimientoCaja, BigDecimal>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText("$ " + item);
                    MovimientoCaja row = getTableView().getItems().get(getIndex());
                    if ("INGRESO".equals(row.getTipo())) setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void actualizarCaja() {
        List<MovimientoCaja> movimientos = cajaRepo.findAllByOrderByFechaHoraDesc();
        tablaMovimientos.getItems().setAll(movimientos);

        BigDecimal ingresosManuales = movimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).map(MovimientoCaja::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal egresos = movimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).map(MovimientoCaja::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sumar ventas de hoy
        BigDecimal ventasHoy = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO").stream()
                .filter(p -> p.getFecha().isEqual(LocalDate.now()))
                .map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIngresos = ingresosManuales.add(ventasHoy);
        lblTotalIngresos.setText("$ " + totalIngresos);
        lblTotalEgresos.setText("$ " + egresos);
        lblSaldoCaja.setText("$ " + totalIngresos.subtract(egresos));
    }

    @FXML public void registrarIngreso() { mostrarDialogoMovimiento("INGRESO"); }
    @FXML public void registrarGasto() { mostrarDialogoMovimiento("EGRESO"); }
    @FXML public void registrarRetiro() { mostrarDialogoMovimiento("RETIRO"); }

    private void mostrarDialogoMovimiento(String tipo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Movimiento: " + tipo);
        VBox content = new VBox(10);
        TextField txtMonto = new TextField(); txtMonto.setPromptText("Monto");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Descripción");
        ComboBox<String> comboCat = new ComboBox<>();

        if (tipo.equals("INGRESO")) comboCat.getItems().addAll("APERTURA", "CAMBIO", "OTRO");
        else comboCat.getItems().addAll("PROVEEDOR", "RETIRO", "GASTO");

        comboCat.getSelectionModel().selectFirst();

        content.getChildren().addAll(new Label("Categoría"), comboCat, new Label("Monto"), txtMonto, new Label("Descripción"), txtDesc);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    MovimientoCaja mov = new MovimientoCaja(tipo.equals("RETIRO") ? "EGRESO" : tipo,
                            comboCat.getValue(), txtDesc.getText(), new BigDecimal(txtMonto.getText()));
                    cajaRepo.save(mov);
                    actualizarCaja();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
}