package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MovimientoCajaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

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
    private List<Pedido> listaMaestraVentas;

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

    // --- PESTAÑA 2: CAJA (NUEVO) ---
    @FXML private Label lblSaldoCaja;
    @FXML private Label lblTotalIngresos;
    @FXML private Label lblTotalEgresos;
    @FXML private TableView<MovimientoCaja> tablaMovimientos;
    @FXML private TableColumn<MovimientoCaja, String> colMovFecha;
    @FXML private TableColumn<MovimientoCaja, String> colMovTipo;
    @FXML private TableColumn<MovimientoCaja, String> colMovCat;
    @FXML private TableColumn<MovimientoCaja, String> colMovDesc;
    @FXML private TableColumn<MovimientoCaja, BigDecimal> colMovMonto;

    public VentasController(PedidoRepository pedidoRepo, MovimientoCajaRepository cajaRepo) {
        this.pedidoRepo = pedidoRepo;
        this.cajaRepo = cajaRepo;
    }

    @FXML
    public void initialize() {
        configurarTablaVentas();
        configurarTablaMovimientos();

        // Cargar datos iniciales
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());
        cargarDatosBase(); // Ventas
        actualizarCaja();  // Caja
    }

    // ==========================================
    // LÓGICA DE VENTAS (Ya la tenías)
    // ==========================================
    private void configurarTablaVentas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getFecha() + " " + cell.getValue().getHora().toString().substring(0, 5)));
        colMesa.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getMesa().getNumero())));
        colMozo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMozo().getNombre()));
    }

    private void cargarDatosBase() {
        listaMaestraVentas = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO");
        filtrar();
    }

    @FXML
    public void filtrar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        List<Pedido> filtrados = listaMaestraVentas.stream()
                .filter(p -> (p.getFecha().isEqual(desde) || p.getFecha().isAfter(desde)) &&
                        (p.getFecha().isEqual(hasta) || p.getFecha().isBefore(hasta)))
                .collect(Collectors.toList());

        tablaVentas.getItems().setAll(filtrados);
        BigDecimal suma = filtrados.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalDia.setText("$ " + suma);
    }

    @FXML public void limpiarFiltros() { dpDesde.setValue(null); dpHasta.setValue(null); filtrar(); }

    // ==========================================
    // LÓGICA DE CAJA (NUEVO)
    // ==========================================
    private void configurarTablaMovimientos() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        colMovFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaHora().format(fmt)));
        colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMovCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMovDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMovMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        // Colorear monto (Verde ingreso, Rojo egreso)
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
        // 1. Cargar lista de movimientos manuales
        List<MovimientoCaja> movimientos = cajaRepo.findAllByOrderByFechaHoraDesc();
        tablaMovimientos.getItems().setAll(movimientos);

        // 2. Calcular Totales Manuales
        BigDecimal totalIngresosManuales = movimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).map(MovimientoCaja::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEgresosManuales = movimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).map(MovimientoCaja::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Sumar también las VENTAS del día (Automático)
        // Opcional: Si quieres que el saldo de caja incluya las ventas de hoy automáticamente
        BigDecimal totalVentasHoy = pedidoRepo.findByEstadoOrderByIdDesc("CERRADO").stream()
                .filter(p -> p.getFecha().isEqual(LocalDate.now()))
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIngresos = totalIngresosManuales.add(totalVentasHoy);

        // 4. Saldo Final
        BigDecimal saldo = totalIngresos.subtract(totalEgresosManuales);

        // 5. Actualizar Pantalla
        lblTotalIngresos.setText("$ " + totalIngresos);
        lblTotalEgresos.setText("$ " + totalEgresosManuales);
        lblSaldoCaja.setText("$ " + saldo);
    }

    // --- ACCIONES DE BOTONES ---
    @FXML public void registrarIngreso() { mostrarDialogoMovimiento("INGRESO"); }
    @FXML public void registrarGasto() { mostrarDialogoMovimiento("EGRESO"); }
    @FXML public void registrarRetiro() { mostrarDialogoMovimiento("RETIRO"); }

    private void mostrarDialogoMovimiento(String tipo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Movimiento: " + tipo);
        dialog.setHeaderText("Complete los datos");

        // Formulario
        VBox content = new VBox(10);
        TextField txtMonto = new TextField(); txtMonto.setPromptText("Monto ($)");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Descripción (ej: Pago Proveedor)");
        ComboBox<String> comboCat = new ComboBox<>();

        if (tipo.equals("INGRESO")) comboCat.getItems().addAll("APERTURA CAJA", "CAMBIO", "OTRO");
        else if (tipo.equals("RETIRO")) { comboCat.getItems().add("RETIRO / SANGRÍA"); comboCat.getSelectionModel().selectFirst(); }
        else comboCat.getItems().addAll("PROVEEDOR", "INSUMO", "GASTO VARIO", "SUELDO");

        content.getChildren().addAll(new Label("Categoría:"), comboCat, new Label("Monto:"), txtMonto, new Label("Descripción:"), txtDesc);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                BigDecimal monto = new BigDecimal(txtMonto.getText());
                String cat = comboCat.getValue() != null ? comboCat.getValue() : "VARIOS";
                String desc = txtDesc.getText();

                // Si es retiro, guardamos como EGRESO en la BD pero categoría RETIRO
                String tipoReal = tipo.equals("RETIRO") ? "EGRESO" : tipo;

                MovimientoCaja mov = new MovimientoCaja(tipoReal, cat, desc, monto);
                cajaRepo.save(mov);
                actualizarCaja(); // Refrescar tabla y saldos

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Monto inválido o error al guardar.");
                alert.show();
            }
        }
    }
}