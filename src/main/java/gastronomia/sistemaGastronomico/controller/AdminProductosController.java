package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminProductosController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colTipo;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;

    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkEsCocina;
    @FXML private Button btnGuardar;

    private Producto productoSeleccionado;

    // Constructor para inyección de dependencias
    public AdminProductosController(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarCategorias();
        cargarProductos();

        // Doble click para editar
        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Producto p = tablaProductos.getSelectionModel().getSelectedItem();
                if (p != null) editarProducto(p);
            }
        });

        // F3 para buscar
        Platform.runLater(() -> {
            if (tablaProductos.getScene() != null) {
                tablaProductos.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F3),
                        this::abrirBusquedaRapida
                );
            }
        });
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioActual"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        colCategoria.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategoria() != null ? cell.getValue().getCategoria().getNombre() : "S/C"));

        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isEsCocina() ? "Cocina" : "Barra"));

        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem itemOcultar = new MenuItem("👁 Ocultar/Mostrar");
            itemOcultar.setOnAction(e -> toggleActivo(row.getItem()));
            MenuItem itemEliminar = new MenuItem("🗑 Eliminar Definitivamente");
            itemEliminar.setOnAction(e -> eliminarProducto(row.getItem()));
            menu.getItems().addAll(itemOcultar, new SeparatorMenuItem(), itemEliminar);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu));
            return row;
        });
    }

    private void cargarProductos() {
        tablaProductos.getItems().setAll(productoRepo.findAll());
    }

    private void cargarCategorias() {
        cmbCategoria.setItems(FXCollections.observableArrayList(categoriaRepo.findAll()));
    }

    private void abrirBusquedaRapida() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buscar");
        dialog.setHeaderText("Búsqueda Rápida (F3)");
        dialog.showAndWait().ifPresent(texto -> {
            List<Producto> lista = productoRepo.findAll().stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(texto.toLowerCase()))
                    .collect(Collectors.toList());
            tablaProductos.getItems().setAll(lista);
        });
    }

    @FXML
    public void guardarProducto() {
        try {
            if (txtNombre.getText().isEmpty() || txtPrecio.getText().isEmpty()) {
                mostrarAlerta("Error", "Nombre y Precio son obligatorios.");
                return;
            }
            Producto p = (productoSeleccionado == null) ? new Producto() : productoSeleccionado;
            p.setNombre(txtNombre.getText());
            p.setPrecioActual(new BigDecimal(txtPrecio.getText()));
            String stockStr = txtStock.getText().trim();
            p.setStock(stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
            p.setCategoria(cmbCategoria.getValue());
            p.setEsCocina(chkEsCocina.isSelected());
            if (productoSeleccionado == null) p.setActivo(true);

            productoRepo.save(p);
            mostrarAlerta("Éxito", "Producto guardado.");
            limpiarFormulario();
            cargarProductos();
        } catch (Exception e) {
            mostrarAlerta("Error", "Revise los datos: " + e.getMessage());
        }
    }

    public void editarProducto(Producto p) {
        this.productoSeleccionado = p;
        txtNombre.setText(p.getNombre());
        txtPrecio.setText(p.getPrecioActual().toString());
        txtStock.setText(String.valueOf(p.getStock()));
        cmbCategoria.setValue(p.getCategoria());
        chkEsCocina.setSelected(p.isEsCocina());
        btnGuardar.setText("ACTUALIZAR");
        btnGuardar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
    }

    @FXML
    public void limpiarFormulario() {
        productoSeleccionado = null;
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        chkEsCocina.setSelected(false);
        btnGuardar.setText("💾 GUARDAR CAMBIOS");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
    }

    private void toggleActivo(Producto p) {
        if(p == null) return;
        p.setActivo(!p.isActivo());
        productoRepo.save(p);
        cargarProductos();
        mostrarAlerta("Info", "Producto " + (p.isActivo() ? "VISIBLE" : "OCULTO"));
    }

    private void eliminarProducto(Producto p) {
        if(p == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar " + p.getNombre() + "?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                productoRepo.delete(p);
                cargarProductos();
            } catch (Exception e) {
                mostrarAlerta("Error", "No se puede eliminar (tiene ventas). Use 'Ocultar'.");
            }
        }
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setContentText(contenido); a.show();
    }
}