package gastronomia.sistemaGastronomico.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import java.net.URL;

public class AlertaHelper {

    /**
     * Muestra una alerta rápida ya estilizada.
     * @param titulo   El encabezado de la ventana.
     * @param mensaje  El contenido del mensaje.
     * @param tipo     El icono (ERROR, WARNING, INFORMATION, etc).
     */
    public static void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Sistema Gastronómico");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);

        // Aplicamos el estilo visual
        estilizarDialogo(alert);

        alert.showAndWait();
    }

    /**
     * Inyecta el CSS a cualquier diálogo (Alert, TextInputDialog, ChoiceDialog).
     */
    public static void estilizarDialogo(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();

        // Buscamos el archivo CSS en la raíz de los recursos
        URL urlCss = AlertaHelper.class.getResource("/estilos.css");

        if (urlCss == null) {
            // Diagnóstico en consola si falla
            System.err.println("❌ [AlertaHelper] ERROR: No se encontró '/estilos.css' en src/main/resources/");
        } else {
            // Éxito: aplicamos la hoja de estilos
            dialogPane.getStylesheets().add(urlCss.toExternalForm());
            dialogPane.getStyleClass().add("mi-dialogo"); // Clase base por si se necesita
        }
    }
}