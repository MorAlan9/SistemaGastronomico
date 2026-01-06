package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.utils.AlertaHelper;
import gastronomia.sistemaGastronomico.utils.Toast;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public abstract class BaseController {

    // Método corto para ALERTAS DE ERROR
    protected void error(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.ERROR);
    }

    // Método corto para ALERTAS DE ADVERTENCIA
    protected void advertencia(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.WARNING);
    }

    // Método corto para INFORMACIÓN
    protected void info(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.INFORMATION);
    }

    // Método corto para TOASTS (Requiere pasarle cualquier nodo de la pantalla actual)
    protected void toast(String mensaje, Node nodoCualquiera) {
        if (nodoCualquiera != null && nodoCualquiera.getScene() != null) {
            Stage stage = (Stage) nodoCualquiera.getScene().getWindow();
            Toast.mostrar(mensaje, stage);
        }
    }
}