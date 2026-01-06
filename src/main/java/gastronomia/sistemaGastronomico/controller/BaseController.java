package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.utils.AlertaHelper;
import gastronomia.sistemaGastronomico.utils.Toast;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;

/**
 * CONTROLADOR BASE (PADRE)
 * Todos los controladores del sistema deben heredar de esta clase (extends BaseController).
 * Proporciona métodos cortos para feedback visual y manejo de errores.
 */
public abstract class BaseController {

    // --- MÉTODOS PARA ALERTAS (Heredados) ---

    protected void error(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.ERROR);
    }

    protected void advertencia(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.WARNING);
    }

    protected void info(String titulo, String mensaje) {
        AlertaHelper.mostrarAlerta(titulo, mensaje, Alert.AlertType.INFORMATION);
    }

    // --- MÉTODO PARA TOAST (Heredado) ---
    // Recibe un mensaje y CUALQUIER nodo visual (botón, tabla, layout) para saber dónde aparecer.
    protected void toast(String mensaje, Node nodoVisual) {
        if (nodoVisual != null && nodoVisual.getScene() != null) {
            Stage stage = (Stage) nodoVisual.getScene().getWindow();
            Toast.mostrar(mensaje, stage);
        }
    }

    // --- MÉTODO PARA ESTILIZAR DIÁLOGOS EXTRA (Como Inputs) ---
    protected void estilizar(Dialog<?> dialog) {
        AlertaHelper.estilizarDialogo(dialog);
    }
}