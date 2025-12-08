package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Mesa; // ¡Importante importar Mesa!
import gastronomia.sistemaGastronomico.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar pedidos abiertos, cerrados, etc.
    List<Pedido> findByEstado(String estado);



    // Busca un pedido específico de una mesa que tenga cierto estado (ej: ABIERTO)
    // Usamos 'Optional' porque puede que no exista ninguno.
    Optional<Pedido> findFirstByMesaAndEstado(Mesa mesa, String estado);

    List<Pedido>findByEstadoOrderByIdDesc(String estado);
}

