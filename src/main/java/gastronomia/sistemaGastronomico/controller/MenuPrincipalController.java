package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.model.Rol;
import gastronomia.sistemaGastronomico.model.Usuario;
import gastronomia.sistemaGastronomico.utils.SesionGlobal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class MenuPrincipalController implements Initializable {

    @Autowired
    private ApplicationContext context;

    @FXML private Label lblUsuarioLogueado;
    @FXML private Label lblRol;

    @FXML private Button btnMesas;
    @FXML private Button btnVentas;
    @FXML private Button btnUsuarios;
    @FXML private Button btnProductos;
    @FXML private Button btnReportes;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatosUsuario();
        aplicarPermisos();
    }

    private void cargarDatosUsuario() {
        Usuario u = SesionGlobal.usuarioActual;
        if (u != null) {
            lblUsuarioLogueado.setText("Usuario: " + u.getUsername());
            lblRol.setText("Rol: " + u.getRol().toString());
        }
    }

    private void aplicarPermisos() {
        Usuario u = SesionGlobal.usuarioActual;
        if (u == null) return;

        btnMesas.setDisable(false);
        btnVentas.setDisable(false);
        btnUsuarios.setVisible(true);
        btnProductos.setDisable(false);

        if (u.getRol() == Rol.MOZO) {
            btnVentas.setDisable(true);
            btnUsuarios.setVisible(false);
            btnProductos.setDisable(true);
        }
        else if (u.getRol() == Rol.CAJERO) {
            btnUsuarios.setVisible(false);
        }
    }

    // --- NAVEGACIÓN ---

    @FXML
    void irAVentas() { navegar("/Views/Ventas.fxml", "Gestión de Caja"); }

    @FXML
    void irAMesas() { navegar("/Views/Restaurante.fxml", "Salón"); }

    @FXML
    void irAUsuarios() { navegar("/Views/Usuarios.fxml", "Gestión de Personal"); }

    @FXML
    void cerrarSesion() {
        SesionGlobal.usuarioActual = null;
        navegar("/Views/Login.fxml", "Login");
    }


    @FXML
    void irAProductos() {
        navegar("/Views/AdminProductos.fxml", "Gestión de Productos");
    }

    private void navegar(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) btnMesas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - " + titulo);
            stage.centerOnScreen();
            stage.setMaximized(true); // Aseguramos que se abra maximizado
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al cargar vista: " + fxml).show();
        }
    }
}