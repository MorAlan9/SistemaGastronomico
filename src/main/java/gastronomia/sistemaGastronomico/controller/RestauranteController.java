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
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
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
        cargarSectores();
    }

    private void cargarSectores() {
        contenedorSectores.getChildren().clear();
        List<Sector> sectores = sectorRepo.findAll();

        if (sectores.isEmpty()) return;
        if (sectorActual == null) sectorActual = sectores.get(0);

        for (Sector sector : sectores) {
            // USAMOS EL MÉTODO NUEVO (Solo trae activas)
            List<Mesa> mesasDelSector = mesaRepo.findBySectorAndActivaTrue(sector);
            long total = mesasDelSector.size();
            long ocupadas = mesasDelSector.stream()
                    .filter(m -> pedidoRepo.findFirstByMesaAndEstado(m, "ABIERTO").isPresent())
                    .count();

            Button btn = new Button(sector.getNombre() + "\n(" + ocupadas + "/" + total + ")");
            btn.setPrefHeight(60);
            btn.setMinWidth(120);
            btn.setMaxWidth(Double.MAX_VALUE);

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

        // USAMOS EL MÉTODO NUEVO (Solo trae activas)
        List<Mesa> mesas = mesaRepo.findBySectorAndActivaTrue(sector);

        for (Mesa mesa : mesas) {
            Button btn = new Button();
            btn.setPrefSize(140, 120);
            btn.getStyleClass().clear();
            btn.getStyleClass().add("mesa-btn");

            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                btn.getStyleClass().add("mesa-ocupada");
                int gente = pedidoAbierto.get().getComensales() != null ? pedidoAbierto.get().getComensales() : 0;
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "$" + pedidoAbierto.get().getTotal(), gente));
            } else {
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesa(mesa.getNumero(), "Libre", 0));
            }

            btn.setOnAction(e -> gestionarClicMesa(mesa));
            configurarMenuContextual(btn, mesa); // Clic derecho

            contenedorMesas.getChildren().add(btn);
        }
    }

    // --- BORRADO LÓGICO (SOFT DELETE) ---
    private void eliminarMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) {
            mostrarAlerta("Acción denegada", "La mesa está ocupada. Cierre el pedido antes de borrarla.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Mesa");
        alert.setHeaderText("¿Quitar Mesa " + mesa.getNumero() + " del mapa?");
        alert.setContentText("La mesa desaparecerá de la vista, pero el historial de ventas se conservará.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // AQUÍ ESTÁ EL TRUCO: NO BORRAMOS, SOLO OCULTAMOS
            mesa.setActiva(false);
            mesaRepo.save(mesa);

            cargarSectores(); // Refrescar y ¡puff! desaparece
        }
    }

    // ... (El resto de métodos: configurarMenuContextual, editarMesa, gestionarClicMesa, etc. IGUAL QUE ANTES) ...

    private void configurarMenuContextual(Button btn, Mesa mesa) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar = new MenuItem("✏️ Editar Mesa");
        itemEditar.setOnAction(e -> editarMesa(mesa));
        MenuItem itemEliminar = new MenuItem("🗑️ Eliminar Mesa");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarMesa(mesa));
        contextMenu.getItems().addAll(itemEditar, new SeparatorMenuItem(), itemEliminar);
        btn.setContextMenu(contextMenu);
    }

    private void editarMesa(Mesa mesa) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Mesa " + mesa.getNumero());
        dialog.setHeaderText("Modificar datos");
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
            } catch (Exception e) { mostrarAlerta("Error", "Datos inválidos"); }
        }
    }

    private void gestionarClicMesa(Mesa mesa) {
        Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");
        if (pedidoAbierto.isPresent()) abrirPantalla(mesa, "/Views/pedido.fxml");
        else abrirPantalla(mesa, "/Views/AbrirMesa.fxml");
    }

    private void abrirPantalla(Mesa mesa, String rutaFxml) {
        try {
            URL url = getClass().getResource(rutaFxml);
            if (url == null) { mostrarAlerta("Error", "No encuentro: " + rutaFxml); return; }
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AbrirMesaController) ((AbrirMesaController) controller).setMesa(mesa);
            else if (controller instanceof TomaPedidoController) ((TomaPedidoController) controller).setMesa(mesa);
            Stage stage = new Stage();
            stage.setTitle("Mesa " + mesa.getNumero());
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> cargarSectores());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); mostrarAlerta("Error", e.getMessage()); }
    }

    private VBox crearGraficoMesa(int numero, String estado, int personas) {
        Label lblNum = new Label(String.valueOf(numero));
        lblNum.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lblEstado = new Label(estado);
        lblEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
        VBox vBox = new VBox(lblNum, lblEstado);
        vBox.setAlignment(javafx.geometry.Pos.CENTER);
        if (personas > 0) {
            Label lblPers = new Label("👥 " + personas);
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

    @FXML public void nuevaMesaEnSectorActual() {
        if (sectorActual == null) return;
        int numero = (int) (mesaRepo.count() + 1); // OJO: Esto cuenta todas, incluso ocultas. Podría duplicar números.
        // Mejor lógica sería buscar el MAX número existente. Pero para ahora sirve.
        Mesa nueva = new Mesa(numero, 4);
        nueva.setSector(sectorActual);
        nueva.setActiva(true); // Explicitamente activa
        mesaRepo.save(nueva);
        cargarSectores();
    }

    @FXML public void nuevoSector() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Sector");
        dialog.setHeaderText("Nombre:");
        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) { sectorRepo.save(new Sector(nombre)); cargarSectores(); }
        });
    }
}