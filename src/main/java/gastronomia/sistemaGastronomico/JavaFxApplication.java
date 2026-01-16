package gastronomia.sistemaGastronomico;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        this.context = new SpringApplicationBuilder()
                .sources(SistemaGastronomicoApplication.class)
                .initializers(context -> {
                    context.getBeanFactory().registerSingleton("javaFxApplication", this);
                })
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 1. CARGAMOS EL LOGIN AL INICIO
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/RegistroUsuario.fxml"));
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setTitle("Sistema Gastronómico - Acceso");
        stage.setScene(scene);
        stage.centerOnScreen(); // El login suele ser pequeño y centrado
        stage.show();
    }

    @Override
    public void stop() {
        this.context.close();
        Platform.exit();
    }
}