package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Pedido;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class MenuPrincipalController {

    private final MesaRepository mesaRepo;
    private final PedidoRepository pedidoRepo;
    private final ApplicationContext context;

    @FXML private StackPane zonaCentral;
    @FXML private FlowPane contenedorMesas;

    public MenuPrincipalController(MesaRepository mesaRepo, PedidoRepository pedidoRepo, ApplicationContext context) {
        this.mesaRepo = mesaRepo;
        this.pedidoRepo = pedidoRepo;
        this.context = context;
    }

    @FXML
    public void initialize() {
        mostrarRestaurante(); // Arranca mostrando el mapa
    }

    // --- NAVEGACIÓN SUPERIOR ---

    @FXML
    public void mostrarRestaurante() {
        zonaCentral.getChildren().clear();
        zonaCentral.getChildren().add(contenedorMesas);
        cargarMesas();
    }

    @FXML
    public void mostrarVentas() {
        // Carga tu FXML de tabla simple
        cargarVistaEnCentro("/Views/ventas.fxml");
    }

    @FXML
    public void mostrarProductos() {
        cargarVistaEnCentro("/Views/admin_productos.fxml");
    }



    private void cargarVistaEnCentro(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            loader.setControllerFactory(context::getBean);
            Parent vista = loader.load();

            zonaCentral.getChildren().clear();
            zonaCentral.getChildren().add(vista);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la pantalla: " + rutaFxml);
        }
    }

    // --- LÓGICA DE MESAS (Mapa) ---

    public void cargarMesas() {
        contenedorMesas.getChildren().clear();
        List<Mesa> mesas = mesaRepo.findAll();

        for (Mesa mesa : mesas) {
            Button btn = new Button("MESA " + mesa.getNumero());
            btn.setPrefSize(140, 120);

            btn.getStyleClass().clear();
            btn.getStyleClass().add("button");
            btn.getStyleClass().add("mesa-btn");

            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                btn.setText("MESA " + mesa.getNumero() + "\nOCUPADA\n$" + pedidoAbierto.get().getTotal());
                btn.getStyleClass().add("mesa-ocupada");
            } else {
                btn.setText("MESA " + mesa.getNumero() + "\nLIBRE");
                btn.getStyleClass().add("mesa-libre");
            }

            btn.setOnAction(e -> gestionarClicMesa(mesa));
            contenedorMesas.getChildren().add(btn);
        }
    }

    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) {
            abrirPantallaTomaPedido(mesa);
        } else {
            abrirVentanaConfiguracionMesa(mesa);
        }
    }

    // --- VENTANAS EMERGENTES (Popups) ---

    private void abrirVentanaConfiguracionMesa(Mesa mesa) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/abrirmesa.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            AbrirMesaController controller = loader.getController();
            controller.setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Abrir Mesa " + mesa.getNumero());
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> cargarMesas());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void abrirPantallaTomaPedido(Mesa mesa) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/pedido.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            TomaPedidoController controller = loader.getController();
            controller.setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Pedido - Mesa " + mesa.getNumero());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setOnCloseRequest(e -> e.consume()); // Bloquea la X
            stage.setOnHidden(e -> cargarMesas());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sistema");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}