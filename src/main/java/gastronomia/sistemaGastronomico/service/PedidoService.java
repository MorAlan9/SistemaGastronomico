package gastronomia.sistemaGastronomico.service;

import gastronomia.sistemaGastronomico.dao.DetallePedidoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.ProductoRepository;
// import gastronomia.sistemaGastronomico.dao.MovimientoCajaRepository; // <--- DESCOMENTAR SI USAS CAJA
// import gastronomia.sistemaGastronomico.model.MovimientoCaja;         // <--- DESCOMENTAR SI USAS CAJA
import gastronomia.sistemaGastronomico.model.DetallePedido;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.model.Producto;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final DetallePedidoRepository detalleRepo;
    // private final MovimientoCajaRepository cajaRepo; // <--- INYECTAR ESTO

    public PedidoService(PedidoRepository pedidoRepo,
                         ProductoRepository productoRepo,
                         DetallePedidoRepository detalleRepo
            /*, MovimientoCajaRepository cajaRepo*/) { // <--- AGREGAR AL CONSTRUCTOR
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.detalleRepo = detalleRepo;
        // this.cajaRepo = cajaRepo;
    }

    public void agregarProducto(Long idPedido, Long idProducto, Integer cantidad) {
        Pedido pedido = pedidoRepo.findById(idPedido).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        Producto producto = productoRepo.findById(idProducto).orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<DetallePedido> detalles = detalleRepo.findByPedido(pedido);
        Optional<DetallePedido> detallePendiente = detalles.stream()
                .filter(d -> d.getProducto().getId().equals(idProducto) && d.getHoraMarchar() == null)
                .findFirst();

        if (detallePendiente.isPresent()) {
            DetallePedido d = detallePendiente.get();
            d.setCantidad(d.getCantidad() + cantidad);
            detalleRepo.save(d);
        } else {
            DetallePedido nuevo = new DetallePedido();
            nuevo.setPedido(pedido);
            nuevo.setProducto(producto);
            nuevo.setCantidad(cantidad);
            nuevo.setPrecioUnitario(producto.getPrecioActual());
            detalleRepo.save(nuevo);
        }
        recalcularTotal(pedido);
    }

    private void recalcularTotal(Pedido pedido) {
        BigDecimal total = detalleRepo.findByPedido(pedido).stream()
                .map(d -> d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);
        pedidoRepo.save(pedido);
    }

    @Transactional
    public void quitarProducto(Long idDetalle) {
        DetallePedido detalle = detalleRepo.findById(idDetalle).orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        Pedido pedido = detalle.getPedido();

        if (detalle.getCantidad() > 1) {
            detalle.setCantidad(detalle.getCantidad() - 1);
            detalleRepo.save(detalle);
        } else {
            detalleRepo.delete(detalle);
        }
        recalcularTotal(pedido);
    }

    // --- COBRAR PEDIDO (MÉTODO CLAVE) ---
    @Transactional
    public void cobrarPedido(Long idPedido, String metodoPago, BigDecimal totalFinal) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // 1. ACTUALIZAR PEDIDO
        pedido.setMetodoPago(metodoPago);
        pedido.setTotal(totalFinal);
        pedido.setEstado("CERRADO");

        // IMPORTANTE: Actualizamos la fecha al momento del cobro
        // Si no haces esto, la venta queda con fecha de "apertura" y puede no salir en el reporte de hoy.
        pedido.setFecha(LocalDate.now());
        pedido.setHora(LocalTime.now());

        // 2. DESCONTAR STOCK
        List<DetallePedido> detalles = detalleRepo.findByPedido(pedido);
        for (DetallePedido det : detalles) {
            Producto prod = det.getProducto();
            int nuevoStock = prod.getStock() - det.getCantidad();
            prod.setStock(nuevoStock);
            productoRepo.save(prod);
        }

        // 3. GUARDAR EL PEDIDO CERRADO
        pedidoRepo.save(pedido);

        // 4. (OPCIONAL) GENERAR MOVIMIENTO DE CAJA
        // Si tu pantalla de Ventas lee de "MovimientoCaja", descomenta esto y ajusta el modelo:
        /*
        MovimientoCaja mov = new MovimientoCaja();
        mov.setFecha(LocalDateTime.now());
        mov.setTipoMovimiento("INGRESO"); // o "VENTA"
        mov.setConcepto("Venta Mesa " + pedido.getMesa().getNumero());
        mov.setMonto(totalFinal);
        mov.setFormaPago(metodoPago);
        mov.setUsuario("Sistema"); // O el mozo
        cajaRepo.save(mov);
        */

        System.out.println("✅ Venta registrada: Mesa " + pedido.getMesa().getNumero() + " ($" + totalFinal + ")");
    }

    @Transactional
    public void cerrarMesa(Long idPedido) {
        cobrarPedido(idPedido, "Efectivo", BigDecimal.ZERO);
    }

    @Transactional
    public void marcarEntrega(Long idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        if (pedido.getHoraEntrega() == null) {
            pedido.setHoraEntrega(java.time.LocalDateTime.now());
            pedidoRepo.save(pedido);
        }
    }

    public String generarReporteMozo(Mozo mozo) {
        List<Object[]> resultados = pedidoRepo.obtenerTotalesPorMozo(mozo, LocalDate.now());
        if (resultados.isEmpty()) return "Sin ventas hoy para " + mozo.getNombre();

        StringBuilder sb = new StringBuilder();
        BigDecimal granTotal = BigDecimal.ZERO;

        sb.append("REPORTE: ").append(mozo.getNombre()).append("\n------------------\n");
        for (Object[] fila : resultados) {
            String metodo = (String) fila[0];
            BigDecimal total = (BigDecimal) fila[1];
            if (metodo == null) metodo = "Sin definir";
            sb.append(String.format("%-15s : $%s\n", metodo, total));
            granTotal = granTotal.add(total);
        }
        sb.append("------------------\nTOTAL: $").append(granTotal);
        return sb.toString();
    }
}