package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Ahora buscamos por el OBJETO Categoria real
    List<Producto> findByCategoria(Categoria categoria);
}