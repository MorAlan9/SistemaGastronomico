package gastronomia.sistemaGastronomico.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Toast {

    public static void mostrar(String mensaje, Stage ownerStage) {
        // Si no hay ventana padre, no podemos calcular la posición
        if (ownerStage == null) return;

        Stage toastStage = new Stage();
        toastStage.initOwner(ownerStage);
        toastStage.initStyle(StageStyle.TRANSPARENT); // Sin bordes ni barra de título
        toastStage.setAlwaysOnTop(true);              // Siempre visible encima

        // Diseño de la etiqueta (Label)
        Label label = new Label(mensaje);
        label.setStyle(
                "-fx-background-color: #2ecc71; " +   // Fondo Verde Éxito
                        "-fx-text-fill: white; " +            // Texto Blanco
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 15px 25px; " +          // Relleno interno
                        "-fx-background-radius: 30px; " +     // Bordes muy redondos
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);" // Sombra
        );

        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); // Fondo de la escena transparente
        toastStage.setScene(scene);

        // Mostramos primero para que JavaFX calcule el ancho real del texto
        toastStage.show();

        // Cálculo de posición: Centrado horizontalmente, y a 100px del borde inferior
        double x = ownerStage.getX() + (ownerStage.getWidth() / 2) - (toastStage.getWidth() / 2);
        double y = ownerStage.getY() + ownerStage.getHeight() - 100;

        toastStage.setX(x);
        toastStage.setY(y);

        // Temporizador: Cierra la ventana automáticamente a los 2 segundos (2000 ms)
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
            toastStage.close();
        }));
        timeline.play();
    }
}