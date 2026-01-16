package gastronomia.sistemaGastronomico.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

public class NavegacionGlobal {

    /**
     * Activa la tecla F9 en cualquier vista para volver al Menú Principal.
     * @param nodoVisual Cualquier elemento de la pantalla (un botón, una tabla, el contenedor principal).
     * @param context El ApplicationContext de Spring para cargar el FXML correctamente.
     */
    public static void activarF9Volver(Node nodoVisual, ApplicationContext context) {
        // Esperamos a que la escena esté lista
        if (nodoVisual.getScene() != null) {
            agregarFiltro(nodoVisual.getScene(), context);
        } else {
            // Si la escena aún no carga (pasa en el initialize), esperamos el evento
            nodoVisual.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    agregarFiltro(newScene, context);
                }
            });
        }
    }

    private static void agregarFiltro(Scene scene, ApplicationContext context) {
        // Usamos addEventFilter en lugar de setOnKeyPressed para que funcione
        // AUNQUE el foco lo tenga un campo de texto o una tabla.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F9) {
                volverAlMenu(scene, context);
                event.consume(); // Evita que F9 haga otra cosa
            }
        });
    }

    private static void volverAlMenu(Scene currentScene, ApplicationContext context) {
        try {
            FXMLLoader loader = new FXMLLoader(NavegacionGlobal.class.getResource("/Views/MenuPrincipal.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Gastronómico - Menú Principal");
            stage.setMaximized(true);
            System.out.println("🔙 Volviendo al menú por F9...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}