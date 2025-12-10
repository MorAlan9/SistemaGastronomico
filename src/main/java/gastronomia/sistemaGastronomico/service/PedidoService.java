package gastronomia.sistemaGastronomico.service;

import gastronomia.sistemaGastronomico.dao.DetallePedidoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
import gastronomia.sistemaGastronomico.model.DetallePedido;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Producto;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final DetallePedidoRepository detalleRepo;

    public PedidoService(PedidoRepository pedidoRepo,
                         ProductoRepository productoRepo,
                         DetallePedidoRepository detalleRepo) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.detalleRepo = detalleRepo;
    }

    /**
     * AGREGAR PRODUCTO (INTELIGENTE)
     * Si el producto ya existe en la mesa, suma la cantidad al renglón existente.
     * Si no existe, crea uno nuevo.
     */
    @Transactional
    public void agregarProducto(Long idPedido, Long idProducto, Integer cantidad) {

        // 1. Buscamos Pedido y Producto
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 2. ¿Ya existe este producto en este pedido?
        Optional<DetallePedido> existente = detalleRepo.findByPedidoAndProducto(pedido, producto);

        if (existente.isPresent()) {
            // CASO A: YA EXISTE -> Solo sumamos la cantidad
            DetallePedido detalle = existente.get();
            detalle.setCantidad(detalle.getCantidad() + cantidad);
            detalleRepo.save(detalle);
        } else {
            // CASO B: NO EXISTE -> Creamos un renglón nuevo
            DetallePedido nuevo = new DetallePedido(pedido, producto, cantidad, producto.getPrecioActual());
            detalleRepo.save(nuevo);
        }

        // 3. Actualizar el TOTAL ($) del Pedido
        BigDecimal subtotal = producto.getPrecioActual().multiply(new BigDecimal(cantidad));
        BigDecimal nuevoTotal = pedido.getTotal().add(subtotal);
        pedido.setTotal(nuevoTotal);

        pedidoRepo.save(pedido);
        System.out.println("➕ Producto agregado/sumado. Nuevo total: $" + nuevoTotal);
    }

    /**
     * QUITAR PRODUCTO (RESTAR DE A UNO)
     * Resta 1 a la cantidad. Si queda en 0, elimina el renglón.
     */
    @Transactional
    public void quitarProducto(Long idDetalle) {
        // 1. Buscamos el detalle
        DetallePedido detalle = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new RuntimeException("El detalle no existe"));

        Pedido pedido = detalle.getPedido();
        BigDecimal precioUnitario = detalle.getPrecioUnitario();

        // 2. Restamos 1 a la cantidad actual
        int nuevaCantidad = detalle.getCantidad() - 1;

        if (nuevaCantidad > 0) {
            // Si todavía quedan (ej: bajó de 3 a 2), actualizamos
            detalle.setCantidad(nuevaCantidad);
            detalleRepo.save(detalle);
        } else {
            // Si llegó a 0, borramos el renglón de la base de datos
            detalleRepo.delete(detalle);
        }

        // 3. Descontamos el precio de UNA unidad al total del pedido
        BigDecimal nuevoTotal = pedido.getTotal().subtract(precioUnitario);
        // Evitamos totales negativos por seguridad
        if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) nuevoTotal = BigDecimal.ZERO;

        pedido.setTotal(nuevoTotal);
        pedidoRepo.save(pedido);

        System.out.println("➖ Producto restado. Nuevo total: $" + nuevoTotal);
    }

    /**
     * CERRAR MESA
     * Finaliza el pedido cambiándolo a estado 'CERRADO'.
     */
    @Transactional
    public void cerrarMesa(Long idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("CERRADO".equals(pedido.getEstado())) {
            throw new RuntimeException("¡La mesa ya estaba cerrada!");
        }

        pedido.setEstado("CERRADO");
        pedidoRepo.save(pedido);

        System.out.println("✅ Mesa cerrada correctamente.");
    }
}