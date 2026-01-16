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
import javafx.scene.input.MouseEvent;
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
    @FXML private Label lblEstadoCaja, lblSaldoCaja, lblSaldoBanco, lblTotalIngresos, lblTotalEgresos;
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

    // ==========================================
    // CARGA Y FILTROS
    // ==========================================
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
            // Sincronizar ventas con las fechas de caja para que el saldo tenga sentido
            llenarTablaVentas(pedidoRepository.findByFechaBetweenAndEstadoOrderByIdDesc(dpMovDesde.getValue(), dpMovHasta.getValue(), "CERRADO"));
            calcularEstadisticas();
        }
    }

    @FXML void limpiarFiltros(ActionEvent e) { inicializarFechas(); cargarVentasInicial(); calcularEstadisticas(); }
    @FXML void limpiarFiltrosMov(ActionEvent e) { inicializarFechas(); cargarMovimientosInicial(); calcularEstadisticas(); }

    // ==========================================
    // ESTADÍSTICAS Y SALDOS
    // ==========================================
    private void calcularEstadisticas() {
        // 1. Total Ventas (Visual)
        BigDecimal totalVentas = listaVentas.stream().map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalDia.setText(currencyFormat.format(totalVentas));
        lblCantOperaciones.setText(String.valueOf(listaVentas.size()));

        // 2. Saldos Financieros
        BigDecimal ingresosManuales = listaMovimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal egresosManuales = listaMovimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).map(MovimientoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ventas Efectivo (Van a la Caja)
        BigDecimal ventasEfectivo = listaVentas.stream().filter(v -> normalizarNombre(v.getMetodoPago()).equals("Efectivo")).map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ventas Digitales (Van al Banco)
        BigDecimal ventasBanco = listaVentas.stream().filter(v -> !normalizarNombre(v.getMetodoPago()).equals("Efectivo")).map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Totales Finales
        BigDecimal saldoFisico = ingresosManuales.add(ventasEfectivo).subtract(egresosManuales);

        lblSaldoCaja.setText(currencyFormat.format(saldoFisico));
        lblSaldoBanco.setText(currencyFormat.format(ventasBanco));
        lblTotalIngresos.setText(currencyFormat.format(ingresosManuales));
        lblTotalEgresos.setText(currencyFormat.format(egresosManuales));

        // Estilos
        lblSaldoCaja.setStyle(saldoFisico.compareTo(BigDecimal.ZERO) < 0 ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");

        // 3. Gráficos
        actualizarGraficoPie(graficoVentas, listaVentas.stream().collect(Collectors.groupingBy(VentaModel::getMozo, Collectors.summingDouble(v->v.getTotal().doubleValue()))));
        actualizarGraficoPie(graficoMetodosHistorial, listaVentas.stream().collect(Collectors.groupingBy(v->normalizarNombre(v.getMetodoPago()), Collectors.counting())));
        actualizarGraficoPie(graficoPagos, listaMovimientos.stream().collect(Collectors.groupingBy(m->normalizarNombre(m.getCategoria()), Collectors.summingDouble(m->m.getMonto().doubleValue()))));
    }

    // ==========================================
    // ARQUEO DE MOZO (NUEVO)
    // ==========================================
    @FXML
    void abrirReporteMozo() {
        // 1. Obtener lista de mozos
        List<String> mozos = tablaVentas.getItems().stream()
                .map(VentaModel::getMozo)
                .distinct().filter(m -> !"-".equals(m) && !"Sin Mozo".equals(m))
                .collect(Collectors.toList());

        if (mozos.isEmpty()) { mostrarAlerta("Info", "No hay mozos con ventas en este período."); return; }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(mozos.get(0), mozos);
        dialog.setTitle("Arqueo Individual");
        dialog.setHeaderText("Seleccione Mozo:");
        dialog.setContentText("Mozo:");

        dialog.showAndWait().ifPresent(this::calcularRendicionMozo);
    }

    private void calcularRendicionMozo(String mozo) {
        List<VentaModel> ventas = tablaVentas.getItems().stream().filter(v -> v.getMozo().equals(mozo)).collect(Collectors.toList());

        BigDecimal total = ventas.stream().map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal digital = ventas.stream().filter(v -> !normalizarNombre(v.getMetodoPago()).equals("Efectivo")).map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo = total.subtract(digital);

        String msg = "MOZO: " + mozo + "\n\n" +
                "💰 TOTAL VENDIDO: " + currencyFormat.format(total) + "\n" +
                "💳 MENOS DIGITAL: " + currencyFormat.format(digital) + "\n" +
                "----------------------------\n" +
                "💵 A ENTREGAR (EFE): " + currencyFormat.format(efectivo);

        mostrarAlerta("Rendición de Caja", msg);
    }

    // ==========================================
    // OPERACIONES CAJA
    // ==========================================
    @FXML
    void accionCerrarCaja(ActionEvent event) {
        BigDecimal totalVendido = listaVentas.stream().map(VentaModel::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String msg = "Resumen del Día:\n\n" +
                "💵 Total Vendido (Todo): " + currencyFormat.format(totalVendido) + "\n" +
                "--------------------------------\n" +
                "🏦 En Banco (QR/MP): " + lblSaldoBanco.getText() + "\n" +
                "🖐 En Mano (Caja): " + lblSaldoCaja.getText() + "\n\n" +
                "¿Confirmar Cierre Z?";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Cierre Z");
        alert.setHeaderText("Confirmar Cierre");
        alert.showAndWait().ifPresent(r -> {
            if(r == ButtonType.OK) {
                mostrarDialogoFijo("EGRESO", "Cierre", "Retiro Final");
                cajaAbierta = false; actualizarEstadoVisual();
            }
        });
    }

    @FXML void accionAbrirCaja() { mostrarDialogoFijo("INGRESO", "Apertura", "Saldo Inicial"); cajaAbierta=true; actualizarEstadoVisual(); }
    @FXML void registrarIngreso() { mostrarDialogo("INGRESO", new String[]{"Ventas Extra", "Ingreso Varios"}); }
    @FXML void registrarGasto() { mostrarDialogo("EGRESO", new String[]{"Proveedores", "Servicios", "Insumos", "Gastos"}); }
    @FXML void registrarRetiro() { mostrarDialogo("EGRESO", new String[]{"Retiro Socio", "Sueldos"}); }

    // ==========================================
    // UTILS Y AUXILIARES
    // ==========================================
    private void mostrarDialogoFijo(String t, String c, String d) {
        TextInputDialog td = new TextInputDialog("0"); td.setTitle(t); td.setHeaderText(c);
        td.showAndWait().ifPresent(m -> guardarMovimiento(t, c, d, new BigDecimal(m.replace(",", "."))));
    }

    private void mostrarDialogo(String tipo, String[] cats) {
        Dialog<Pair<String,String>> d = new Dialog<>(); d.setTitle(tipo); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(cats)); cb.getSelectionModel().selectFirst();
        TextField tD = new TextField(); TextField tM = new TextField();
        g.addRow(0, new Label("Cat:"), cb); g.addRow(1, new Label("Det:"), tD); g.addRow(2, new Label("$:"), tM);
        d.getDialogPane().setContent(g); Platform.runLater(tM::requestFocus);
        d.setResultConverter(b -> b==ButtonType.OK ? new Pair<>(cb.getValue()+"|"+tD.getText(), tM.getText()) : null);
        d.showAndWait().ifPresent(r -> {
            try { guardarMovimiento(tipo, r.getKey().split("\\|")[0], r.getKey().split("\\|")[1], new BigDecimal(r.getValue().replace(",", "."))); }
            catch(Exception e) { mostrarAlerta("Error", "Monto inválido"); }
        });
    }

    private void guardarMovimiento(String t, String c, String d, BigDecimal m) {
        movimientoRepository.save(new MovimientoCaja(t, c, d, m));
        cargarMovimientosInicial(); calcularEstadisticas();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(contenido);
        a.show(); // Usamos show() en lugar de showAndWait() para mensajes simples
    }

    private String normalizarNombre(String raw) {
        if (raw == null) return "Efectivo"; String s = raw.toLowerCase();
        if (s.contains("tarjeta") || s.contains("débito") || s.contains("crédito")) return "Tarjeta";
        if (s.contains("qr") || s.contains("mp") || s.contains("mercado")) return "QR / MP";
        if (s.contains("efectivo")) return "Efectivo";
        if (s.contains("apertura")) return "Apertura";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    private void actualizarGraficoPie(PieChart c, Map<String, ? extends Number> d) {
        if(c==null)return; c.getData().clear(); c.setLabelsVisible(false); c.setLegendSide(Side.RIGHT);
        d.forEach((k,v)->c.getData().add(new PieChart.Data(k,v.doubleValue())));
        for(PieChart.Data dt:c.getData()){ Tooltip.install(dt.getNode(),new Tooltip(dt.getName()+": "+dt.getPieValue()));
            dt.getNode().setOnMouseEntered(e->{dt.getNode().setScaleX(1.1);dt.getNode().setScaleY(1.1);});
            dt.getNode().setOnMouseExited(e->{dt.getNode().setScaleX(1.0);dt.getNode().setScaleY(1.0);});}
    }

    private void actualizarEstadoVisual() {
        lblEstadoCaja.setText(cajaAbierta?"🟢 ABIERTA":"🔴 CERRADA");
        lblEstadoCaja.setStyle(cajaAbierta?"-fx-text-fill:#2ecc71;":"-fx-text-fill:#e74c3c;");
        btnAbrirCaja.setVisible(!cajaAbierta);
        btnIngreso.setDisable(!cajaAbierta); btnGasto.setDisable(!cajaAbierta); btnRetiro.setDisable(!cajaAbierta); btnCerrarCaja.setDisable(!cajaAbierta);
    }

    private void llenarTablaVentas(List<Pedido> p) { listaVentas.clear(); for(Pedido x:p) listaVentas.add(new VentaModel(x.getId(),(x.getFecha()!=null?LocalDateTime.of(x.getFecha(),x.getHora()):LocalDateTime.now()), (x.getMesa()!=null?"Mesa "+x.getMesa().getNumero():"Barra"), (x.getMozo()!=null?x.getMozo().getNombre():"-"), "Pedido #"+x.getId(), x.getMetodoPago(), x.getTotal())); tablaVentas.refresh(); }
    private void llenarTablaMovimientos(List<MovimientoCaja> m) { listaMovimientos.clear(); for(MovimientoCaja x:m) listaMovimientos.add(new MovimientoModel(x.getFechaHora(), x.getTipo(), x.getCategoria(), x.getDescripcion(), x.getMonto())); tablaMovimientos.refresh(); }
    @FXML void exportarExcel(ActionEvent e) {} @FXML void exportarMovimientosCSV(ActionEvent e) {}

    // Config Tablas
    private void configurarTablas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id")); colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora")); colFecha.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(LocalDateTime i,boolean e){super.updateItem(i,e);setText(e||i==null?null:i.format(timeFormatter));}}); colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa")); colMozo.setCellValueFactory(new PropertyValueFactory<>("mozo")); colInfo.setCellValueFactory(new PropertyValueFactory<>("detalle")); colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago")); colTotal.setCellValueFactory(new PropertyValueFactory<>("total")); colTotal.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(BigDecimal i,boolean e){super.updateItem(i,e);setText(e||i==null?null:currencyFormat.format(i));setStyle("-fx-alignment:CENTER-RIGHT;-fx-font-weight:bold;");}}); tablaVentas.setItems(listaVentas);
        colMovFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora")); colMovFecha.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(LocalDateTime i,boolean e){super.updateItem(i,e);setText(e||i==null?null:i.format(timeFormatter));}}); colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo")); colMovCat.setCellValueFactory(new PropertyValueFactory<>("categoria")); colMovDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion")); colMovMonto.setCellValueFactory(new PropertyValueFactory<>("monto")); colMovMonto.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(BigDecimal i,boolean e){super.updateItem(i,e);if(!e&&i!=null){setText(currencyFormat.format(i));MovimientoModel r=getTableRow().getItem();if(r!=null)setStyle("INGRESO".equals(r.getTipo())?"-fx-text-fill:#27ae60;-fx-alignment:CENTER-RIGHT;":"-fx-text-fill:#c0392b;-fx-alignment:CENTER-RIGHT;");}else{setText(null);}}}); tablaMovimientos.setItems(listaMovimientos);
    }

    // Modelos Internos
    public static class VentaModel { private Long id; private LocalDateTime fechaHora; private String mesa, mozo, detalle, metodoPago; private BigDecimal total; public VentaModel(Long id, LocalDateTime fh, String me, String mo, String de, String mp, BigDecimal t) { this.id=id; this.fechaHora=fh; this.mesa=me; this.mozo=mo; this.detalle=de; this.metodoPago=mp; this.total=t; } public Long getId() {return id;} public LocalDateTime getFechaHora() {return fechaHora;} public String getMesa() {return mesa;} public String getMozo() {return mozo;} public String getDetalle() {return detalle;} public String getMetodoPago() {return metodoPago;} public BigDecimal getTotal() {return total;} }
    public static class MovimientoModel { private LocalDateTime fechaHora; private String tipo, categoria, descripcion; private BigDecimal monto; public MovimientoModel(LocalDateTime fh, String t, String c, String d, BigDecimal m) { this.fechaHora=fh; this.tipo=t; this.categoria=c; this.descripcion=d; this.monto=m; } public LocalDateTime getFechaHora() {return fechaHora;} public String getTipo() {return tipo;} public String getCategoria() {return categoria;} public String getDescripcion() {return descripcion;} public BigDecimal getMonto() {return monto;} }
}