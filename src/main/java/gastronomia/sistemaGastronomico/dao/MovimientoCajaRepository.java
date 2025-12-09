package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    // Buscar los últimos movimientos primero (orden descendente)
    List<MovimientoCaja> findAllByOrderByFechaHoraDesc();
}