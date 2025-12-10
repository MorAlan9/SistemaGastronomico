package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AdminProductosController {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    @FXML private ListView<Producto> listaProductos;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;
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

        // --- NUEVO: MENÚ CONTEXTUAL (Clic Derecho) ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemStock = new MenuItem("📦 Modificar Stock");
        itemStock.setOnAction(e -> accionModificarStockRapido());

        MenuItem itemEliminar = new MenuItem("🗑️ Eliminar");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarProducto());

        contextMenu.getItems().addAll(itemStock, new SeparatorMenuItem(), itemEliminar);
        listaProductos.setContextMenu(contextMenu);
    }

    private void cargarCategorias() {
        comboCategoria.getItems().clear();
        comboCategoria.getItems().addAll(categoriaRepo.findAll());
    }

    @FXML
    public void guardarProducto() {
        try {
            String nombre = txtNombre.getText().trim(); // Quitamos espacios extra
            String precioStr = txtPrecio.getText();
            String stockStr = txtStock.getText();
            Categoria categoria = comboCategoria.getValue();

            // 1. Validaciones básicas
            if (nombre.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty() || categoria == null) {
                mostrarMensaje("❌ Complete todos los campos.", true);
                return;
            }

            // 2. VALIDACIÓN DE DUPLICADOS (NUEVO)
            // Preguntamos a la base de datos si ya existe ese nombre
            if (productoRepo.existsByNombreIgnoreCase(nombre)) {
                mostrarMensaje("❌ Error: Ya existe un producto llamado '" + nombre + "'.", true);
                return;
            }

            BigDecimal precio = new BigDecimal(precioStr);
            Integer stock = Integer.parseInt(stockStr);

            Producto nuevo = new Producto();
            nuevo.setNombre(nombre);
            nuevo.setPrecioActual(precio);
            nuevo.setCategoria(categoria);
            nuevo.setStock(stock);

            productoRepo.save(nuevo);

            limpiarCampos();
            mostrarMensaje("✅ Guardado: " + nombre, false);
            actualizarLista();

        } catch (NumberFormatException e) {
            mostrarMensaje("❌ Precio y Stock deben ser números.", true);
        } catch (Exception e) {
            mostrarMensaje("❌ Error: " + e.getMessage(), true);
        }
    }

    // --- LÓGICA CLIC DERECHO: MODIFICAR STOCK ---
    private void accionModificarStockRapido() {
        Producto seleccionado = listaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        TextInputDialog dialog = new TextInputDialog(String.valueOf(seleccionado.getStock()));
        dialog.setTitle("Gestión de Stock");
        dialog.setHeaderText("Modificar Stock para: " + seleccionado.getNombre());
        dialog.setContentText("Nuevo Stock Total:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nuevoStockStr -> {
            try {
                int nuevoStock = Integer.parseInt(nuevoStockStr);
                if (nuevoStock < 0) {
                    mostrarMensaje("❌ El stock no puede ser negativo.", true);
                    return;
                }

                // Actualizamos y guardamos
                seleccionado.setStock(nuevoStock);
                productoRepo.save(seleccionado);

                actualizarLista();
                mostrarMensaje("✅ Stock actualizado a " + nuevoStock, false);

            } catch (NumberFormatException e) {
                mostrarMensaje("❌ Ingrese un número válido.", true);
            }
        });
    }

    @FXML
    public void eliminarProducto() {
        Producto seleccionado = listaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarMensaje("⚠️ Seleccione un producto para eliminar.", true);
            return;
        }

        // --- LÓGICA DE BORRADO LOGICO (SOFT DELETE) ---
        // En lugar de borrarlo, lo apagamos.
        seleccionado.setActivo(false);
        productoRepo.save(seleccionado);

        actualizarLista(); // Se recarga la lista y el producto desaparece visualmente
        mostrarMensaje("🗑️ Producto eliminado (archivado).", false);
    }

    @FXML
    public void actualizarLista() {
        listaProductos.getItems().clear();
        // CAMBIO IMPORTANTE: Usamos findByActivoTrue() en vez de findAll()
        // Así solo vemos los productos "vivos".
        listaProductos.getItems().addAll(productoRepo.findByActivoTrue());
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        comboCategoria.getSelectionModel().clearSelection();
    }

    private void mostrarMensaje(String msg, boolean error) {
        lblInfo.setText(msg);
        if (error) {
            lblInfo.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            lblInfo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }
}
