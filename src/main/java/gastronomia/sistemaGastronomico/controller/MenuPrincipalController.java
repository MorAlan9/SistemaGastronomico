package gastronomia.sistemaGastronomico.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MenuPrincipalController {

    private final ApplicationContext context;

    @FXML private TabPane tabPanePrincipal;

    // Inyectamos las pestañas que creamos en el FXML
    @FXML private Tab tabRestaurante;
    @FXML private Tab tabVentas;
    @FXML private Tab tabProductos;
    @FXML private Tab tabGastos;

    public MenuPrincipalController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        // Al arrancar, cargamos el contenido dentro de cada pestaña
        cargarContenidoEnTab(tabRestaurante, "/Views/restaurante.fxml");
        cargarContenidoEnTab(tabVentas, "/Views/ventas.fxml");
        cargarContenidoEnTab(tabProductos, "/Views/admin_productos.fxml");

        // Si aún no tienes gastos.fxml, comenta esta línea para que no de error
        // cargarContenidoEnTab(tabGastos, "/Views/gastos.fxml");
    }

    private void cargarContenidoEnTab(Tab tab, String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            loader.setControllerFactory(context::getBean); // Clave para que funcione Spring
            Parent contenido = loader.load();

            // Metemos la pantalla dentro de la pestaña
            tab.setContent(contenido);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error al cargar pestaña: " + rutaFxml);
            tab.setText(tab.getText() + " (Error)");
        }
    }
}