package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private LocalTime hora;
    private String estado; // "ABIERTO", "CERRADO"
    private BigDecimal total;

    // --- CAMPO IMPORTANTE PARA EL COBRO ---
    private String metodoPago; // "Efectivo", "Tarjeta", "QR"

    private LocalDateTime horaComanda;
    private LocalDateTime horaEntrega;
    private LocalDateTime horaUltimoProducto;

    // Cantidad de personas en la mesa (opcional)
    private Integer comensales;

    @ManyToOne
    @JoinColumn(name = "id_mesa")
    private Mesa mesa;

    @ManyToOne
    @JoinColumn(name = "id_mozo")
    private Mozo mozo;

    public Pedido() {
    }

    public Pedido(LocalDate fecha, LocalTime hora, String estado, BigDecimal total, Mesa mesa, Mozo mozo) {
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.total = total;
        this.mesa = mesa;
        this.mozo = mozo;
    }

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }

    public Mozo getMozo() { return mozo; }
    public void setMozo(Mozo mozo) { this.mozo = mozo; }

    public LocalDateTime getHoraComanda() { return horaComanda; }
    public void setHoraComanda(LocalDateTime horaComanda) { this.horaComanda = horaComanda; }

    public LocalDateTime getHoraEntrega() { return horaEntrega; }
    public void setHoraEntrega(LocalDateTime horaEntrega) { this.horaEntrega = horaEntrega; }

    public LocalDateTime getHoraUltimoProducto() { return horaUltimoProducto; }
    public void setHoraUltimoProducto(LocalDateTime horaUltimoProducto) { this.horaUltimoProducto = horaUltimoProducto; }

    public Integer getComensales() { return comensales; }
    public void setComensales(Integer comensales) { this.comensales = comensales; }

    // CAMPO NUEVO: Referencia al pedido original
    @Column(name = "id_pedido_padre")
    private Long idPedidoPadre;

    // Getter y Setter
    public Long getIdPedidoPadre() {
        return idPedidoPadre;
    }

    public void setIdPedidoPadre(Long idPedidoPadre) {
        this.idPedidoPadre = idPedidoPadre;
    }
}