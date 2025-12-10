package gastronomia.sistemaGastronomico.dao;

import gastronomia.sistemaGastronomico.model.Categoria;
import gastronomia.sistemaGastronomico.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Validar duplicados
    boolean existsByNombreIgnoreCase(String nombre);

    // --- FILTROS DE ACTIVOS (Para no mostrar los borrados) ---

    // 1. Traer TODO lo activo (Para el botón "TODO" o recarga inicial)
    List<Producto> findByActivoTrue();

    // 2. Traer por Categoría solo activos (Para los botones de filtros)
    List<Producto> findByCategoriaAndActivoTrue(Categoria categoria);

    // NOTA: El método findAll() sigue existiendo, pero trae borrados también.
    // Solo lo usaremos en Admin si quisiéramos ver historial, pero en Pedidos usaremos estos.
}