package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Producto;
import gastronomia.sistemaGastronomico.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    // Método extra: Trae todos los renglones (platos) de un pedido específico
    // Al usar el objeto "Pedido", JPA hace la magia con la Foreign Key
    List<DetallePedido> findByPedido(Pedido pedido);

    Optional<DetallePedido> findByPedidoAndProducto(Pedido pedido, Producto producto);

    long countByPedido(Pedido pedido);
}
