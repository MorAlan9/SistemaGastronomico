package gastronomia.sistemaGastronomico.controller;

import gastronomia.sistemaGastronomico.model.Pedido;
import gastronomia.sistemaGastronomico.service.PedidoService;
import gastronomia.sistemaGastronomico.dao.PedidoRepository;
import gastronomia.sistemaGastronomico.dao.MesaRepository;
import gastronomia.sistemaGastronomico.dao.MozoRepository;
import gastronomia.sistemaGastronomico.model.Mesa;
import gastronomia.sistemaGastronomico.model.Mozo;

import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController // 1. Dice: "Soy una antena que recibe peticiones Web"
@RequestMapping("/api/pedidos") // 2. Todas las URLs empezarán con esta dirección
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepo;
    // (Solo para simplificar la apertura, inyectamos estos repos extra)
    private final MesaRepository mesaRepo;
    private final MozoRepository mozoRepo;

    public PedidoController(PedidoService pedidoService, PedidoRepository pedidoRepo,
                            MesaRepository mesaRepo, MozoRepository mozoRepo) {
        this.pedidoService = pedidoService;
        this.pedidoRepo = pedidoRepo;
        this.mesaRepo = mesaRepo;
        this.mozoRepo = mozoRepo;
    }

    // --- A. VER TODOS LOS PEDIDOS (GET) ---
    // URL: http://localhost:8080/api/pedidos
    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }

    // --- B. ABRIR UNA MESA (POST) ---
    // URL: http://localhost:8080/api/pedidos/abrir?idMesa=1&idMozo=1
    @PostMapping("/abrir")
    public Pedido abrirMesa(@RequestParam Long idMesa, @RequestParam Long idMozo) {
        Mesa mesa = mesaRepo.findById(idMesa).orElseThrow();
        Mozo mozo = mozoRepo.findById(idMozo).orElseThrow();

        Pedido nuevo = new Pedido(LocalDate.now(), LocalTime.now(), "ABIERTO", BigDecimal.ZERO, mesa, mozo);
        return pedidoRepo.save(nuevo);
    }

    // --- C. AGREGAR PLATO (POST) ---
    // URL: http://localhost:8080/api/pedidos/{id}/agregar?idProducto=5&cantidad=2
    @PostMapping("/{idPedido}/agregar")
    public String agregarProducto(@PathVariable Long idPedido,
                                  @RequestParam Long idProducto,
                                  @RequestParam Integer cantidad) {

        pedidoService.agregarProducto(idPedido, idProducto, cantidad);
        return "✅ Producto agregado correctamente.";
    }

    // --- D. CERRAR MESA (POST) ---
    // URL: http://localhost:8080/api/pedidos/{id}/cerrar
    @PostMapping("/{idPedido}/cerrar")
    public String cerrarMesa(@PathVariable Long idPedido) {
        pedidoService.cerrarMesa(idPedido);
        return "✅ Mesa cerrada. Facturación completada.";
    }
}