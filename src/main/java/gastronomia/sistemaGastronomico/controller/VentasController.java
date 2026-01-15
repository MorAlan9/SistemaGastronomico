package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.dao.MovimientoCajaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class VentasController implements Initializable {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private MovimientoCajaRepository movimientoRepository;

    // --- PESTAÑA 1: HISTORIAL ---
    @FXML private DatePicker dpDesde, dpHasta;
    @FXML private Label lblCantOperaciones, lblTotalDia;
    @FXML private TableView<VentaModel> tablaVentas;
    @FXML private TableColumn<VentaModel, Long> colId;
    @FXML private TableColumn<VentaModel, LocalDateTime> colFecha;
    @FXML private TableColumn<VentaModel, String> colMesa, colMozo, colInfo, colMetodo;
    @FXML private TableColumn<VentaModel, BigDecimal> colTotal;

    // Gráficos Historial
    @FXML private PieChart graficoVentas; // Mozos
    @FXML private PieChart graficoMetodosHistorial; // Métodos de pago (Historial)

    // --- PESTAÑA 2: CAJA ---
    @FXML private Label lblEstadoCaja, lblSaldoCaja, lblTotalIngresos, lblTotalEgresos;
    @FXML private Button btnAbrirCaja, btnIngreso, btnGasto, btnRetiro, btnCerrarCaja;
    @FXML private TableView<MovimientoModel> tablaMovimientos;
    @FXML private TableColumn<MovimientoModel, LocalDateTime> colMovFecha;
    @FXML private TableColumn<MovimientoModel, String> colMovTipo, colMovCat, colMovDesc;
    @FXML private TableColumn<MovimientoModel, BigDecimal> colMovMonto;
    @FXML private PieChart graficoPagos; // Métodos de pago (Caja del día)

    // --- Variables ---
    private boolean cajaAbierta = false;
    private ObservableList<VentaModel> listaVentas = FXCollections.observableArrayList();
    private ObservableList<MovimientoModel> listaMovimientos = FXCollections.observableArrayList();

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTablas();
        inicializarFechas();

        cargarVentasInicial();  // Carga inicial (todo)
        cargarMovimientos();

        actualizarEstadoVisual();
        calcularEstadisticas();
    }

    // ==========================================
    // CARGA DE DATOS
    // ==========================================

    private void cargarVentasInicial() {
        // Por defecto: Carga todo lo cerrado, ORDENADO DESCENDENTE
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByIdDesc("CERRADO");
            llenarTablaVentas(pedidos);
        } catch (Exception e) {
            System.err.println("Error cargando ventas iniciales: " + e.getMessage());
        }
    }

    @FXML
    void filtrar(ActionEvent event) {
        LocalDate inicio = dpDesde.getValue();
        LocalDate fin = dpHasta.getValue();

        if (inicio != null && fin != null) {
            try {
                // Filtro por fechas y orden descendente
                List<Pedido> pedidosFiltrados = pedidoRepository.findByFechaBetweenAndEstadoOrderByIdDesc(inicio, fin, "CERRADO");
                llenarTablaVentas(pedidosFiltrados);
                calcularEstadisticas();
            } catch (Exception e) {
                mostrarAlerta("Error Filtro", "Verifica que el método findByFechaBetween... exista en tu PedidoRepository.");
            }
        } else {
            cargarVentasInicial();
        }
    }

    @FXML
    void limpiarFiltros(ActionEvent event) {
        inicializarFechas();
        cargarVentasInicial();
        calcularEstadisticas();
    }

    private void llenarTablaVentas(List<Pedido> pedidos) {
        listaVentas.clear();
        for (Pedido p : pedidos) {
            LocalDateTime fechaHora = (p.getFecha() != null && p.getHora() != null)
                    ? LocalDateTime.of(p.getFecha(), p.getHora())
                    : LocalDateTime.now();

            String mesaStr = (p.getMesa() != null) ? "Mesa " + p.getMesa().getNumero() : "Barra";
            String mozoStr = (p.getMozo() != null) ? p.getMozo().getNombre() : "Sin Mozo";
            String metodoStr = (p.getMetodoPago() != null) ? p.getMetodoPago() : "Sin Definir";

            listaVentas.add(new VentaModel(
                    p.getId(), fechaHora, mesaStr, mozoStr, "Pedido #" + p.getId(), metodoStr, p.getTotal()
            ));
        }
        tablaVentas.refresh();
    }

    private void cargarMovimientos() {
        listaMovimientos.clear();
        try {
            // Usamos findAllByOrderByFechaHoraDesc si lo agregaste al repo, sino findAll()
            List<MovimientoCaja> movs = movimientoRepository.findAllByOrderByFechaHoraDesc();

            for (MovimientoCaja m : movs) {
                listaMovimientos.add(new MovimientoModel(
                        m.getFechaHora(), m.getTipo(), m.getCategoria(), m.getDescripcion(), m.getMonto()
                ));
            }
        } catch (Exception e) {
            // Fallback si no existe el método ordenado
            List<MovimientoCaja> movs = movimientoRepository.findAll();
            for (MovimientoCaja m : movs) {
                listaMovimientos.add(new MovimientoModel(
                        m.getFechaHora(), m.getTipo(), m.getCategoria(), m.getDescripcion(), m.getMonto()
                ));
            }
        }
        tablaMovimientos.refresh();
    }

    // ==========================================
    // ESTADÍSTICAS Y GRÁFICOS (CON EFECTO HOVER)
    // ==========================================

    private void calcularEstadisticas() {
        // 1. Totales Numéricos Historial
        BigDecimal totalVentas = listaVentas.stream()
                .map(VentaModel::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalDia.setText(currencyFormat.format(totalVentas));
        lblCantOperaciones.setText(String.valueOf(listaVentas.size()));

        // 2. Gráfico 1: Ventas por Mozo
        actualizarGraficoPie(graficoVentas, listaVentas.stream().collect(Collectors.groupingBy(
                VentaModel::getMozo, Collectors.summingDouble(v -> v.getTotal().doubleValue())
        )));

        // 3. Gráfico 2: Métodos de Pago (EN HISTORIAL)
        actualizarGraficoPie(graficoMetodosHistorial, listaVentas.stream().collect(Collectors.groupingBy(
                VentaModel::getMetodoPago,
                Collectors.counting()
        )));

        // 4. Totales Caja
        BigDecimal ingresos = listaMovimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal egresos = listaMovimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalIngresos.setText(currencyFormat.format(ingresos));
        lblTotalEgresos.setText(currencyFormat.format(egresos));
        lblSaldoCaja.setText(currencyFormat.format(ingresos.subtract(egresos)));

        // 5. Gráfico Caja (Opcional, copia del de historial o solo lo de hoy)
        // Por ahora le pasamos lo mismo para que no quede vacío
        actualizarGraficoPie(graficoPagos, listaVentas.stream().collect(Collectors.groupingBy(
                VentaModel::getMetodoPago, Collectors.counting()
        )));
    }

    // Método genérico para llenar y animar tortas
    private void actualizarGraficoPie(PieChart chart, Map<String, ? extends Number> datos) {
        if (chart == null) return; // Protección contra nulos si el FXML no cargó bien

        chart.getData().clear();
        datos.forEach((key, value) -> {
            PieChart.Data slice = new PieChart.Data(key, value.doubleValue());
            chart.getData().add(slice);
        });
        aplicarEfectoHover(chart);
    }

    private void aplicarEfectoHover(PieChart chart) {
        for (PieChart.Data data : chart.getData()) {
            data.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
                data.getNode().setScaleX(1.15); // Agrandar 15%
                data.getNode().setScaleY(1.15);
                chart.setCursor(javafx.scene.Cursor.HAND);
            });
            data.getNode().addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
                data.getNode().setScaleX(1.0); // Volver normal
                data.getNode().setScaleY(1.0);
                chart.setCursor(javafx.scene.Cursor.DEFAULT);
            });

            // Tooltip básico
            Tooltip tooltip = new Tooltip(data.getName() + ": " + data.getPieValue());
            Tooltip.install(data.getNode(), tooltip);
        }
    }

    // ==========================================
    // CAJA Y OPERACIONES
    // ==========================================

    private void actualizarEstadoVisual() {
        if (cajaAbierta) {
            lblEstadoCaja.setText("🟢 CAJA ABIERTA");
            lblEstadoCaja.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            if(btnAbrirCaja != null) { btnAbrirCaja.setVisible(false); btnAbrirCaja.setDisable(true); }
            setBotonesDisable(false);
        } else {
            lblEstadoCaja.setText("🔴 CAJA CERRADA");
            lblEstadoCaja.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            if(btnAbrirCaja != null) { btnAbrirCaja.setVisible(true); btnAbrirCaja.setDisable(false); }
            setBotonesDisable(true);
        }
    }

    private void setBotonesDisable(boolean disable) {
        if (btnIngreso != null) btnIngreso.setDisable(disable);
        if (btnGasto != null) btnGasto.setDisable(disable);
        if (btnRetiro != null) btnRetiro.setDisable(disable);
        if (btnCerrarCaja != null) btnCerrarCaja.setDisable(disable);
    }

    @FXML
    void accionAbrirCaja(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Apertura de Caja");
        dialog.setHeaderText("Iniciando Turno");
        dialog.setContentText("Monto inicial (Cambio):");
        dialog.showAndWait().ifPresent(montoStr -> {
            try {
                BigDecimal monto = new BigDecimal(montoStr);
                guardarMovimiento("INGRESO", "Caja", "Apertura", monto);
                cajaAbierta = true;
                actualizarEstadoVisual();
            } catch (Exception e) { mostrarAlerta("Error", "Monto inválido"); }
        });
    }

    @FXML
    void accionCerrarCaja(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cierre de Caja");
        alert.setHeaderText("¿Confirmar Cierre Z?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                cajaAbierta = false;
                actualizarEstadoVisual();
                mostrarAlerta("Cierre", "Caja cerrada correctamente.");
            }
        });
    }

    @FXML void registrarIngreso() { guardarMovimiento("INGRESO", "Varios", "Ingreso Manual", new BigDecimal("1000")); }
    @FXML void registrarGasto() { guardarMovimiento("EGRESO", "Proveedores", "Gasto Manual", new BigDecimal("500")); }
    @FXML void registrarRetiro() { guardarMovimiento("EGRESO", "Retiro", "Sangría", new BigDecimal("2000")); }

    private void guardarMovimiento(String tipo, String cat, String desc, BigDecimal monto) {
        MovimientoCaja m = new MovimientoCaja(tipo, cat, desc, monto);
        movimientoRepository.save(m);
        cargarMovimientos();
        calcularEstadisticas();
    }

    @FXML
    void exportarExcel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Fecha,Mesa,Mozo,Metodo Pago,Total");
                for (VentaModel v : listaVentas) {
                    writer.println(v.getId() + "," + v.getFechaHora() + "," + v.getMesa() + "," + v.getMozo() + "," + v.getMetodoPago() + "," + v.getTotal());
                }
                mostrarAlerta("Éxito", "Exportado correctamente.");
            } catch (Exception e) { mostrarAlerta("Error", e.getMessage()); }
        }
    }

    @FXML void abrirReporteMozo() { mostrarAlerta("Info", "Próximamente"); }

    private void mostrarAlerta(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.show();
    }

    private void configurarTablas() {
        // Ventas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa"));
        colMozo.setCellValueFactory(new PropertyValueFactory<>("mozo"));
        colInfo.setCellValueFactory(new PropertyValueFactory<>("detalle"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(timeFormatter));
            }
        });
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : currencyFormat.format(item));
                setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
            }
        });
        tablaVentas.setItems(listaVentas);

        // Movimientos
        colMovFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMovCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMovDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMovMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        colMovFecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(timeFormatter));
            }
        });
        colMovMonto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    setText(currencyFormat.format(item));
                    MovimientoModel row = getTableRow().getItem();
                    if (row != null) setStyle("INGRESO".equals(row.getTipo()) ? "-fx-text-fill: green; -fx-alignment: CENTER-RIGHT;" : "-fx-text-fill: red; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
        tablaMovimientos.setItems(listaMovimientos);
    }

    private void inicializarFechas() { dpDesde.setValue(LocalDate.now()); dpHasta.setValue(LocalDate.now()); }

    // Models
    public static class VentaModel {
        private Long id; private LocalDateTime fechaHora; private String mesa, mozo, detalle, metodoPago; private BigDecimal total;
        public VentaModel(Long id, LocalDateTime fh, String me, String mo, String de, String mp, BigDecimal t) {
            this.id = id; this.fechaHora = fh; this.mesa = me; this.mozo = mo; this.detalle = de; this.metodoPago = mp; this.total = t;
        }
        public Long getId() { return id; } public LocalDateTime getFechaHora() { return fechaHora; }
        public String getMesa() { return mesa; } public String getMozo() { return mozo; }
        public String getDetalle() { return detalle; } public String getMetodoPago() { return metodoPago; }
        public BigDecimal getTotal() { return total; }
    }
    public static class MovimientoModel {
        private LocalDateTime fechaHora; private String tipo, categoria, descripcion; private BigDecimal monto;
        public MovimientoModel(LocalDateTime fh, String t, String c, String d, BigDecimal m) {
            this.fechaHora = fh; this.tipo = t; this.categoria = c; this.descripcion = d; this.monto = m;
        }
        public LocalDateTime getFechaHora() { return fechaHora; } public String getTipo() { return tipo; }
        public String getCategoria() { return categoria; } public String getDescripcion() { return descripcion; }
        public BigDecimal getMonto() { return monto; }
    }
}