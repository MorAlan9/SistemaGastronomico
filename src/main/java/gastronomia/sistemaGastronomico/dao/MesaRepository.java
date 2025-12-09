package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    // Buscar mesas de un sector específico
    List<Mesa> findBySector(Sector sector);

    // Contar cuántas mesas hay en un sector (para el texto "5/10")
    long countBySector(Sector sector);
}