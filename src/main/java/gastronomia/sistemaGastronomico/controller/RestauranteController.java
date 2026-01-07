package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.SectorRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Sector;
import gastronomia.sistemaGastronomico.service.PedidoService; // Importar Service
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RestauranteController extends BaseController {

    private final MesaRepository mesaRepo;
    private final PedidoRepository pedidoRepo;
    private final SectorRepository sectorRepo;
    private final PedidoService pedidoService; // Agregamos el Service
    private final ApplicationContext context;

    @FXML private HBox contenedorSectores;
    @FXML private FlowPane contenedorMesas;
    @FXML private TextField txtBuscarMesa;

    private Sector sectorActual;

    public RestauranteController(MesaRepository mesaRepo, PedidoRepository pedidoRepo,
                                 SectorRepository sectorRepo, PedidoService pedidoService,
                                 ApplicationContext context) {
        this.mesaRepo = mesaRepo;
        this.pedidoRepo = pedidoRepo;
        this.sectorRepo = sectorRepo;
        this.pedidoService = pedidoService;
        this.context = context;
    }

    @FXML
    public void initialize() {
        cargarSectores();
        configurarAtajoTeclado();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(60), e -> {
                    if (sectorActual != null) cargarMesasDelSector(sectorActual);
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ... (Métodos buscarMesaRapida y configurarAtajoTeclado iguales) ...
    private void configurarAtajoTeclado() { /* Igual que antes */ }
    @FXML public void buscarMesaRapida() { /* Igual que antes */ }

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
            List<Mesa> mesasDelSector = mesaRepo.findBySectorAndActivaTrue(sector);
            long total = mesasDelSector.size();
            long ocupadas = mesasDelSector.stream().filter(m -> pedidoRepo.findFirstByMesaAndEstado(m, "ABIERTO").isPresent()).count();

            Button btn = new Button(sector.getNombre() + "\n(" + ocupadas + "/" + total + ")");
            btn.getStyleClass().add("button-sector");
            if (sector.equals(sectorActual)) btn.getStyleClass().add("button-sector-activo");

            btn.setOnAction(e -> { this.sectorActual = sector; cargarSectores(); });
            contenedorSectores.getChildren().add(btn);
        }

        Button btnAdd = new Button("+");
        btnAdd.getStyleClass().add("button-add-sector");
        btnAdd.setOnAction(e -> nuevoSector());
        contenedorSectores.getChildren().add(btnAdd);

        cargarMesasDelSector(sectorActual);
    }

    private void cargarMesasDelSector(Sector sector) {
        contenedorMesas.getChildren().clear();
        if (sector == null) return;
        List<Mesa> mesas = mesaRepo.findBySectorAndActivaTrue(sector);

        for (Mesa mesa : mesas) {
            Button btn = new Button();
            btn.setPrefSize(160, 140);
            btn.getStyleClass().clear();
            btn.getStyleClass().add("mesa-btn");

            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                btn.getStyleClass().add("mesa-ocupada");
                int gente = pedidoAbierto.get().getComensales() != null ? pedidoAbierto.get().getComensales() : 0;
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Ocupada ($" + pedidoAbierto.get().getTotal() + ")", gente, pedidoAbierto.get()));

                // Configurar menú contextual CON el pedido
                configurarMenuContextual(btn, mesa, pedidoAbierto.get());
            } else {
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Disponible", 0, null));

                // Configurar menú contextual SIN pedido
                configurarMenuContextual(btn, mesa, null);
            }
            btn.setOnAction(e -> gestionarClicMesa(mesa));
            contenedorMesas.getChildren().add(btn);
        }
    }

    // --- MONITOR INTELIGENTE ACTUALIZADO ---
    private VBox crearGraficoMesa(int numero, String estado, int personas, Pedido pedido) {
        Label lblNum = new Label(String.valueOf(numero));
        lblNum.getStyleClass().add("mesa-numero");
        Label lblEstado = new Label(estado);
        lblEstado.getStyleClass().add("mesa-estado");

        VBox vBox = new VBox(lblNum, lblEstado);
        vBox.setAlignment(javafx.geometry.Pos.CENTER);
        vBox.setSpacing(2);

        if (personas > 0) {
            Label lblPers = new Label("👥 " + personas);
            lblPers.getStyleClass().add("mesa-badge");
            vBox.getChildren().add(lblPers);
        }

        if (pedido != null) {
            LocalDateTime ahora = LocalDateTime.now();

            // PRIORIDAD 1: YA COMIENDO (Se entregó la comida)
            if (pedido.getHoraEntrega() != null) {
                long minComiendo = java.time.Duration.between(pedido.getHoraEntrega(), ahora).toMinutes();
                Label lblComiendo = new Label("🍽 Comiendo: " + minComiendo + "m");
                lblComiendo.getStyleClass().add("mesa-monitor");
                // Color Azul/Verde relajado
                lblComiendo.setStyle("-fx-text-fill: #b3e5fc; -fx-font-weight: bold;");
                vBox.getChildren().add(lblComiendo);
            }
            // PRIORIDAD 2: EN COCINA (Esperando comida)
            else if (pedido.getHoraComanda() != null) {
                long minCocina = java.time.Duration.between(pedido.getHoraComanda(), ahora).toMinutes();
                Label lblCocina = new Label("♨️ Cocina: " + minCocina + "m");
                lblCocina.getStyleClass().add("mesa-monitor");

                if (minCocina > 40) lblCocina.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.15);");
                else if (minCocina > 25) lblCocina.setStyle("-fx-text-fill: #ffeb3b; -fx-font-weight: bold;");
                else lblCocina.setStyle("-fx-text-fill: #b9f6ca;");

                vBox.getChildren().add(lblCocina);
            }
            // PRIORIDAD 3: SOLO ABIERTA
            else {
                LocalDateTime inicio = LocalDateTime.of(pedido.getFecha(), pedido.getHora());
                long minTotal = java.time.Duration.between(inicio, ahora).toMinutes();
                Label lblTiempo = new Label("🕒 Abierta: " + minTotal + "m");
                lblTiempo.getStyleClass().add("mesa-monitor");
                lblTiempo.setStyle("-fx-text-fill: #e0e0e0;");
                vBox.getChildren().add(lblTiempo);
            }
        }
        return vBox;
    }

    // --- NUEVO MENU CONTEXTUAL ---
    private void configurarMenuContextual(Button btn, Mesa mesa, Pedido pedido) {
        ContextMenu contextMenu = new ContextMenu();

        // OPCIÓN: MARCAR ENTREGADO (Solo si hay pedido, está comandado y NO entregado aún)
        if (pedido != null && pedido.getHoraComanda() != null && pedido.getHoraEntrega() == null) {
            MenuItem itemEntregado = new MenuItem("🍽 Marcar Comida Entregada");
            itemEntregado.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71;"); // Verde
            itemEntregado.setOnAction(e -> accionMarcarEntregado(pedido));
            contextMenu.getItems().add(itemEntregado);
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem itemMover = new MenuItem("↔ Mover Mesa");
        itemMover.setOnAction(e -> moverMesa(mesa));

        MenuItem itemEditar = new MenuItem("✏️ Editar Mesa");
        itemEditar.setOnAction(e -> editarMesa(mesa));

        MenuItem itemEliminar = new MenuItem("🗑️ Eliminar Mesa");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarMesa(mesa));

        contextMenu.getItems().addAll(itemMover, new SeparatorMenuItem(), itemEditar, itemEliminar);
        btn.setContextMenu(contextMenu);
    }

    private void accionMarcarEntregado(Pedido pedido) {
        try {
            pedidoService.marcarEntrega(pedido.getId());
            toast("🍽 Comida entregada en Mesa " + pedido.getMesa().getNumero(), contenedorMesas);
            cargarSectores(); // Refrescar visual
        } catch (Exception e) {
            error("Error", e.getMessage());
        }
    }

    // ... (El resto de métodos moverMesa, gestionarClic, etc. siguen igual que antes) ...
    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) abrirPantalla(mesa, "/Views/pedido.fxml", true);
        else abrirPantalla(mesa, "/Views/AbrirMesa.fxml", false);
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml, boolean maximizado) {
        try {
            URL url = getClass().getResource(rutaFxml);
            if (url == null) { error("Error", "No encuentro: " + rutaFxml); return; }
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AbrirMesaController) ((AbrirMesaController) controller).setMesa(mesa);
            else if (controller instanceof TomaPedidoController) ((TomaPedidoController) controller).setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Mesa " + mesa.getNumero());
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);

            if (maximizado) stage.setMaximized(true);
            else { stage.initModality(Modality.APPLICATION_MODAL); stage.setResizable(false); }
            stage.setOnHidden(e -> cargarSectores());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); error("Error", e.getMessage()); }
    }

    // Métodos mover, editar, eliminar, nuevo sector (iguales a tu versión anterior)...
    private void moverMesa(Mesa mesaOrigen) { /* Código anterior */ }
    private void editarMesa(Mesa mesa) { /* Código anterior */ }
    private void eliminarMesa(Mesa mesa) { /* Código anterior */ }
    @FXML public void nuevaMesaEnSectorActual() { /* Código anterior */ }
    @FXML public void nuevoSector() { /* Código anterior */ }
}