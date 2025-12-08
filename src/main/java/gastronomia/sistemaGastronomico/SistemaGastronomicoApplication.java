package gastronomia.sistemaGastronomico;

import gastronomia.sistemaGastronomico.dao.CategoriaRepository;
import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import javafx.application.Application;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SistemaGastronomicoApplication {

    public static void main(String[] args) {
        // Lanzamos la aplicación gráfica JavaFX
        Application.launch(JavaFxApplication.class, args);
    }

    /**
     * ESTO SE EJECUTA AL INICIO PARA CARGAR DATOS BASE
     * Si tu base de datos está vacía (recién instalada), esto crea lo mínimo necesario.
     */
    @Bean
    public CommandLineRunner iniciarDatos(CategoriaRepository categoriaRepo,
                                          MesaRepository mesaRepo,
                                          MozoRepository mozoRepo) {
        return args -> {

            // 1. Crear Categorías si no existen
            if (categoriaRepo.count() == 0) {
                categoriaRepo.save(new Categoria("Cocina"));
                categoriaRepo.save(new Categoria("Barra"));
                categoriaRepo.save(new Categoria("Cafetería"));
                categoriaRepo.save(new Categoria("Postres"));
                System.out.println("✅ Base de Datos: Categorías iniciales creadas.");
            }

            // 2. Crear Mesas si no existen
            if (mesaRepo.count() == 0) {
                mesaRepo.save(new Mesa(1, 4));
                mesaRepo.save(new Mesa(2, 2));
                mesaRepo.save(new Mesa(3, 4));
                mesaRepo.save(new Mesa(4, 6));
                mesaRepo.save(new Mesa(5, 8));
                System.out.println("✅ Base de Datos: Mesas iniciales creadas.");
            }

            // 3. Crear un Mozo por defecto si no existe
            if (mozoRepo.count() == 0) {
                mozoRepo.save(new Mozo("Admin", "00000000"));
                System.out.println("✅ Base de Datos: Mozo Admin creado.");
            }

            System.out.println("🚀 SISTEMA LISTO PARA USAR.");
        };
    }
}