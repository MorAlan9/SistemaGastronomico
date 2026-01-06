package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.SectorRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Sector;
import gastronomia.sistemaGastronomico.utils.AlertaHelper; // Importamos tus Utils
import gastronomia.sistemaGastronomico.utils.Toast;       // Importamos tus Utils
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
    @FXML private TextField txtBuscarMesa;

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
        cargarSectores();
        configurarAtajoTeclado();
    }

    private void configurarAtajoTeclado() {
        contenedorMesas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F3) {
                        txtBuscarMesa.requestFocus();
                        txtBuscarMesa.selectAll();
                        event.consume();
                    }
                });
            }
        });
    }

    @FXML
    public void buscarMesaRapida() {
        String texto = txtBuscarMesa.getText().trim();
        if (texto.isEmpty()) return;

        try {
            int numero = Integer.parseInt(texto);
            Optional<Mesa> mesaOpt = mesaRepo.findFirstByNumeroAndActivaTrue(numero);

            if (mesaOpt.isPresent()) {
                gestionarClicMesa(mesaOpt.get());
                txtBuscarMesa.clear();
            } else {
                AlertaHelper.mostrarAlerta("No encontrada", "La Mesa N° " + numero + " no existe o no está activa.", Alert.AlertType.WARNING);
            }
        } catch (NumberFormatException e) {
            AlertaHelper.mostrarAlerta("Error", "Ingrese un número válido.", Alert.AlertType.ERROR);
        }
    }

    private void cargarSectores() {
        contenedorSectores.getChildren().clear();
        List<Sector> sectores = sectorRepo.findAll();

        if (sectores.isEmpty()) {
            sectorActual = null;
        } else if (sectorActual == null) {
            sectorActual = sectores.get(0);
        } else {
            boolean existe = sectores.stream().anyMatch(s -> s.getId().equals(sectorActual.getId()));
            if (!existe) sectorActual = sectores.get(0);
        }

        for (Sector sector : sectores) {
            List<Mesa> mesasDelSector = mesaRepo.findBySectorAndActivaTrue(sector);
            long total = mesasDelSector.size();
            long ocupadas = mesasDelSector.stream()
                    .filter(m -> pedidoRepo.findFirstByMesaAndEstado(m, "ABIERTO").isPresent())
                    .count();

            Button btn = new Button(sector.getNombre() + "\n(" + ocupadas + "/" + total + ")");

            // UX: Estilos de botones de sector
            btn.getStyleClass().add("button-sector");
            if (sector != null && sectorActual != null && sector.getId().equals(sectorActual.getId())) {
                btn.getStyleClass().add("button-sector-activo");
            }

            btn.setOnAction(e -> {
                this.sectorActual = sector;
                cargarSectores();
            });
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

            // UX: Limpiamos y asignamos clase base
            btn.getStyleClass().clear();
            btn.getStyleClass().add("mesa-btn");

            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                // UX: Mesa Ocupada (ROJA)
                btn.getStyleClass().add("mesa-ocupada");
                int gente = pedidoAbierto.get().getComensales() != null ? pedidoAbierto.get().getComensales() : 0;
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Ocupada ($" + pedidoAbierto.get().getTotal() + ")", gente));
            } else {
                // UX: Mesa Libre (VERDE)
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Disponible", 0));
            }

            btn.setOnAction(e -> gestionarClicMesa(mesa));
            configurarMenuContextual(btn, mesa); // Aquí se agrega la opción "Mover"

            contenedorMesas.getChildren().add(btn);
        }
    }

    private VBox crearGraficoMesa(int numero, String estado, int personas) {
        Label lblNum = new Label(String.valueOf(numero));
        lblNum.getStyleClass().add("mesa-numero");

        Label lblEstado = new Label(estado);
        lblEstado.getStyleClass().add("mesa-estado");

        VBox vBox = new VBox(lblNum, lblEstado);
        vBox.setAlignment(javafx.geometry.Pos.CENTER);
        vBox.setSpacing(5);

        if (personas > 0) {
            Label lblPers = new Label("👥 " + personas);
            lblPers.getStyleClass().add("mesa-badge");
            vBox.getChildren().add(lblPers);
        }
        return vBox;
    }

    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) abrirPantalla(mesa, "/Views/pedido.fxml", true); // Maximizada
        else abrirPantalla(mesa, "/Views/AbrirMesa.fxml", false); // Ventana chica
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml, boolean maximizado) {
        try {
            URL url = getClass().getResource(rutaFxml);
            if (url == null) {
                AlertaHelper.mostrarAlerta("Error", "No encuentro: " + rutaFxml, Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AbrirMesaController) ((AbrirMesaController) controller).setMesa(mesa);
            else if (controller instanceof TomaPedidoController) ((TomaPedidoController) controller).setMesa(mesa);

            Stage stage = new Stage();
            stage.setTitle("Mesa " + mesa.getNumero());

            // UX: Inyectar CSS global al nuevo Stage
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm());
            } catch (Exception e) { System.err.println("No se pudo cargar estilos.css para el stage"); }

            stage.setScene(scene);

            if (maximizado) {
                stage.setMaximized(true);
            } else {
                stage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás
                stage.setResizable(false);
            }

            stage.setOnHidden(e -> cargarSectores());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertaHelper.mostrarAlerta("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // --- NUEVA FUNCIONALIDAD: MENÚ CONTEXTUAL ---
    private void configurarMenuContextual(Button btn, Mesa mesa) {
        ContextMenu contextMenu = new ContextMenu();

        // OPCIÓN 1: MOVER MESA (Nueva)
        MenuItem itemMover = new MenuItem("↔ Mover Mesa");
        itemMover.setStyle("-fx-font-weight: bold;"); // Resaltar
        itemMover.setOnAction(e -> moverMesa(mesa));

        // OPCIÓN 2: EDITAR
        MenuItem itemEditar = new MenuItem("✏️ Editar Mesa");
        itemEditar.setOnAction(e -> editarMesa(mesa));

        // OPCIÓN 3: ELIMINAR
        MenuItem itemEliminar = new MenuItem("🗑️ Eliminar Mesa");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarMesa(mesa));

        contextMenu.getItems().addAll(itemMover, new SeparatorMenuItem(), itemEditar, itemEliminar);
        btn.setContextMenu(contextMenu);
    }

    // --- NUEVA LÓGICA: MOVER MESA ---
    private void moverMesa(Mesa mesaOrigen) {
        // 1. Validar que haya qué mover
        Optional<Pedido> pedidoOrigenOpt = pedidoRepo.findFirstByMesaAndEstado(mesaOrigen, "ABIERTO");
        if (pedidoOrigenOpt.isEmpty()) {
            AlertaHelper.mostrarAlerta("Aviso", "Esta mesa está libre, no hay pedidos para mover.", Alert.AlertType.INFORMATION);
            return;
        }

        // 2. Pedir destino
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mover Mesa");
        dialog.setHeaderText("Mover Mesa " + mesaOrigen.getNumero() + " a...");
        dialog.setContentText("Número de mesa destino:");
        AlertaHelper.estilizarDialogo(dialog); // Estilo lindo

        dialog.showAndWait().ifPresent(texto -> {
            try {
                int numDestino = Integer.parseInt(texto);

                // 3. Buscar mesa destino
                Optional<Mesa> mesaDestinoOpt = mesaRepo.findFirstByNumeroAndActivaTrue(numDestino);

                if (mesaDestinoOpt.isEmpty()) {
                    AlertaHelper.mostrarAlerta("Error", "La mesa " + numDestino + " no existe.", Alert.AlertType.ERROR);
                    return;
                }

                Mesa mesaDestino = mesaDestinoOpt.get();

                // 4. Validar que destino esté libre
                if (pedidoRepo.findFirstByMesaAndEstado(mesaDestino, "ABIERTO").isPresent()) {
                    AlertaHelper.mostrarAlerta("Ocupada", "La mesa " + numDestino + " ya está ocupada.", Alert.AlertType.WARNING);
                    return;
                }

                // 5. CAMBIAR DE MESA (Transferencia)
                Pedido pedido = pedidoOrigenOpt.get();
                pedido.setMesa(mesaDestino);
                pedidoRepo.save(pedido);

                // 6. Feedback visual
                Toast.mostrar("✅ Mesa traslada a la " + numDestino, (Stage) contenedorMesas.getScene().getWindow());
                cargarSectores(); // Refrescar

            } catch (NumberFormatException e) {
                AlertaHelper.mostrarAlerta("Error", "Ingrese un número válido.", Alert.AlertType.ERROR);
            } catch (Exception e) {
                AlertaHelper.mostrarAlerta("Error", "No se pudo mover: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editarMesa(Mesa mesa) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Mesa " + mesa.getNumero());
        AlertaHelper.estilizarDialogo(dialog); // Estilo

        TextField txtNumero = new TextField(String.valueOf(mesa.getNumero()));
        TextField txtCapacidad = new TextField(String.valueOf(mesa.getCapacidad()));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Nuevo Número:"), 0, 0); grid.add(txtNumero, 1, 0);
        grid.add(new Label("Sillas:"), 0, 1); grid.add(txtCapacidad, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                mesa.setNumero(Integer.parseInt(txtNumero.getText()));
                mesa.setCapacidad(Integer.parseInt(txtCapacidad.getText()));
                mesaRepo.save(mesa);
                cargarSectores();
            } catch (Exception e) {
                AlertaHelper.mostrarAlerta("Error", "Datos inválidos", Alert.AlertType.ERROR);
            }
        }
    }

    private void eliminarMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) {
            AlertaHelper.mostrarAlerta("Acción denegada", "La mesa está ocupada.", Alert.AlertType.WARNING);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Mesa");
        alert.setHeaderText("¿Quitar Mesa " + mesa.getNumero() + "?");
        AlertaHelper.estilizarDialogo(alert); // Estilo

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            mesa.setActiva(false);
            mesaRepo.save(mesa);
            cargarSectores();
        }
    }

    @FXML public void nuevaMesaEnSectorActual() {
        if (sectorActual == null) return;
        int numero = (int) (mesaRepo.count() + 1); // Lógica simple para evitar ID duplicado (mejorable)
        Mesa nueva = new Mesa(numero, 4);
        nueva.setSector(sectorActual);
        nueva.setActiva(true);
        mesaRepo.save(nueva);
        cargarSectores();
    }

    @FXML public void nuevoSector() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Sector");
        dialog.setHeaderText("Nombre:");
        AlertaHelper.estilizarDialogo(dialog); // Estilo

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) { sectorRepo.save(new Sector(nombre)); cargarSectores(); }
        });
    }
}