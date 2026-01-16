package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.UsuarioRepository;
import gastronomia.sistemaGastronomico.model.Rol;
import gastronomia.sistemaGastronomico.model.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class RegistroUsuarioController implements Initializable {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApplicationContext context; // Necesario para navegar al Login

    @FXML private TextField txtNombre, txtUsuario, txtDireccion, txtTelefono;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Rol> cmbRol;
    @FXML private Label lblMensaje;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbRol.setItems(FXCollections.observableArrayList(Rol.values()));
        cmbRol.getSelectionModel().select(Rol.MOZO);
    }

    @FXML
    void guardarUsuario() {
        if (validarCampos()) {
            try {
                if (usuarioRepository.findByUsername(txtUsuario.getText()).isPresent()) {
                    mostrarMensaje("El usuario ya existe.", true);
                    return;
                }

                Usuario nuevo = new Usuario(txtUsuario.getText(), txtPassword.getText(), txtNombre.getText(), cmbRol.getValue(), txtDireccion.getText(), txtTelefono.getText());
                usuarioRepository.save(nuevo);

                // ÉXITO: Redirigir al Login
                mostrarMensaje("¡Guardado! Redirigiendo...", false);
                irAlLogin(); // <--- NAVEGACIÓN AUTOMÁTICA

            } catch (Exception e) {
                mostrarMensaje("Error al guardar.", true);
            }
        }
    }

    @FXML
    void saltarRegistro() {
        // Acción del botón "Saltar este paso"
        irAlLogin();
    }

    private void irAlLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            // Obtenemos la ventana actual y cambiamos la escena
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - Login");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Validaciones y Auxiliares ---
    private boolean validarCampos() {
        if (txtNombre.getText().isEmpty() || txtUsuario.getText().isEmpty() || txtPassword.getText().isEmpty() || txtDireccion.getText().isEmpty()) {
            mostrarMensaje("Complete los campos obligatorios (*)", true);
            return false;
        }
        return true;
    }

    private void mostrarMensaje(String msg, boolean error) {
        lblMensaje.setText(msg);
        lblMensaje.setStyle(error ? "-fx-text-fill: #e74c3c; -fx-background-color: #fadbd8;" : "-fx-text-fill: #27ae60; -fx-background-color: #d5f5e3;");
    }

    @FXML void cancelar() { saltarRegistro(); } // Cancelar ahora actúa como saltar
}