package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.UsuarioRepository;
import gastronomia.sistemaGastronomico.model.Rol;
import gastronomia.sistemaGastronomico.model.Usuario;
import gastronomia.sistemaGastronomico.utils.GoogleMaps;
import gastronomia.sistemaGastronomico.JavaFxApplication;
import javafx.application.Platform; // Import
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode; // Import
import javafx.scene.input.KeyCodeCombination; // Import
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class UsuariosController implements Initializable {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApplicationContext context;
    @Autowired private JavaFxApplication app;

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre, colUser, colRol, colDireccion;

    @FXML private TextField txtNombre, txtUser, txtPass, txtDireccion;
    @FXML private ComboBox<Rol> cmbRol;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTabla();
        cargarUsuarios();

        if(cmbRol != null) cmbRol.setItems(FXCollections.observableArrayList(Rol.values()));

        // --- AGREGADO: F9 PARA VOLVER ---
        Platform.runLater(() -> {
            if (tablaUsuarios.getScene() != null) {
                tablaUsuarios.getScene().getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F9),
                        this::volverAlMenu
                );
            }
        });
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
    }

    private void cargarUsuarios() {
        tablaUsuarios.setItems(FXCollections.observableArrayList(usuarioRepository.findAll()));
    }

    @FXML
    void nuevoUsuario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/RegistroUsuario.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nuevo Usuario");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarUsuarios();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void eliminarUsuario() {
        Usuario u = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (u != null) {
            usuarioRepository.delete(u);
            cargarUsuarios();
        }
    }

    @FXML
    void verMapa() {
        Usuario u = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (u != null) GoogleMaps.abrirMapa(app.getHostServices(), u.getDireccion());
    }

    @FXML
    void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) tablaUsuarios.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - Menú Principal");
            stage.setMaximized(true);
            stage.show(); // Forzamos show para asegurar refresco
        } catch (Exception e) { e.printStackTrace(); }
    }
}