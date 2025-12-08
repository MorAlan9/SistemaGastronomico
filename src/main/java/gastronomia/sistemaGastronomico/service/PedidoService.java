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

@Service // <--- ¡Esto es clave! Le dice a Spring "Este es el Chef"
public class PedidoService {

    // 1. Llamamos a los ayudantes (Repositorios)
    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final DetallePedidoRepository detalleRepo;

    // 2. Constructor: Spring nos inyecta los repositorios automáticamente
    public PedidoService(PedidoRepository pedidoRepo,
                         ProductoRepository productoRepo,
                         DetallePedidoRepository detalleRepo) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.detalleRepo = detalleRepo;
    }

    /**
     * Lógica para agregar un plato y QUE SE SUME SOLO AL TOTAL
     */
    @Transactional // Si falla algo, no guarda nada (seguridad)
    public void agregarProducto(Long idPedido, Long idProducto, Integer cantidad) {

        // A. Buscamos el Pedido y el Producto en la Base de Datos
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // B. Creamos el Detalle (El renglón de la comanda)
        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getPrecioActual()); // Congelamos precio actual

        // Guardamos el detalle
        detalleRepo.save(detalle);

        // C. LA MAGIA: Calculamos el subtotal y sumamos al pedido
        // Subtotal = Precio * Cantidad
        BigDecimal subtotal = producto.getPrecioActual().multiply(new BigDecimal(cantidad));

        // Total Nuevo = Total Viejo + Subtotal
        BigDecimal totalActualizado = pedido.getTotal().add(subtotal);
        pedido.setTotal(totalActualizado);

        // D. Guardamos el Pedido actualizado con el nuevo precio
        pedidoRepo.save(pedido);

        System.out.println("👨‍🍳 SERVICE: Se agregaron " + cantidad + " " + producto.getNombre() +
                ". Nuevo total de la mesa: $" + totalActualizado);


    }

    /**
     * MÉTODO 2: ELIMINAR PLATO (Corrección de error)
     * Borra el item y descuenta la plata del total.
     */
    @Transactional
    public void eliminarPlato(Long idDetalle) {
        // 1. Buscamos el detalle a borrar
        DetallePedido detalle = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new RuntimeException("El detalle no existe"));

        // 2. Recuperamos el pedido padre para descontar la plata
        Pedido pedido = detalle.getPedido();

        // 3. Calculamos cuánto hay que restar (Precio * Cantidad)
        BigDecimal montoARestar = detalle.getPrecioUnitario()
                .multiply(new BigDecimal(detalle.getCantidad()));

        // 4. Actualizamos el total del pedido (Total Viejo - Monto a Restar)
        BigDecimal nuevoTotal = pedido.getTotal().subtract(montoARestar);
        pedido.setTotal(nuevoTotal);

        // 5. Borramos el detalle y guardamos el pedido corregido
        detalleRepo.delete(detalle); // Borra de la BDD
        pedidoRepo.save(pedido);     // Actualiza el total

        System.out.println("🗑️ SERVICE: Se eliminó " + detalle.getProducto().getNombre() +
                ". Se descontaron: $" + montoARestar);
    }

    /**
     * MÉTODO 3: CERRAR MESA (Facturar)
     * Cambia el estado para liberar la mesa.
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

        System.out.println("✅ SERVICE: Mesa cerrada. Total final facturado: $" + pedido.getTotal());
    }
}