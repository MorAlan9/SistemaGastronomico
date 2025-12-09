package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.SectorRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Sector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component
public class RestauranteController {

    private final MesaRepository mesaRepo;
    private final PedidoRepository pedidoRepo;
    private final SectorRepository sectorRepo;
    private final ApplicationContext context;

    @FXML private HBox contenedorSectores;
    @FXML private FlowPane contenedorMesas;

    private Sector sectorActual;

    public RestauranteController(MesaRepository mesaRepo, PedidoRepository pedidoRepo,
                                 SectorRepository sectorRepo, ApplicationContext context) {
        this.mesaRepo = mesaRepo;
        this.pedidoRepo = pedidoRepo;
        this.sectorRepo = sectorRepo;
        this.context = context;
    }

    @FXML
    public void initialize() {
        System.out.println("✅ RestauranteController iniciado.");
        cargarSectores();
    }

    private void cargarSectores() {
        contenedorSectores.getChildren().clear();
        List<Sector> sectores = sectorRepo.findAll();

        if (sectores.isEmpty()) {
            System.out.println("⚠️ No hay sectores creados.");
            return;
        }

        if (sectorActual == null) sectorActual = sectores.get(0);

        for (Sector sector : sectores) {
            List<Mesa> mesasDelSector = mesaRepo.findBySector(sector);
            long total = mesasDelSector.size();
            long ocupadas = mesasDelSector.stream()
                    .filter(m -> pedidoRepo.findFirstByMesaAndEstado(m, "ABIERTO").isPresent())
                    .count();

            Button btn = new Button(sector.getNombre() + "\n(" + ocupadas + "/" + total + ")");
            btn.setPrefHeight(50);

            // Estilo de pestaña
            if (sector.getId().equals(sectorActual.getId())) {
                btn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
            } else {
                btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: #bdc3c7; -fx-background-radius: 5;");
            }

            btn.setOnAction(e -> {
                this.sectorActual = sector;
                cargarSectores();
            });

            contenedorSectores.getChildren().add(btn);
        }
        cargarMesasDelSector(sectorActual);
    }

    private void cargarMesasDelSector(Sector sector) {
        contenedorMesas.getChildren().clear();
        if (sector == null) return;

        List<Mesa> mesas = mesaRepo.findBySector(sector);

        for (Mesa mesa : mesas) {
            Button btn = new Button();
            btn.setPrefSize(140, 120);
            btn.getStyleClass().clear();
            btn.getStyleClass().add("mesa-btn");

            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                // MESA ROJA (Ocupada)
                btn.getStyleClass().add("mesa-ocupada");
                int gente = pedidoAbierto.get().getComensales() != null ? pedidoAbierto.get().getComensales() : 0;
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "$" + pedidoAbierto.get().getTotal(), gente));
            } else {
                // MESA VERDE (Libre)
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Libre", 0));
            }

            // ACCIÓN AL CLIC
            btn.setOnAction(e -> gestionarClicMesa(mesa));

            contenedorMesas.getChildren().add(btn);
        }
    }

    /**
     * LÓGICA DE APERTURA (Aquí es donde decidimos qué ventana abrir)
     */
    private void gestionarClicMesa(Mesa mesa) {
        System.out.println("👆 Clic detectado en Mesa " + mesa.getNumero());

        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

        if (pedidoAbierto.isPresent()) {
            System.out.println("   -> La mesa está OCUPADA. Abriendo Pedido...");
            abrirPantalla(mesa, "/Views/pedido.fxml");
        } else {
            System.out.println("   -> La mesa está LIBRE. Abriendo Configuración...");
            abrirPantalla(mesa, "/Views/AbrirMesa.fxml");
        }
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml) {
        try {
            // VERIFICACIÓN DE RUTA
            URL url = getClass().getResource(rutaFxml);
            if (url == null) {
                System.err.println("❌ ERROR: No encuentro el archivo: " + rutaFxml);
                mostrarAlerta("Error de Archivo", "No se encuentra la vista: " + rutaFxml);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            // Pasamos la mesa al controlador correspondiente
            Object controller = loader.getController();

            if (controller instanceof AbrirMesaController) {
                ((AbrirMesaController) controller).setMesa(mesa);
            } else if (controller instanceof TomaPedidoController) {
                ((TomaPedidoController) controller).setMesa(mesa);
            }

            Stage stage = new Stage();
            stage.setTitle("Mesa " + mesa.getNumero());
            stage.setScene(new Scene(root));


            // Al cerrar, refrescar mapa
            stage.setOnHidden(e -> cargarSectores());

            stage.show();

        } catch (Exception e) {
            e.printStackTrace(); // ¡MIRA LA CONSOLA SI FALLA AQUÍ!
            mostrarAlerta("Error Técnico", "Falló al abrir ventana: " + e.getMessage());
        }
    }

    private VBox crearGraficoMesa(int numero, String estado, int personas) {
        javafx.scene.control.Label lblNum = new javafx.scene.control.Label(String.valueOf(numero));
        lblNum.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");

        javafx.scene.control.Label lblEstado = new javafx.scene.control.Label(estado);
        lblEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");

        VBox vBox = new VBox(lblNum, lblEstado);
        vBox.setAlignment(javafx.geometry.Pos.CENTER);

        if (personas > 0) {
            javafx.scene.control.Label lblPers = new javafx.scene.control.Label("👥 " + personas);
            lblPers.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 2;");
            vBox.getChildren().add(lblPers);
        }
        return vBox;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sistema");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    public void nuevaMesaEnSectorActual() {
        if (sectorActual == null) return;
        int numero = (int) (mesaRepo.count() + 1);
        Mesa nueva = new Mesa(numero, 4);
        nueva.setSector(sectorActual);
        mesaRepo.save(nueva);
        cargarSectores();
    }

    @FXML
    public void nuevoSector() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Sector");
        dialog.setHeaderText("Crear nueva zona");
        dialog.setContentText("Nombre:");
        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                sectorRepo.save(new Sector(nombre));
                cargarSectores();
            }
        });
    }
}