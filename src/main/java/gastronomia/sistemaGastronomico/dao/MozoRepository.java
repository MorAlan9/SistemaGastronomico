package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mozo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MozoRepository extends JpaRepository<Mozo, Long> {
}