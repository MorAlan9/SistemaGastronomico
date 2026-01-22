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
    @Autowired private ApplicationContext context;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        // SEGURIDAD: Si es la primera vez (BD vacía), crea un Admin por defecto.
        if (usuarioRepository.count() == 0) {
            // CORRECCIÓN: Agregamos el PIN "1234" al final del constructor
            Usuario adminDefault = new Usuario(
                    "admin",          // Usuario
                    "admin",          // Password
                    "Super Admin",    // Nombre
                    Rol.ADMIN,        // Rol
                    "Sistema",        // Dirección
                    "000",            // Teléfono
                    "1234"            // PIN (Nuevo campo obligatorio)
            );

            usuarioRepository.save(adminDefault);

            lblError.setText("Modo inicial: admin / admin (PIN: 1234)");
            lblError.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    void ingresar() {
        String u = txtUsuario.getText();
        String p = txtPassword.getText();

        if (u.isEmpty() || p.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña");
            return;
        }

        // Buscamos el usuario en la BD
        usuarioRepository.findByUsername(u).ifPresentOrElse(usuarioEncontrado -> {

            // Verificamos contraseña
            if (usuarioEncontrado.getPassword().equals(p)) {

                // 1. Verificar si está activo (por si lo borraste lógicamente)
                // (Si no tienes el campo activo en Usuario.java, borra este if)
                // if (!usuarioEncontrado.isActivo()) {
                //    mostrarError("Usuario inhabilitado.");
                //    return;
                // }

                // 2. GUARDAMOS LA SESIÓN GLOBAL
                SesionGlobal.usuarioActual = usuarioEncontrado;
                SesionGlobal.mozoActual = null; // Limpiamos mozo por seguridad

                // 3. CAMBIAMOS AL MENÚ PRINCIPAL
                abrirMenuPrincipal();

            } else {
                mostrarError("Contraseña incorrecta");
            }
        }, () -> mostrarError("Usuario no encontrado"));
    }

    @FXML
    void irARegistro() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/RegistroUsuario.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirMenuPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));

            // Título dinámico
            stage.setTitle("Sistema Gastronómico - Usuario: " + SesionGlobal.usuarioActual.getUsername());
            stage.setMaximized(true);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error crítico al cargar el menú.");
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }
}