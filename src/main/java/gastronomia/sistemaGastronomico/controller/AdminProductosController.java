package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AdminProductosController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo; // <--- Nuevo

    @FXML private ListView<Producto> listaProductos;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private ComboBox<Categoria> comboCategoria; // <--- Ahora es de objetos Categoria
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
        // Trae las categorías reales de la Base de Datos
        comboCategoria.getItems().addAll(categoriaRepo.findAll());
    }

    @FXML
    public void guardarProducto() {
        try {
            String nombre = txtNombre.getText();
            String precioStr = txtPrecio.getText();
            Categoria categoria = comboCategoria.getValue(); // Obtenemos el objeto seleccionado

            if (nombre.isEmpty() || precioStr.isEmpty() || categoria == null) {
                lblInfo.setText("❌ Complete todos los campos.");
                lblInfo.setStyle("-fx-text-fill: red;");
                return;
            }

            BigDecimal precio = new BigDecimal(precioStr);

            Producto nuevo = new Producto(nombre, precio, categoria);
            productoRepo.save(nuevo);

            txtNombre.clear();
            txtPrecio.clear();
            lblInfo.setText("✅ Guardado: " + nombre);
            lblInfo.setStyle("-fx-text-fill: green;");

            actualizarLista();

        } catch (NumberFormatException e) {
            lblInfo.setText("❌ El precio debe ser numérico.");
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

    private void actualizarLista() {
        listaProductos.getItems().clear();
        listaProductos.getItems().addAll(productoRepo.findAll());
    }
}