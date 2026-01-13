package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;
import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepo;
    private final MesaRepository mesaRepo;
    private final MozoRepository mozoRepo;

    public PedidoController(PedidoService pedidoService, PedidoRepository pedidoRepo,
                            MesaRepository mesaRepo, MozoRepository mozoRepo) {
        this.pedidoService = pedidoService;
        this.pedidoRepo = pedidoRepo;
        this.mesaRepo = mesaRepo;
        this.mozoRepo = mozoRepo;
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }

    @PostMapping("/abrir")
    public Pedido abrirMesa(@RequestParam Long idMesa, @RequestParam Long idMozo) {
        Mesa mesa = mesaRepo.findById(idMesa).orElseThrow();
        Mozo mozo = mozoRepo.findById(idMozo).orElseThrow();
        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesa, mozo);
        return pedidoRepo.save(nuevo);
    }

    @PostMapping("/{idPedido}/agregar")
    public String agregarProducto(@PathVariable Long idPedido,
                                  @RequestParam Long idProducto,
                                  @RequestParam Integer cantidad) {
        pedidoService.agregarProducto(idPedido, idProducto, cantidad);
        return "Producto agregado correctamente.";
    }

    // --- ESTE ERA EL QUE DABA ERROR ---
    @PostMapping("/{idPedido}/cerrar")
    public String cerrarMesa(@PathVariable Long idPedido) {
        // Ahora sí funciona porque agregamos 'cerrarMesa' en el Service
        pedidoService.cerrarMesa(idPedido);
        return "Mesa cerrada correctamente.";
    }
}