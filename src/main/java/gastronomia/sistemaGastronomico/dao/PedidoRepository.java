package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByEstado(String estado);

    Optional<Pedido> findFirstByMesaAndEstado(Mesa mesa, String estado);

    List<Pedido> findByEstadoOrderByIdDesc(String estado);

    // --- CORREGIDO AQUÍ: Usamos 'metodoPago' en lugar de 'formaPago' ---
    @Query("SELECT p.metodoPago, SUM(p.total) FROM Pedido p " +
            "WHERE p.mozo = :mozo AND p.fecha = :fecha AND p.estado = 'CERRADO' " +
            "GROUP BY p.metodoPago")
    List<Object[]> obtenerTotalesPorMozo(@Param("mozo") Mozo mozo, @Param("fecha") LocalDate fecha);
}