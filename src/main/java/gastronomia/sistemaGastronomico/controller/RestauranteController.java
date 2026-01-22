package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.SectorRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Sector;
import gastronomia.sistemaGastronomico.service.PedidoService;
import gastronomia.sistemaGastronomico.utils.DialogoPinMozo;
import gastronomia.sistemaGastronomico.utils.SesionGlobal;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component
public class RestauranteController extends BaseController {

    private final MesaRepository mesaRepo;
    private final PedidoRepository pedidoRepo;
    private final SectorRepository sectorRepo;
    private final PedidoService pedidoService;
    private final ApplicationContext context;
    private final MozoRepository mozoRepo;

    @FXML private HBox contenedorSectores;
    @FXML private FlowPane contenedorMesas;
    @FXML private TextField txtBuscarMesa;

    private Sector sectorActual;

    public RestauranteController(MesaRepository mesaRepo, PedidoRepository pedidoRepo,
                                 SectorRepository sectorRepo, PedidoService pedidoService,
                                 ApplicationContext context, MozoRepository mozoRepo) {
        this.mesaRepo = mesaRepo;
        this.pedidoRepo = pedidoRepo;
        this.sectorRepo = sectorRepo;
        this.pedidoService = pedidoService;
        this.context = context;
        this.mozoRepo = mozoRepo;
    }

    @FXML
    public void initialize() {
        cargarSectores();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(60), e -> {
                    if (sectorActual != null) cargarMesasDelSector(sectorActual);
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Platform.runLater(() -> {
            if (contenedorMesas.getScene() != null) {
                // F9 - Volver al menú
                contenedorMesas.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F9),
                        this::volverAlMenu
                );
                // F2 - Abrir mesa
                contenedorMesas.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F2),
                        this::accionF2AbrirMesa
                );
            }
        });
    }

    // --- MÉTODOS QUE TE FALTABAN ---

    private void cargarSectores() {
        contenedorSectores.getChildren().clear();
        List<Sector> sectores = sectorRepo.findAll();
        if (sectores.isEmpty()) sectorActual = null;
        else if (sectorActual == null) sectorActual = sectores.get(0);
        else {
            boolean existe = sectores.stream().anyMatch(s -> s.getId().equals(sectorActual.getId()));
            if (!existe) sectorActual = sectores.get(0);
        }
        for (Sector sector : sectores) {
            Button btn = new Button(sector.getNombre());
            btn.getStyleClass().add("button-sector");
            if (sector.equals(sectorActual)) {
                btn.getStyleClass().add("button-sector-activo");
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
            }
            btn.setOnAction(e -> { this.sectorActual = sector; cargarSectores(); });
            contenedorSectores.getChildren().add(btn);
        }
        cargarMesasDelSector(sectorActual);
    }

    private void accionF2AbrirMesa() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Llamar Mesa");
        dialog.setHeaderText("Ingrese N° de Mesa:");
        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);
                if (mesaOpt.isPresent()) gestionarClicMesa(mesaOpt.get());
            } catch (Exception e) {}
        });
    }

    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) {
            abrirPantalla(mesa, "/Views/pedido.fxml", true);
        } else {
            Mozo mozoIdentificado = DialogoPinMozo.solicitar(mozoRepo).orElse(null);
            if (mozoIdentificado != null) {
                SesionGlobal.mozoActual = mozoIdentificado;
                abrirPantalla(mesa, "/Views/AbrirMesa.fxml", false);
            }
        }
    }

    private void cargarMesasDelSector(Sector sector) {
        contenedorMesas.getChildren().clear();
        if (sector == null) return;
        List<Mesa> mesas = mesaRepo.findBySectorAndActivaTrue(sector);
        for (Mesa mesa : mesas) {
            Button btn = new Button();
            btn.setPrefSize(160, 140);
            btn.getStyleClass().add("mesa-btn");
            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                btn.getStyleClass().add("mesa-ocupada");
                int gente = pedidoAbierto.get().getComensales() != null ? pedidoAbierto.get().getComensales() : 0;
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Ocupada", gente));
            } else {
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Disponible", 0));
            }
            btn.setOnAction(e -> gestionarClicMesa(mesa));
            contenedorMesas.getChildren().add(btn);
        }
    }

    private VBox crearGraficoMesa(int numero, String estado, int personas) {
        VBox v = new VBox(new Label(String.valueOf(numero)), new Label(estado));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) contenedorMesas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml, boolean maximizado) {
        try {
            URL url = getClass().getResource(rutaFxml);
            if(url==null) return;
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Object controller = loader.getController();
            try { controller.getClass().getMethod("setMesa", Mesa.class).invoke(controller, mesa); } catch(Exception ignored){}
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            if(maximizado) stage.setMaximized(true);
            else stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> cargarSectores());
            stage.show();
        } catch(Exception e) { e.printStackTrace(); }
    }

    // Stubs para evitar errores
    @FXML public void nuevaMesaEnSectorActual() {}
    @FXML public void nuevoSector() {}
    @FXML public void buscarMesaRapida() {}
    @FXML public void abrirReporteMozo() {}
}