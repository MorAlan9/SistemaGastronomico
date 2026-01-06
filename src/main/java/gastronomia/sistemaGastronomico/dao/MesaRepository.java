package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    // Buscar mesas de un sector específico
    List<Mesa> findBySector(Sector sector);

    // Contar cuántas mesas hay en un sector
    long countBySector(Sector sector);

    // Filtra automáticamente las mesas ocultas (Solo activas)
    List<Mesa> findBySectorAndActivaTrue(Sector sector);

    // --- SOLUCIÓN AL ERROR ---
    // 'findFirst': Si hay duplicados, toma solo el primero (evita el error de 4 resultados).
    // 'AndActivaTrue': Ignora las mesas que borraste anteriormente.
    Optional<Mesa> findFirstByNumeroAndActivaTrue(Integer numero);
}