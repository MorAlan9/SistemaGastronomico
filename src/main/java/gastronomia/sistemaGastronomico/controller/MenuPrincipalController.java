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
    private ApplicationContext context; // Para navegar a otras pantallas

    // --- ELEMENTOS VISUALES (Asegúrate que coincidan con los fx:id en SceneBuilder) ---
    @FXML private Label lblUsuarioLogueado;
    @FXML private Label lblRol;

    // BOTONES DEL MENÚ
    @FXML private Button btnMesas;     // Para ir al salón
    @FXML private Button btnVentas;    // Para ir a Ventas/Caja
    @FXML private Button btnUsuarios;  // Para crear mozos (Solo Admin)
    @FXML private Button btnProductos; // Para cargar comida (Admin/Cajero)
    @FXML private Button btnReportes;  // (Opcional)

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatosUsuario();
        aplicarPermisos(); // <--- AQUÍ SE EJECUTA LA SEGURIDAD
    }

    private void cargarDatosUsuario() {
        Usuario u = SesionGlobal.usuarioActual;
        if (u != null) {
            lblUsuarioLogueado.setText("Usuario: " + u.getUsername());
            lblRol.setText("Rol: " + u.getRol().toString());
        }
    }

    // --- EL SEMÁFORO DE PERMISOS ---
    private void aplicarPermisos() {
        Usuario u = SesionGlobal.usuarioActual;
        if (u == null) return; // Si no hay usuario, no hacemos nada (o cerramos)

        // Reiniciar estado (Habilitar todo por defecto)
        btnMesas.setDisable(false);
        btnVentas.setDisable(false);
        btnUsuarios.setVisible(true); // El admin lo ve
        btnProductos.setDisable(false);

        // APLICAR RESTRICCIONES SEGÚN ROL
        if (u.getRol() == Rol.MOZO) {
            // EL MOZO: Solo trabaja. No toca dinero ni configuración.
            btnVentas.setDisable(true);      // No puede entrar a Caja/Ventas
            btnUsuarios.setVisible(false);   // No ve el botón de crear usuarios
            btnProductos.setDisable(true);   // No puede editar precios
        }
        else if (u.getRol() == Rol.CAJERO) {
            // EL CAJERO: Cobra y atiende. No crea usuarios.
            btnUsuarios.setVisible(false);   // No puede crear usuarios Admin
            // Puede entrar a ventas y mesas sin problemas
        }
        else if (u.getRol() == Rol.ADMIN) {
            // EL ADMIN: Dios. Acceso total.
            // No se deshabilita nada.
        }
    }

    // --- NAVEGACIÓN ---

    @FXML
    void irAVentas() { navegar("/Views/Ventas.fxml", "Gestión de Caja"); }

    @FXML
    void irAMesas() { navegar("/Views/Restaurante.fxml", "Salón"); } // O tu vista de mesas

    @FXML
    void irAUsuarios() { navegar("/Views/Usuarios.fxml", "Gestión de Personal"); }

    @FXML
    void cerrarSesion() {
        SesionGlobal.usuarioActual = null; // Borramos al usuario
        navegar("/Views/Login.fxml", "Login");
    }
    @FXML
    void irAPro() {
        navegar("/Views/AdminProductos.fxml", "Gestión de Productos");
    }

    // Método genérico para cambiar de pantalla
    private void navegar(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) btnMesas.getScene().getWindow(); // Obtenemos la ventana actual
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - " + titulo);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al cargar vista: " + fxml).show();
        }
    }
}