package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    // Trae todo ordenado por fecha (el más nuevo arriba)
    List<MovimientoCaja> findAllByOrderByFechaHoraDesc();

    // ESTO ES LO NUEVO: Filtro "Between" (Entre fechas) para la caja
    List<MovimientoCaja> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);
}