package gastronomia.sistemaGastronomico.utils;

import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.model.Mozo;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import java.util.Optional;

public class DialogoPinMozo {

    /**
     * Muestra un popup que solicita el PIN del mozo.
     * @param repo El repositorio de mozos para validar el código.
     * @return Un Optional con el Mozo si el PIN es correcto, o vacío si cancela.
     */
    public static Optional<Mozo> solicitar(MozoRepository repo) {
        // Crear el diálogo
        Dialog<Mozo> dialog = new Dialog<>();
        dialog.setTitle("Identificación de Personal");
        dialog.setHeaderText("Ingrese su PIN de Mozo");

        // Agregar botón de cancelar por defecto
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        // Contenedor principal
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 25; -fx-background-color: #f4f4f4;");

        // Campo de PIN (PasswordField para que no se vean los números)
        PasswordField txtPin = new PasswordField();
        txtPin.setPromptText("PIN");
        txtPin.setStyle("-fx-font-size: 20px; -fx-alignment: center; -fx-pref-width: 150px; -fx-background-radius: 5;");

        // Etiqueta para mensajes de error
        Label lblError = new Label("");
        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        root.getChildren().addAll(new Label("Identifíquese para continuar:"), txtPin, lblError);
        dialog.getDialogPane().setContent(root);

        // Dar foco automático al campo de texto al abrir
        Platform.runLater(txtPin::requestFocus);

        // Lógica de validación al presionar ENTER
        txtPin.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String pinIngresado = txtPin.getText();

                // Buscamos en la base de datos de mozos
                Optional<Mozo> mozoOpt = repo.findByPin(pinIngresado);

                if (mozoOpt.isPresent()) {
                    // PIN Correcto: devolvemos el mozo y cerramos
                    dialog.setResult(mozoOpt.get());
                    dialog.close();
                } else {
                    // PIN Incorrecto: limpiar y avisar
                    lblError.setText("PIN INCORRECTO");
                    txtPin.clear();
                    // Efecto visual de error
                    txtPin.setStyle(txtPin.getStyle() + "-fx-border-color: red; -fx-border-width: 2;");
                }
            }
        });

        // Retorna el resultado (el objeto Mozo o null si cerró la ventana)
        return dialog.showAndWait();
    }
}