package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AdminProductosController extends BaseController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    // Campos del Formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkEsCocina;

    // Tabla
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colTipo;

    public AdminProductosController(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarCategorias();
        actualizarLista();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioActual"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Mostrar nombre de la Categoría
        colCategoria.setCellValueFactory(cellData -> {
            Categoria cat = cellData.getValue().getCategoria();
            return new SimpleStringProperty(cat != null ? cat.getNombre() : "Sin Categoría");
        });

        // Mostrar Tipo (Cocina/Barra)
        colTipo.setCellValueFactory(cellData -> {
            boolean esCocina = cellData.getValue().isEsCocina();
            return new SimpleStringProperty(esCocina ? "♨ Cocina" : "🍸 Barra");
        });
    }

    private void cargarCategorias() {
        List<Categoria> categorias = categoriaRepo.findAll();
        cmbCategoria.getItems().setAll(categorias);

        // Convertidor para que el ComboBox muestre el nombre bonito
        cmbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria cat) {
                return cat != null ? cat.getNombre() : "";
            }
            @Override
            public Categoria fromString(String string) { return null; }
        });
    }

    @FXML
    public void guardarProducto() {
        try {
            String nombre = txtNombre.getText();
            String precioStr = txtPrecio.getText();
            String stockStr = txtStock.getText();
            Categoria categoriaSeleccionada = cmbCategoria.getValue();
            boolean esCocina = chkEsCocina.isSelected();

            if (nombre.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty() || categoriaSeleccionada == null) {
                advertencia("Datos Incompletos", "Complete todos los campos.");
                return;
            }

            BigDecimal precio = new BigDecimal(precioStr);
            Integer stock = Integer.parseInt(stockStr);

            Producto nuevo = new Producto();
            nuevo.setNombre(nombre);
            nuevo.setPrecioActual(precio);
            nuevo.setStock(stock);
            nuevo.setCategoria(categoriaSeleccionada);
            nuevo.setEsCocina(esCocina);
            nuevo.setActivo(true);

            productoRepo.save(nuevo);

            mostrarMensaje("✅ Producto Guardado", false);
            limpiarCampos();
            actualizarLista();

        } catch (NumberFormatException e) {
            error("Error", "Precio y Stock deben ser números.");
        } catch (Exception e) {
            error("Error", "No se pudo guardar: " + e.getMessage());
        }
    }

    private void mostrarMensaje(String s, boolean b) {
    }

    private void actualizarLista() {
        tablaProductos.getItems().clear();
        tablaProductos.getItems().addAll(productoRepo.findAll());
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        chkEsCocina.setSelected(false);
    }
}