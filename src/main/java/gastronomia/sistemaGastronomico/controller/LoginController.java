package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.UsuarioRepository;
import gastronomia.sistemaGastronomico.model.Rol;
import gastronomia.sistemaGastronomico.model.Usuario;
import gastronomia.sistemaGastronomico.utils.SesionGlobal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApplicationContext context; // Para poder cargar la siguiente pantalla con Spring

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        // SEGURIDAD: Si es la primera vez que usas la app (BD vacía), crea un Admin.
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Usuario("admin", "admin", "Super Admin", Rol.ADMIN, "Sistema", "000"));
            lblError.setText("Modo inicial: admin / admin");
            lblError.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    void ingresar() {
        String u = txtUsuario.getText();
        String p = txtPassword.getText();

        // Buscamos el usuario en la BD
        usuarioRepository.findByUsername(u).ifPresentOrElse(usuarioEncontrado -> {

            // Verificamos contraseña
            if (usuarioEncontrado.getPassword().equals(p)) {

                // 1. GUARDAMOS LA SESIÓN (¡Esto es lo más importante!)
                SesionGlobal.usuarioActual = usuarioEncontrado;

                // 2. CAMBIAMOS DE PANTALLA
                abrirMenuPrincipal();

            } else {
                mostrarError("Contraseña incorrecta");
            }
        }, () -> mostrarError("Usuario no encontrado"));
    }

    private void abrirMenuPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean); // Inyección de dependencias
            Parent root = loader.load();

            // Obtenemos la ventana actual y le cambiamos la escena
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - Usuario: " + SesionGlobal.usuarioActual.getUsername());
            stage.setMaximized(true); // El menú se ve mejor en pantalla completa
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar el sistema");
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setStyle("-fx-text-fill: red;");
    }
}