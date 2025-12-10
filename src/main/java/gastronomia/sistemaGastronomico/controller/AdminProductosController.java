package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdminProductosController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    @FXML private ListView<Producto> listaProductos;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock; // Campo nuevo
    @FXML private ComboBox<Categoria> comboCategoria;
    @FXML private Label lblInfo;

    public AdminProductosController(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @FXML
    public void initialize() {
        cargarCategorias();
        actualizarLista();
    }

    private void cargarCategorias() {
        comboCategoria.getItems().clear();
        comboCategoria.getItems().addAll(categoriaRepo.findAll());
    }

    @FXML
    public void guardarProducto() {
        try {
            String nombre = txtNombre.getText();
            String precioStr = txtPrecio.getText();
            String stockStr = txtStock.getText();
            Categoria categoria = comboCategoria.getValue();

            if (nombre.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty() || categoria == null) {
                lblInfo.setText("❌ Complete todos los campos.");
                lblInfo.setStyle("-fx-text-fill: red;");
                return;
            }

            BigDecimal precio = new BigDecimal(precioStr);
            Integer stock = Integer.parseInt(stockStr); // Convertimos stock

            Producto nuevo = new Producto();
            nuevo.setNombre(nombre);
            nuevo.setPrecioActual(precio);
            nuevo.setCategoria(categoria);
            nuevo.setStock(stock); // Guardamos stock

            productoRepo.save(nuevo);

            txtNombre.clear();
            txtPrecio.clear();
            txtStock.clear();
            comboCategoria.getSelectionModel().clearSelection();

            lblInfo.setText("✅ Guardado: " + nombre);
            lblInfo.setStyle("-fx-text-fill: green;");

            actualizarLista(); // Recargamos la lista automáticamente

        } catch (NumberFormatException e) {
            lblInfo.setText("❌ Precio y Stock deben ser números.");
        } catch (Exception e) {
            lblInfo.setText("❌ Error: " + e.getMessage());
        }
    }

    @FXML
    public void eliminarProducto() {
        Producto seleccionado = listaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            productoRepo.delete(seleccionado);
            actualizarLista();
            lblInfo.setText("🗑️ Eliminado.");
        }
    }

    // Método público para el botón 🔄
    @FXML
    public void actualizarLista() {
        listaProductos.getItems().clear();
        listaProductos.getItems().addAll(productoRepo.findAll());
        System.out.println("Lista de productos actualizada desde BD.");
    }
}