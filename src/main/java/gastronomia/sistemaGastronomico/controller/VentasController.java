package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.dao.MovimientoCajaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class VentasController implements Initializable {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private MovimientoCajaRepository movimientoRepository;

    // --- PESTAÑA 1: HISTORIAL ---
    @FXML private DatePicker dpDesde, dpHasta;
    @FXML private Label lblCantOperaciones, lblTotalDia;
    @FXML private TableView<VentaModel> tablaVentas;
    @FXML private TableColumn<VentaModel, Long> colId;
    @FXML private TableColumn<VentaModel, LocalDateTime> colFecha;
    @FXML private TableColumn<VentaModel, String> colMesa, colMozo, colInfo, colMetodo;
    @FXML private TableColumn<VentaModel, BigDecimal> colTotal;
    @FXML private PieChart graficoVentas, graficoMetodosHistorial;

    // --- PESTAÑA 2: CAJA ---
    @FXML private DatePicker dpMovDesde, dpMovHasta;

    // ETIQUETAS DE DINERO
    @FXML private Label lblEstadoCaja;
    @FXML private Label lblSaldoCaja;      // SOLO EFECTIVO
    @FXML private Label lblSaldoBanco;     // NUEVO: SOLO QR/TARJETA
    @FXML private Label lblTotalIngresos;  // Entradas Manuales
    @FXML private Label lblTotalEgresos;   // Salidas Manuales

    @FXML private Button btnAbrirCaja, btnIngreso, btnGasto, btnRetiro, btnCerrarCaja;
    @FXML private TableView<MovimientoModel> tablaMovimientos;
    @FXML private TableColumn<MovimientoModel, LocalDateTime> colMovFecha;
    @FXML private TableColumn<MovimientoModel, String> colMovTipo, colMovCat, colMovDesc;
    @FXML private TableColumn<MovimientoModel, BigDecimal> colMovMonto;
    @FXML private PieChart graficoPagos;

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
        cargarVentasInicial();
        cargarMovimientosInicial();
        actualizarEstadoVisual();
        calcularEstadisticas();
    }

    // [BLOQUE DE CARGA DE DATOS IGUAL QUE ANTES...]
    private void inicializarFechas() {
        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy); dpHasta.setValue(hoy);
        dpMovDesde.setValue(hoy); dpMovHasta.setValue(hoy);
    }
    private void cargarVentasInicial() {
        try {
            List<Pedido> p = pedidoRepository.findByFechaBetweenAndEstadoOrderByIdDesc(LocalDate.now(), LocalDate.now(), "CERRADO");
            if(p.isEmpty()) p = pedidoRepository.findByEstadoOrderByIdDesc("CERRADO");
            llenarTablaVentas(p);
        } catch (Exception e) {}
    }
    private void cargarMovimientosInicial() {
        try {
            LocalDateTime inicio = LocalDate.now().atStartOfDay();
            LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
            List<MovimientoCaja> m = movimientoRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
            if(m.isEmpty()) m = movimientoRepository.findAllByOrderByFechaHoraDesc();
            llenarTablaMovimientos(m);
        } catch (Exception e) {}
    }
    // [FILTROS IGUAL QUE ANTES...]
    @FXML void filtrar(ActionEvent event) {
        if (dpDesde.getValue() != null && dpHasta.getValue() != null) {
            llenarTablaVentas(pedidoRepository.findByFechaBetweenAndEstadoOrderByIdDesc(dpDesde.getValue(), dpHasta.getValue(), "CERRADO"));
            calcularEstadisticas();
        }
    }
    @FXML void filtrarMovimientos(ActionEvent event) {
        if (dpMovDesde.getValue() != null && dpMovHasta.getValue() != null) {
            LocalDateTime inicio = dpMovDesde.getValue().atStartOfDay();
            LocalDateTime fin = dpMovHasta.getValue().atTime(LocalTime.MAX);
            llenarTablaMovimientos(movimientoRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin));
            llenarTablaVentas(pedidoRepository.findByFechaBetweenAndEstadoOrderByIdDesc(dpMovDesde.getValue(), dpMovHasta.getValue(), "CERRADO"));
            calcularEstadisticas();
        }
    }
    @FXML void limpiarFiltros(ActionEvent e) { inicializarFechas(); cargarVentasInicial(); calcularEstadisticas(); }
    @FXML void limpiarFiltrosMov(ActionEvent e) { inicializarFechas(); cargarMovimientosInicial(); calcularEstadisticas(); }


    // ==========================================
    // CÁLCULOS FINANCIEROS (LA CLAVE)
    // ==========================================

    private void calcularEstadisticas() {
        // 1. Total Ventas General (Para mostrar arriba)
        BigDecimal totalVentas = listaVentas.stream().map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalDia.setText(currencyFormat.format(totalVentas));
        lblCantOperaciones.setText(String.valueOf(listaVentas.size()));

        // --- CÁLCULO DE CAJA (Solo Efectivo) ---
        // A. Movimientos Manuales (Ingresos vs Egresos)
        BigDecimal ingresosManuales = listaMovimientos.stream()
                .filter(m -> "INGRESO".equals(m.getTipo()))
                .map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal egresosManuales = listaMovimientos.stream()
                .filter(m -> "EGRESO".equals(m.getTipo()))
                .map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        // B. Ventas Efectivo (Solo lo que entra al cajón)
        BigDecimal ventasEfectivo = listaVentas.stream()
                .filter(v -> normalizarNombre(v.getMetodoPago()).equals("Efectivo"))
                .map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // C. Ventas Digitales (QR, MP, Tarjeta) -> AL BANCO
        BigDecimal ventasBanco = listaVentas.stream()
                .filter(v -> !normalizarNombre(v.getMetodoPago()).equals("Efectivo"))
                .map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // RESULTADOS
        BigDecimal saldoFisicoCaja = ingresosManuales.add(ventasEfectivo).subtract(egresosManuales);

        // Seteo de Labels
        lblSaldoCaja.setText(currencyFormat.format(saldoFisicoCaja)); // Caja física
        lblSaldoBanco.setText(currencyFormat.format(ventasBanco));    // Banco / Virtual
        lblTotalIngresos.setText(currencyFormat.format(ingresosManuales)); // Mov. Manuales
        lblTotalEgresos.setText(currencyFormat.format(egresosManuales));   // Mov. Manuales

        // Colores
        if (saldoFisicoCaja.compareTo(BigDecimal.ZERO) < 0) lblSaldoCaja.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        else lblSaldoCaja.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        // --- GRÁFICOS ---
        actualizarGraficoPie(graficoVentas, listaVentas.stream().collect(Collectors.groupingBy(VentaModel::getMozo, Collectors.summingDouble(v->v.getTotal().doubleValue()))));
        actualizarGraficoPie(graficoMetodosHistorial, listaVentas.stream().collect(Collectors.groupingBy(v->normalizarNombre(v.getMetodoPago()), Collectors.counting())));
        actualizarGraficoPie(graficoPagos, listaMovimientos.stream().collect(Collectors.groupingBy(m->normalizarNombre(m.getCategoria()), Collectors.summingDouble(m->m.getMonto().doubleValue()))));
    }

    // ==========================================
    // OPERACIONES DE CAJA
    // ==========================================

    @FXML
    void accionCerrarCaja(ActionEvent event) {
        // CÁLCULO FINAL PARA EL RESUMEN
        BigDecimal totalVendido = listaVentas.stream().map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoCaja = new BigDecimal(lblSaldoCaja.getText().replace("$", "").replace(".", "").replace(",", ".").trim());

        String mensaje = "Resumen del Día:\n\n" +
                "💵 Total Vendido (Todo): " + currencyFormat.format(totalVendido) + "\n" +
                "--------------------------------\n" +
                "🏦 En Banco (QR/MP): " + lblSaldoBanco.getText() + "\n" +
                "🖐 En Mano (Caja): " + lblSaldoCaja.getText() + "\n\n" +
                "¿Confirmar Cierre Z?";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cierre de Caja");
        alert.setHeaderText("Resumen de Cierre");
        alert.setContentText(mensaje);

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                mostrarDialogoFijo("EGRESO", "Cierre", "Retiro de Caja Final");
                cajaAbierta = false;
                actualizarEstadoVisual();
            }
        });
    }

    @FXML void accionAbrirCaja() {
        mostrarDialogoFijo("INGRESO", "Apertura", "Saldo Inicial");
        cajaAbierta = true; actualizarEstadoVisual();
    }

    // [RESTO DE METODOS IGUAL QUE ANTES: registrarIngreso, mostrarDialogo, utils, etc...]
    @FXML void registrarIngreso() { mostrarDialogo("INGRESO", new String[]{"Ventas Extra", "Ingreso Varios"}); }
    @FXML void registrarGasto() { mostrarDialogo("EGRESO", new String[]{"Proveedores", "Servicios", "Insumos", "Gastos"}); }
    @FXML void registrarRetiro() { mostrarDialogo("EGRESO", new String[]{"Retiro Socio", "Sueldos"}); }

    private void mostrarDialogoFijo(String tipo, String cat, String descDefecto) {
        TextInputDialog d = new TextInputDialog("0"); d.setTitle("Registro " + cat); d.setHeaderText("Monto:");
        d.showAndWait().ifPresent(m -> guardarMovimiento(tipo, cat, descDefecto, new BigDecimal(m.replace(",", "."))));
    }
    private void mostrarDialogo(String tipo, String[] cats) {
        Dialog<Pair<String, String>> dialog = new Dialog<>(); dialog.setTitle(tipo); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<String> cmb = new ComboBox<>(FXCollections.observableArrayList(cats)); cmb.getSelectionModel().selectFirst();
        TextField txtD = new TextField(); TextField txtM = new TextField();
        grid.addRow(0, new Label("Cat:"), cmb); grid.addRow(1, new Label("Det:"), txtD); grid.addRow(2, new Label("$:"), txtM);
        dialog.getDialogPane().setContent(grid); Platform.runLater(txtM::requestFocus);
        dialog.setResultConverter(b -> b==ButtonType.OK ? new Pair<>(cmb.getValue()+"|"+txtD.getText(), txtM.getText()) : null);
        dialog.showAndWait().ifPresent(r -> guardarMovimiento(tipo, r.getKey().split("\\|")[0], r.getKey().split("\\|")[1], new BigDecimal(r.getValue().replace(",", "."))));
    }
    private void guardarMovimiento(String t, String c, String d, BigDecimal m) { movimientoRepository.save(new MovimientoCaja(t, c, d, m)); cargarMovimientosInicial(); calcularEstadisticas(); }

    private String normalizarNombre(String raw) {
        if (raw == null) return "Efectivo"; String s = raw.toLowerCase();
        if (s.contains("tarjeta") || s.contains("débito")) return "Tarjeta";
        if (s.contains("qr") || s.contains("mp")) return "QR / MP";
        if (s.contains("efectivo")) return "Efectivo";
        if (s.contains("apertura")) return "Apertura"; return raw;
    }

    private void actualizarGraficoPie(PieChart chart, Map<String, ? extends Number> d) {
        if(chart==null)return; chart.getData().clear(); chart.setLabelsVisible(false); chart.setLegendSide(Side.RIGHT);
        d.forEach((k,v)->chart.getData().add(new PieChart.Data(k,v.doubleValue())));
        for(PieChart.Data dt:chart.getData()){ Tooltip.install(dt.getNode(),new Tooltip(dt.getName()+": "+dt.getPieValue()));
            dt.getNode().setOnMouseEntered(e->{dt.getNode().setScaleX(1.1);dt.getNode().setScaleY(1.1);});
            dt.getNode().setOnMouseExited(e->{dt.getNode().setScaleX(1.0);dt.getNode().setScaleY(1.0);});}
    }

    // UTILS
    private void actualizarEstadoVisual() { lblEstadoCaja.setText(cajaAbierta?"🟢 ABIERTA":"🔴 CERRADA"); lblEstadoCaja.setStyle(cajaAbierta?"-fx-text-fill:#2ecc71;":"-fx-text-fill:#e74c3c;"); btnAbrirCaja.setVisible(!cajaAbierta); btnIngreso.setDisable(!cajaAbierta); btnGasto.setDisable(!cajaAbierta); btnRetiro.setDisable(!cajaAbierta); btnCerrarCaja.setDisable(!cajaAbierta); }
    private void llenarTablaVentas(List<Pedido> p) { listaVentas.clear(); for(Pedido x:p) listaVentas.add(new VentaModel(x.getId(),(x.getFecha()!=null?LocalDateTime.of(x.getFecha(),x.getHora()):LocalDateTime.now()), (x.getMesa()!=null?"Mesa "+x.getMesa().getNumero():"Barra"), (x.getMozo()!=null?x.getMozo().getNombre():"-"), "Pedido #"+x.getId(), x.getMetodoPago(), x.getTotal())); tablaVentas.refresh(); }
    private void llenarTablaMovimientos(List<MovimientoCaja> m) { listaMovimientos.clear(); for(MovimientoCaja x:m) listaMovimientos.add(new MovimientoModel(x.getFechaHora(), x.getTipo(), x.getCategoria(), x.getDescripcion(), x.getMonto())); tablaMovimientos.refresh(); }
    @FXML void exportarExcel(ActionEvent e) {} @FXML void exportarMovimientosCSV(ActionEvent e) {} @FXML void abrirReporteMozo() {}

    // Config Tablas (Igual que antes)
    private void configurarTablas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id")); colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora")); colFecha.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(LocalDateTime i,boolean e){super.updateItem(i,e);setText(e||i==null?null:i.format(timeFormatter));}}); colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa")); colMozo.setCellValueFactory(new PropertyValueFactory<>("mozo")); colInfo.setCellValueFactory(new PropertyValueFactory<>("detalle")); colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago")); colTotal.setCellValueFactory(new PropertyValueFactory<>("total")); colTotal.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(BigDecimal i,boolean e){super.updateItem(i,e);setText(e||i==null?null:currencyFormat.format(i));setStyle("-fx-alignment:CENTER-RIGHT;-fx-font-weight:bold;");}}); tablaVentas.setItems(listaVentas);
        colMovFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora")); colMovFecha.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(LocalDateTime i,boolean e){super.updateItem(i,e);setText(e||i==null?null:i.format(timeFormatter));}}); colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo")); colMovCat.setCellValueFactory(new PropertyValueFactory<>("categoria")); colMovDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion")); colMovMonto.setCellValueFactory(new PropertyValueFactory<>("monto")); colMovMonto.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(BigDecimal i,boolean e){super.updateItem(i,e);if(!e&&i!=null){setText(currencyFormat.format(i));MovimientoModel r=getTableRow().getItem();if(r!=null)setStyle("INGRESO".equals(r.getTipo())?"-fx-text-fill:#27ae60;-fx-alignment:CENTER-RIGHT;":"-fx-text-fill:#c0392b;-fx-alignment:CENTER-RIGHT;");}else{setText(null);}}}); tablaMovimientos.setItems(listaMovimientos);
    }

    // Inner Classes
    public static class VentaModel { private Long id; private LocalDateTime fechaHora; private String mesa, mozo, detalle, metodoPago; private BigDecimal total; public VentaModel(Long id, LocalDateTime fh, String me, String mo, String de, String mp, BigDecimal t) { this.id=id; this.fechaHora=fh; this.mesa=me; this.mozo=mo; this.detalle=de; this.metodoPago=mp; this.total=t; } public Long getId() {return id;} public LocalDateTime getFechaHora() {return fechaHora;} public String getMesa() {return mesa;} public String getMozo() {return mozo;} public String getDetalle() {return detalle;} public String getMetodoPago() {return metodoPago;} public BigDecimal getTotal() {return total;} }
    public static class MovimientoModel { private LocalDateTime fechaHora; private String tipo, categoria, descripcion; private BigDecimal monto; public MovimientoModel(LocalDateTime fh, String t, String c, String d, BigDecimal m) { this.fechaHora=fh; this.tipo=t; this.categoria=c; this.descripcion=d; this.monto=m; } public LocalDateTime getFechaHora() {return fechaHora;} public String getTipo() {return tipo;} public String getCategoria() {return categoria;} public String getDescripcion() {return descripcion;} public BigDecimal getMonto() {return monto;} }
}