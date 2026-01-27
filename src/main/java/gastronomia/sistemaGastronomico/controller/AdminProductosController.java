package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminProductosController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    @Autowired private ApplicationContext context; // Necesario para navegar

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre, colCategoria, colTipo;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;

    @FXML private TextField txtNombre, txtPrecio, txtStock;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkEsCocina;
    @FXML private Button btnGuardar;

    private Producto productoSeleccionado;

    public AdminProductosController(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarCategorias();
        cargarProductos();

        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaProductos.getSelectionModel().getSelectedItem() != null) {
                editarProducto(tablaProductos.getSelectionModel().getSelectedItem());
            }
        });

        // --- AQUÍ ACTIVAMOS EL F9 ---
        Platform.runLater(() -> {
            if (tablaProductos.getScene() != null) {
                // F9 -> Volver
                tablaProductos.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F9),
                        this::volverAlMenu
                );
                // F3 -> Buscar
                tablaProductos.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F3),
                        this::abrirBusquedaRapida
                );
            }
        });
    }

    // --- EL MÉTODO QUE NECESITAS ---
    @FXML
    public void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) tablaProductos.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- Resto de la lógica (sin cambios) ---
    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioActual"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCategoria.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoria() != null ? cell.getValue().getCategoria().getNombre() : "S/C"));
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isEsCocina() ? "Cocina" : "Barra"));
        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem itemOcultar = new MenuItem("👁 Ocultar/Mostrar"); itemOcultar.setOnAction(e -> toggleActivo(row.getItem()));
            MenuItem itemEliminar = new MenuItem("🗑 Eliminar Definitivamente"); itemEliminar.setOnAction(e -> eliminarProducto(row.getItem()));
            menu.getItems().addAll(itemOcultar, new SeparatorMenuItem(), itemEliminar);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu));
            return row;
        });
    }

    private void cargarProductos() { tablaProductos.getItems().setAll(productoRepo.findAll()); }
    private void cargarCategorias() { cmbCategoria.setItems(FXCollections.observableArrayList(categoriaRepo.findAll())); }

    private void abrirBusquedaRapida() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buscar"); dialog.setHeaderText("Búsqueda Rápida (F3)");
        dialog.showAndWait().ifPresent(texto -> {
            List<Producto> lista = productoRepo.findAll().stream().filter(p -> p.getNombre().toLowerCase().contains(texto.toLowerCase())).collect(Collectors.toList());
            tablaProductos.getItems().setAll(lista);
        });
    }

    @FXML public void guardarProducto() {
        try {
            if (txtNombre.getText().isEmpty() || txtPrecio.getText().isEmpty()) { mostrarAlerta("Error", "Datos incompletos."); return; }
            Producto p = (productoSeleccionado == null) ? new Producto() : productoSeleccionado;
            p.setNombre(txtNombre.getText());
            p.setPrecioActual(new BigDecimal(txtPrecio.getText()));
            String stockStr = txtStock.getText().trim();
            p.setStock(stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
            p.setCategoria(cmbCategoria.getValue());
            p.setEsCocina(chkEsCocina.isSelected());
            if (productoSeleccionado == null) p.setActivo(true);
            productoRepo.save(p);
            mostrarAlerta("Éxito", "Guardado.");
            limpiarFormulario(); cargarProductos();
        } catch (Exception e) { mostrarAlerta("Error", e.getMessage()); }
    }

    public void editarProducto(Producto p) {
        this.productoSeleccionado = p; txtNombre.setText(p.getNombre()); txtPrecio.setText(p.getPrecioActual().toString()); txtStock.setText(String.valueOf(p.getStock()));
        cmbCategoria.setValue(p.getCategoria()); chkEsCocina.setSelected(p.isEsCocina()); btnGuardar.setText("ACTUALIZAR"); btnGuardar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
    }

    @FXML public void limpiarFormulario() {
        productoSeleccionado = null; txtNombre.clear(); txtPrecio.clear(); txtStock.clear(); cmbCategoria.getSelectionModel().clearSelection(); chkEsCocina.setSelected(false);
        btnGuardar.setText("💾 GUARDAR CAMBIOS"); btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
    }

    private void toggleActivo(Producto p) { if(p!=null){p.setActivo(!p.isActivo()); productoRepo.save(p); cargarProductos();} }
    private void eliminarProducto(Producto p) { if(p!=null){ productoRepo.delete(p); cargarProductos(); } }
    private void mostrarAlerta(String t, String c) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.show(); }
}