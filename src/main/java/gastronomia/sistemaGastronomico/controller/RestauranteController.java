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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

        // Actualización automática cada 60s
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(60), e -> {
                    if (sectorActual != null) cargarMesasDelSector(sectorActual);
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // CONFIGURACIÓN DE TECLAS (F9 y F2)
        Platform.runLater(() -> {
            if (contenedorMesas.getScene() != null) {
                contenedorMesas.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F9),
                        this::volverAlMenu
                );
                contenedorMesas.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F2),
                        this::accionF2AbrirMesa
                );
            }
        });
    }

    // --- LÓGICA PRINCIPAL DE DIBUJADO ---

    private void cargarMesasDelSector(Sector sector) {
        contenedorMesas.getChildren().clear();
        if (sector == null) return;

        List<Mesa> mesas = mesaRepo.findBySectorAndActivaTrue(sector);

        for (Mesa mesa : mesas) {
            Button btn = new Button();
            btn.setPrefSize(160, 140);
            btn.getStyleClass().add("mesa-btn");

            // Buscamos pedido ABIERTO
            Optional<Pedido> pedidoAbierto = pedidoRepo.findFirstByMesaAndEstado(mesa, "ABIERTO");

            if (pedidoAbierto.isPresent()) {
                // --- MESA OCUPADA ---
                Pedido p = pedidoAbierto.get();
                btn.getStyleClass().add("mesa-ocupada");

                // 1. Gráfico con Doble Reloj (Espera/Comiendo)
                btn.setGraphic(crearGraficoMesaOcupada(mesa, p));

                // 2. Menú Click Derecho Ocupada
                ContextMenu contextMenu = new ContextMenu();

                // Opción A: Marcar Comida Servida (si aún no se sirvió)
                if (p.getHoraServido() == null) {
                    MenuItem itemComida = new MenuItem("✅ Marcar 'Comida Servida'");
                    itemComida.setStyle("-fx-font-weight: bold;");
                    itemComida.setOnAction(e -> {
                        p.setHoraServido(LocalTime.now());
                        pedidoRepo.save(p);
                        cargarMesasDelSector(sectorActual); // Refrescar para ver el cambio
                    });
                    contextMenu.getItems().add(itemComida);
                }

                // Opción B: Mover a otra Mesa (Mudanza) - NUEVO
                MenuItem itemMover = new MenuItem("🔄 Mover a otra Mesa (Mudanza)");
                itemMover.setOnAction(e -> accionMoverClientes(mesa, p));

                // Opción C: Ver Pedido
                MenuItem itemVer = new MenuItem("📝 Ver / Editar Pedido");
                itemVer.setOnAction(e -> abrirPantalla(mesa, "/Views/pedido.fxml", true));

                // Opción D: Cerrar Mesa
                MenuItem itemCerrar = new MenuItem("💰 Cerrar Mesa (Cobrar)");
                itemCerrar.setOnAction(e -> abrirPantalla(mesa, "/Views/pedido.fxml", true));

                // Opción E: Renombrar Mesa (Corrección administrativa)
                MenuItem itemRenombrar = new MenuItem("✏️ Cambiar N° de Mesa");
                itemRenombrar.setOnAction(e -> accionEditarNumeroMesa(mesa));

                // Agregamos todo al menú con separadores para ordenar
                contextMenu.getItems().addAll(
                        new SeparatorMenuItem(),
                        itemMover,
                        new SeparatorMenuItem(),
                        itemVer,
                        itemCerrar,
                        new SeparatorMenuItem(),
                        itemRenombrar
                );
                btn.setContextMenu(contextMenu);

            } else {
                // --- MESA LIBRE ---
                btn.getStyleClass().add("mesa-libre");
                btn.setGraphic(crearGraficoMesaLibre(mesa));

                // Menú Click Derecho Libre (Para acomodar el salón)
                ContextMenu contextMenuLibre = new ContextMenu();
                MenuItem itemRenombrar = new MenuItem("✏️ Cambiar N° de Mesa");
                itemRenombrar.setOnAction(e -> accionEditarNumeroMesa(mesa));

                contextMenuLibre.getItems().add(itemRenombrar);
                btn.setContextMenu(contextMenuLibre);
            }

            // Click Izquierdo: Acción por defecto (Abrir/Ver)
            btn.setOnAction(e -> gestionarClicMesa(mesa));

            contenedorMesas.getChildren().add(btn);
        }
    }

    // --- MÉTODOS VISUALES MEJORADOS ---

    private VBox crearGraficoMesaOcupada(Mesa mesa, Pedido pedido) {
        VBox v = new VBox(5);
        v.setAlignment(Pos.CENTER);

        // N° Mesa
        Label lblNumero = new Label("MESA " + mesa.getNumero());
        lblNumero.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Mozo
        String nombreMozo = (pedido.getMozo() != null) ? pedido.getMozo().getNombre() : "S/Mozo";
        Label lblMozo = new Label("Mozo: " + nombreMozo);
        lblMozo.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");

        // --- LÓGICA DE TIEMPOS ---

        // 1. Tiempo Total (desde que se abrió la mesa)
        long minutosTotal = 0;
        if (pedido.getHora() != null) {
            minutosTotal = ChronoUnit.MINUTES.between(pedido.getHora(), LocalTime.now());
        }
        Label lblRelojTotal = new Label("Total: " + minutosTotal + " min");
        lblRelojTotal.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px;");

        // 2. Tiempo Comida (desde que se sirvió)
        if (pedido.getHoraServido() != null) {
            long minComida = ChronoUnit.MINUTES.between(pedido.getHoraServido(), LocalTime.now());
            Label lblComida = new Label("🍽️ Comiendo: " + minComida + " min");

            // Semáforo de colores para ofrecer postre/café
            if (minComida >= 45) {
                lblComida.setStyle("-fx-text-fill: #ff7675; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: rgba(0,0,0,0.3);");
            } else if (minComida >= 20) {
                lblComida.setStyle("-fx-text-fill: #ffeaa7; -fx-font-weight: bold; -fx-font-size: 12px;");
            } else {
                lblComida.setStyle("-fx-text-fill: #55efc4; -fx-font-weight: bold; -fx-font-size: 12px;");
            }

            // Agregamos todo al VBox
            v.getChildren().addAll(lblNumero, lblMozo, lblComida, lblRelojTotal);

        } else {
            // Si aún no se marca como servido
            Label lblEspera = new Label("⏳ Esperando Comida");
            lblEspera.setStyle("-fx-text-fill: #81ecec; -fx-font-size: 11px;");
            v.getChildren().addAll(lblNumero, lblMozo, lblEspera, lblRelojTotal);
        }

        return v;
    }

    private VBox crearGraficoMesaLibre(Mesa mesa) {
        VBox v = new VBox(5);
        v.setAlignment(Pos.CENTER);
        Label lblNumero = new Label("MESA " + mesa.getNumero());
        lblNumero.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label lblEstado = new Label("Disponible");
        lblEstado.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        v.getChildren().addAll(lblNumero, lblEstado);
        return v;
    }

    // --- ACCIÓN: EDITAR NÚMERO (Renombrar mesa) ---
    private void accionEditarNumeroMesa(Mesa mesa) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(mesa.getNumero()));
        estilizar(dialog);
        dialog.setTitle("Configurar Mesa");
        dialog.setHeaderText("Editar numeración de la Mesa " + mesa.getNumero());
        dialog.setContentText("Ingrese el NUEVO número real:");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            try {
                int nuevoNumero = Integer.parseInt(resultado.get());

                if (nuevoNumero == mesa.getNumero()) return;

                Optional<Mesa> conflicto = mesaRepo.findByNumero(nuevoNumero);
                if (conflicto.isPresent()) {
                    advertencia("Error", "Ya existe una mesa con el número " + nuevoNumero + ".");
                    return;
                }

                mesa.setNumero(nuevoNumero);
                mesaRepo.save(mesa);
                cargarMesasDelSector(sectorActual);
                informacion("Guardado", "La mesa ahora es la N° " + nuevoNumero);

            } catch (NumberFormatException e) {
                error("Error", "Debe ingresar un número válido.");
            } catch (Exception e) {
                e.printStackTrace();
                error("Error", "No se pudo cambiar el número.");
            }
        }
    }

    // --- ACCIÓN: MOVER CLIENTES (Mudanza) - NUEVO ---
    private void accionMoverClientes(Mesa mesaActual, Pedido pedidoActual) {
        TextInputDialog dialog = new TextInputDialog();
        estilizar(dialog);
        dialog.setTitle("Mover Mesa (Mudanza)");
        dialog.setHeaderText("Mover clientes de la Mesa " + mesaActual.getNumero());
        dialog.setContentText("Ingrese el N° de la mesa de DESTINO (debe estar libre):");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            try {
                int nroDestino = Integer.parseInt(resultado.get());

                // Validar que no sea la misma
                if (nroDestino == mesaActual.getNumero()) return;

                // Buscar mesa destino
                Optional<Mesa> mesaDestinoOpt = mesaRepo.findByNumero(nroDestino);

                if (mesaDestinoOpt.isPresent()) {
                    Mesa mesaDestino = mesaDestinoOpt.get();

                    // Validar que destino esté LIBRE
                    Optional<Pedido> pedidoEnDestino = pedidoRepo.findFirstByMesaAndEstado(mesaDestino, "ABIERTO");

                    if (pedidoEnDestino.isPresent()) {
                        advertencia("Mesa Ocupada", "La mesa " + nroDestino + " ya está ocupada.");
                    } else {
                        // Realizar la Mudanza
                        pedidoActual.setMesa(mesaDestino);
                        pedidoRepo.save(pedidoActual);

                        cargarMesasDelSector(sectorActual); // Refrescar
                        informacion("Éxito", "Clientes movidos correctamente a la Mesa " + nroDestino);
                    }
                } else {
                    error("Error", "La mesa N° " + nroDestino + " no existe.");
                }

            } catch (NumberFormatException e) {
                error("Error", "Debe ingresar un número válido.");
            } catch (Exception e) {
                e.printStackTrace();
                error("Error", "No se pudo realizar el movimiento.");
            }
        }
    }

    // --- NAVEGACIÓN Y UTILIDADES (SIN CAMBIOS) ---

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
            btn.setOnAction(e -> { this.sectorActual = sector; cargarMesasDelSector(sectorActual); });
            contenedorSectores.getChildren().add(btn);
        }
        cargarMesasDelSector(sectorActual);
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

    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) contenedorMesas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch(Exception e) {
            e.printStackTrace();
            error("Error", "No se pudo cargar el Menú Principal.");
        }
    }

    private void accionF2AbrirMesa() {
        TextInputDialog dialog = new TextInputDialog();
        estilizar(dialog);
        dialog.setTitle("Llamar Mesa");
        dialog.setHeaderText("Ingrese N° de Mesa:");
        dialog.showAndWait().ifPresent(numeroStr -> {
            try {
                int numero = Integer.parseInt(numeroStr);
                Optional<Mesa> mesaOpt = mesaRepo.findByNumero(numero);
                if (mesaOpt.isPresent()) gestionarClicMesa(mesaOpt.get());
                else advertencia("Atención", "Mesa no encontrada.");
            } catch (Exception e) {}
        });
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
            stage.setOnHidden(e -> cargarMesasDelSector(sectorActual));
            stage.show();
        } catch(Exception e) {
            e.printStackTrace();
            error("Error", "No se pudo abrir la pantalla: " + rutaFxml);
        }
    }

    // Stubs
    @FXML public void nuevaMesaEnSectorActual() {}
    @FXML public void nuevoSector() {}
    @FXML public void buscarMesaRapida() {}
    @FXML public void abrirReporteMozo() {}
}