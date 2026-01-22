package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MozoRepository; // <--- Importante
import gastronomia.sistemaGastronomico.dao.UsuarioRepository;
import gastronomia.sistemaGastronomico.model.Mozo;
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
    @Autowired private MozoRepository mozoRepository; // <--- Agregamos esto
    @Autowired private ApplicationContext context;

    @FXML private TextField txtNombre, txtUsuario, txtDireccion, txtTelefono, txtPin; // <--- txtPin DEBE ESTAR EN EL FXML
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Rol> cmbRol;
    @FXML private Label lblMensaje;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbRol.setItems(FXCollections.observableArrayList(Rol.values()));
        cmbRol.getSelectionModel().select(Rol.MOZO);

        // Listener para ocultar campos si es MOZO (Opcional, pero queda bien)
        cmbRol.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esMozo = (newVal == Rol.MOZO);
            txtPassword.setDisable(esMozo); // Mozo no necesita password compleja, solo PIN
            if(esMozo) txtPassword.clear();
        });
    }

    @FXML
    void guardarUsuario() {
        if (validarCampos()) {
            try {
                String pin = txtPin.getText();

                if (cmbRol.getValue() == Rol.MOZO) {
                    // --- GUARDAR EN TABLA DE MOZOS ---
                    Mozo nuevoMozo = new Mozo(txtNombre.getText(), txtUsuario.getText(), pin);
                    mozoRepository.save(nuevoMozo);
                    mostrarMensaje("¡Mozo registrado!", false);

                } else {
                    // --- GUARDAR EN TABLA DE USUARIOS (Admin/Cajero) ---
                    if (usuarioRepository.findByUsername(txtUsuario.getText()).isPresent()) {
                        mostrarMensaje("El usuario ya existe.", true);
                        return;
                    }

                    Usuario nuevo = new Usuario(
                            txtUsuario.getText(),
                            txtPassword.getText(),
                            txtNombre.getText(),
                            cmbRol.getValue(),
                            txtDireccion.getText(),
                            txtTelefono.getText(),
                            pin // <--- Pasamos el PIN
                    );
                    usuarioRepository.save(nuevo);
                    mostrarMensaje("¡Usuario guardado!", false);
                }

                // ÉXITO: Redirigir
                irAlLogin();

            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al guardar: " + e.getMessage());
            }
        }
    }

    // ... (El resto de métodos irAlLogin y validarCampos igual que antes) ...

    @FXML void saltarRegistro() { irAlLogin(); }
    @FXML void cancelar() { saltarRegistro(); }

    private void irAlLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean validarCampos() {
        if (txtNombre.getText().isEmpty() || txtUsuario.getText().isEmpty() || txtPin.getText().isEmpty()) {
            mostrarError("Complete nombre, usuario y PIN.");
            return false;
        }
        return true;
    }

    private void mostrarMensaje(String msg, boolean error) {
        lblMensaje.setText(msg);
        lblMensaje.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private void mostrarError(String msg) { mostrarMensaje(msg, true); }
}