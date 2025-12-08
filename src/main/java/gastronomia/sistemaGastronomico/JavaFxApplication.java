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
        // 1. Iniciamos Spring Boot manualmente antes de que aparezca la ventana
        this.context = new SpringApplicationBuilder()
                .sources(SistemaGastronomicoApplication.class)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 2. Cargamos tu archivo visual (.fxml)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MenuPrincipal.fxml"));

        // ¡MAGIA! Esto permite que tus controladores visuales usen @Autowired
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root);


        stage.setTitle("Sistema Gastronómico");
        stage.setScene(scene);
        stage.setMaximized(true); // Que arranque en pantalla completa como un POS real
        stage.show();
    }

    @Override
    public void stop() {
        // 3. Si cierras la ventana, apagamos Spring Boot
        this.context.close();
        Platform.exit();
    }
}