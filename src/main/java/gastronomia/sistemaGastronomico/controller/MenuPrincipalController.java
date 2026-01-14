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

        // 1. Restaurante
        cargarContenidoEnTab(tabRestaurante, "/Views/Restaurante.fxml");

        // 2. Ventas
        cargarContenidoEnTab(tabVentas, "/Views/Ventas.fxml");

        // 3. Productos (CORREGIDO: Sin el guion bajo)
        // Asegúrate de que el archivo en resources/Views se llame 'AdminProductos.fxml'
        cargarContenidoEnTab(tabProductos, "/Views/AdminProductos.fxml");

        // 4. Gastos (Descomentar cuando exista)
        // cargarContenidoEnTab(tabGastos, "/Views/gastos.fxml");
    }

    private void cargarContenidoEnTab(Tab tab, String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));

            // ESTA LÍNEA ES LA CLAVE: Conecta el Controller con la Base de Datos
            loader.setControllerFactory(context::getBean);

            Parent contenido = loader.load();

            // Metemos la pantalla dentro de la pestaña
            tab.setContent(contenido);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error al cargar pestaña: " + rutaFxml);
            // Esto te avisará visualmente si falla
            tab.setText("ERROR AL CARGAR");
            tab.setDisable(true);
        }
    }
}