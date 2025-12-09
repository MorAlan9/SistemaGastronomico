package gastronomia.sistemaGastronomico.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MenuPrincipalController {

    private final ApplicationContext context;

    @FXML private StackPane zonaCentral; // El área cambiante del centro

    public MenuPrincipalController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        // Al iniciar, cargamos la vista del Restaurante (Sectores y Mesas)
        mostrarRestaurante();
    }

    // ==========================================
    // BARRA DE NAVEGACIÓN
    // ==========================================

    @FXML
    public void mostrarRestaurante() {
        // Ahora cargamos el FXML de restaurante (que tiene los sectores y el mapa)
        cargarVistaEnCentro("/Views/restaurante.fxml");
    }

    @FXML
    public void mostrarVentas() {
        cargarVistaEnCentro("/Views/ventas.fxml");
    }

    @FXML
    public void mostrarProductos() {
        cargarVistaEnCentro("/Views/admin_productos.fxml");
    }



    // ==========================================
    // BOTONES DE ACCIÓN RÁPIDA (Si tienes alguno en la barra superior)
    // ==========================================

    @FXML
    public void abrirAdmin() {
        // El botón de configuración abre los productos
        mostrarProductos();
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    private void cargarVistaEnCentro(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            loader.setControllerFactory(context::getBean); // Inyección de dependencias de Spring
            Parent vista = loader.load();

            zonaCentral.getChildren().clear();
            zonaCentral.getChildren().add(vista);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error de Navegación", "No se pudo cargar la pantalla: " + rutaFxml);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sistema");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}