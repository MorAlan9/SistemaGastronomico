package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mozo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MozoRepository extends JpaRepository<Mozo, Long> {
    // Buscamos por PIN en la tabla de MOZOS
    Optional<Mozo> findByPin(String pin);

}