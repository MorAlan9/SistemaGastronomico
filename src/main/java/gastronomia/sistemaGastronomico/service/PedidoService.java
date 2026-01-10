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
import java.util.List;
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

    // --- AGREGAR PRODUCTO ---
    public void agregarProducto(Long idPedido, Long idProducto, Integer cantidad) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 1. OBTENER TODOS LOS DETALLES DEL PEDIDO
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedido);

        // 2. BUSCAR SI EXISTE UNA LÍNEA "PENDIENTE" (horaMarchar == null) PARA ESTE PRODUCTO
        // Si el producto ya se marchó, horaMarchar tendrá fecha y NO entrará aquí.
        Optional<DetallePedido> detallePendiente = detalles.stream()
                .filter(d -> d.getProducto().getId().equals(idProducto) && d.getHoraMarchar() == null)
                .findFirst();

        if (detallePendiente.isPresent()) {
            // CASO A: Ya existe una línea NUEVA sin marchar -> SUMAMOS CANTIDAD
            // Ej: Tenía 1 Coca nueva y agrego otra -> Ahora son 2 Cocas nuevas.
            DetallePedido d = detallePendiente.get();
            d.setCantidad(d.getCantidad() + cantidad);
            detalleRepo.save(d);
        } else {
            // CASO B: No existe línea o las que existen YA MARCHARON -> CREAMOS NUEVA LÍNEA
            // Ej: Ya marché 2 Lomos. Agrego 1 Lomo más.
            // El sistema creará una línea aparte para ese nuevo Lomo (horaMarchar = null).
            DetallePedido nuevo = new DetallePedido();
            nuevo.setPedido(pedido);
            nuevo.setProducto(producto);
            nuevo.setCantidad(cantidad);
            nuevo.setPrecioUnitario(producto.getPrecioActual());
            // horaMarchar nace nulo automáticamente
            detalleRepo.save(nuevo);
        }

        // Opcional: Recalcular total del pedido aquí si tu sistema lo requiere
        recalcularTotal(pedido);
    }

    // Método auxiliar (si no lo tienes, agrégalo para mantener el total sincronizado)
    private void recalcularTotal(Pedido pedido) {
        BigDecimal total = detalleRepo.findByPedido(pedido).stream()
                .map(d -> d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);
        pedidoRepo.save(pedido);
    }

    // --- QUITAR PRODUCTO ---
    @Transactional
    public void quitarProducto(Long idDetalle) {
        DetallePedido detalle = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

        Pedido pedido = detalle.getPedido();
        BigDecimal precio = detalle.getPrecioUnitario();

        if (detalle.getCantidad() > 1) {
            detalle.setCantidad(detalle.getCantidad() - 1);
            detalleRepo.save(detalle);
        } else {
            detalleRepo.delete(detalle);
        }

        BigDecimal nuevoTotal = pedido.getTotal().subtract(precio);
        if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) nuevoTotal = BigDecimal.ZERO;

        pedido.setTotal(nuevoTotal);
        pedidoRepo.save(pedido);
    }

    // --- CERRAR MESA / COBRAR (SOLUCIÓN HISTORIAL) ---
    @Transactional
    public void cobrarPedido(Long idPedido, String metodoPago, BigDecimal totalFinal) {
        // 1. Buscar Pedido
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // 2. Validar que no esté cerrado ya
        if ("CERRADO".equals(pedido.getEstado())) {
            throw new RuntimeException("Esta mesa ya fue cobrada anteriormente.");
        }

        // 3. Actualizar Datos
        pedido.setMetodoPago(metodoPago);
        pedido.setTotal(totalFinal);
        pedido.setEstado("CERRADO"); // Esto hace que aparezca en el historial

        // 4. Descontar Stock
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedido);
        for (DetallePedido det : detalles) {
            Producto prod = det.getProducto();
            int nuevoStock = prod.getStock() - det.getCantidad();
            prod.setStock(nuevoStock);
            productoRepo.save(prod);
        }

        // 5. Guardar Cambios
        pedidoRepo.save(pedido);
        System.out.println("✅ Venta CERRADA y guardada: Mesa " + pedido.getMesa().getNumero());
    }

    // Método legacy por compatibilidad (si se usa en otro lado)
    @Transactional
    public void cerrarMesa(Long idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido).orElseThrow();
        pedido.setEstado("CERRADO");
        pedidoRepo.save(pedido);
    }



    // --- MARCAR COMIDA ENTREGADA ---
    @Transactional
    public void marcarEntrega(Long idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getHoraComanda() == null) {
            throw new RuntimeException("¡No se puede entregar algo que no se ha comandado!");
        }

        // Si ya estaba entregado, no hacemos nada (o actualizamos la hora, según prefieras)
        if (pedido.getHoraEntrega() != null) {
            throw new RuntimeException("El pedido ya figura como entregado.");
        }

        pedido.setHoraEntrega(java.time.LocalDateTime.now());
        pedidoRepo.save(pedido);

        System.out.println("🍽 Pedido entregado: Mesa " + pedido.getMesa().getNumero());
    }
}