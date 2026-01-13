package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.SectorRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Sector;
import gastronomia.sistemaGastronomico.service.PedidoService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RestauranteController extends BaseController {

    private final MesaRepository mesaRepo;
    private final PedidoRepository pedidoRepo;
    private final SectorRepository sectorRepo;
    private final PedidoService pedidoService;
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

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(60), e -> {
                    if (sectorActual != null) cargarMesasDelSector(sectorActual);
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Platform.runLater(() -> {
            if (contenedorMesas.getScene() != null) {
                contenedorMesas.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F2),
                        this::accionF2AbrirMesa
                );
            }
        });
    }

    @FXML
    public void buscarMesaRapida() {
        String texto = txtBuscarMesa.getText().trim();
        if (texto.isEmpty()) return;
        try {
            int nroMesa = Integer.parseInt(texto);
            Optional<Mesa> mesaOpt = mesaRepo.findByNumero(nroMesa);
            if (mesaOpt.isPresent()) {
                gestionarClicMesa(mesaOpt.get());
                txtBuscarMesa.clear();
            } else {
                toast("Mesa " + nroMesa + " no encontrada", txtBuscarMesa);
            }
        } catch (NumberFormatException e) {
            error("Error", "Ingrese solo números");
        }
    }

    private void accionF2AbrirMesa() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Llamar Mesa");
        dialog.setHeaderText("Abrir Mesa por Número");
        dialog.setContentText("Ingrese N° de Mesa:");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);
                if (mesaOpt.isPresent()) {
                    gestionarClicMesa(mesaOpt.get());
                } else {
                    error("Error", "La mesa " + numero + " no existe.");
                }
            } catch (NumberFormatException e) {
                error("Error", "Ingrese un número válido.");
            }
        });
    }

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

            if (sector.equals(sectorActual)) {
                btn.getStyleClass().add("button-sector-activo");
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
            }

            btn.setOnAction(e -> { this.sectorActual = sector; cargarSectores(); });
            contenedorSectores.getChildren().add(btn);
        }
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
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Ocupada", gente, pedidoAbierto.get()));
                configurarMenuContextual(btn, mesa, pedidoAbierto.get());
            } else {
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Disponible", 0, null));
                configurarMenuContextual(btn, mesa, null);
            }
            btn.setOnAction(e -> gestionarClicMesa(mesa));
            contenedorMesas.getChildren().add(btn);
        }
    }

    private VBox crearGraficoMesa(int numero, String estado, int personas, Pedido pedido) {
        Label lblNum = new Label(String.valueOf(numero));
        lblNum.getStyleClass().add("mesa-numero");

        String txtEstado = estado;
        if(pedido != null) txtEstado += " ($" + pedido.getTotal() + ")";
        Label lblEstado = new Label(txtEstado);
        lblEstado.getStyleClass().add("mesa-estado");

        VBox vBox = new VBox(lblNum, lblEstado);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(2);

        if (personas > 0) {
            Label lblPers = new Label("👥 " + personas);
            lblPers.getStyleClass().add("mesa-badge");
            vBox.getChildren().add(lblPers);
        }

        if (pedido != null) {
            LocalDateTime ahora = LocalDateTime.now();
            boolean cocinaActiva = false;
            boolean esAdicional = false;

            if (pedido.getHoraComanda() != null) {
                if (pedido.getHoraEntrega() == null) cocinaActiva = true;
                else if (pedido.getHoraComanda().isAfter(pedido.getHoraEntrega())) {
                    cocinaActiva = true;
                    esAdicional = true;
                }
            }

            if (pedido.getHoraEntrega() != null) {
                long minComiendo = java.time.Duration.between(pedido.getHoraEntrega(), ahora).toMinutes();
                Label lblComiendo = new Label("🍽 Comiendo: " + minComiendo + "m");
                lblComiendo.getStyleClass().add("mesa-monitor");
                lblComiendo.setStyle("-fx-text-fill: #b3e5fc; -fx-font-weight: bold;");
                vBox.getChildren().add(lblComiendo);
            }

            if (cocinaActiva) {
                long minCocina = java.time.Duration.between(pedido.getHoraComanda(), ahora).toMinutes();
                String texto = esAdicional ? "♨ Cocina (+): " : "♨ Cocina: ";
                Label lblCocina = new Label(texto + minCocina + "m");
                lblCocina.getStyleClass().add("mesa-monitor");

                if (minCocina > 40) lblCocina.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;");
                else if (minCocina > 25) lblCocina.setStyle("-fx-text-fill: #ffeb3b; -fx-font-weight: bold;");
                else lblCocina.setStyle("-fx-text-fill: #b9f6ca;");

                vBox.getChildren().add(lblCocina);
            }

            if (!cocinaActiva && pedido.getHoraEntrega() == null) {
                Label lblTiempo = new Label("🕒 Abierta");
                lblTiempo.getStyleClass().add("mesa-monitor");
                lblTiempo.setStyle("-fx-text-fill: #e0e0e0;");
                vBox.getChildren().add(lblTiempo);
            }
        }
        return vBox;
    }

    private void configurarMenuContextual(Button btn, Mesa mesa, Pedido pedido) {
        ContextMenu contextMenu = new ContextMenu();
        boolean puedeMarcar = false;
        if (pedido != null && pedido.getHoraComanda() != null) {
            if (pedido.getHoraEntrega() == null || pedido.getHoraComanda().isAfter(pedido.getHoraEntrega())) {
                puedeMarcar = true;
            }
        }

        if (puedeMarcar) {
            MenuItem itemEntregado = new MenuItem("🍽 Marcar Comida Entregada");
            itemEntregado.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71;");
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
            cargarMesasDelSector(sectorActual);
        } catch (Exception e) {
            error("Error", e.getMessage());
        }
    }

    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) abrirPantalla(mesa, "/Views/pedido.fxml", true);
        else abrirPantalla(mesa, "/Views/AbrirMesa.fxml", false);
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml, boolean maximizado) {
        try {
            URL url = getClass().getResource(rutaFxml);
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Object controller = loader.getController();
            try { controller.getClass().getMethod("setMesa", Mesa.class).invoke(controller, mesa); } catch(Exception ignored){}

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);

            if (maximizado) stage.setMaximized(true);
            else { stage.initModality(Modality.APPLICATION_MODAL); stage.setResizable(false); }
            stage.setOnHidden(e -> cargarSectores());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void nuevaMesaEnSectorActual() {
        if (sectorActual == null) {
            advertencia("Atención", "Seleccione un sector primero.");
            return;
        }
        try {
            int nroUltima = mesaRepo.findAll().stream().mapToInt(Mesa::getNumero).max().orElse(0);
            Mesa nueva = new Mesa(nroUltima + 1, 4);
            nueva.setSector(sectorActual);
            nueva.setActiva(true);
            mesaRepo.save(nueva);
            cargarMesasDelSector(sectorActual);
            toast("Mesa " + nueva.getNumero() + " creada", contenedorMesas);
        } catch (Exception e) {
            error("Error", "No se pudo crear mesa: " + e.getMessage());
        }
    }

    // --- NUEVO MÉTODO PARA ABRIR REPORTE ---
    @FXML
    public void abrirReporteMozo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ReporteMozo.fxml"));
            // IMPORTANTE: context::getBean para inyectar repositorios
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Reporte de Ventas por Mozo");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            error("Error", "No se pudo abrir el reporte.");
        }
    }

    private void moverMesa(Mesa m) {}
    private void editarMesa(Mesa m) {}

    private void eliminarMesa(Mesa m) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar Mesa " + m.getNumero() + "?");
        if(alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            m.setActiva(false);
            mesaRepo.save(m);
            cargarMesasDelSector(sectorActual);
        }
    }

    @FXML public void nuevoSector() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Sector");
        dialog.setHeaderText("Nombre del nuevo sector:");
        dialog.showAndWait().ifPresent(nombre -> {
            Sector s = new Sector(); s.setNombre(nombre);
            sectorRepo.save(s);
            cargarSectores();
        });
    }
}