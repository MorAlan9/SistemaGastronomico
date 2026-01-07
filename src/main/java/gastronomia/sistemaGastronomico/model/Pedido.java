package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private LocalTime hora;

    // TIEMPOS
    private LocalDateTime horaComanda;        // Marchado a cocina
    private LocalDateTime horaEntrega;        // <--- NUEVO: Comida en mesa
    private LocalDateTime horaUltimoProducto; // Última actividad

    private String estado;
    private BigDecimal total;

    @ManyToOne
    @JoinColumn(name = "id_mesa")
    private Mesa mesa;

    @ManyToOne
    @JoinColumn(name = "id_mozo")
    private Mozo mozo;

    private Integer comensales;
    private String comentarios;

    @Column(name = "metodo_pago")
    private String metodoPago;

    public Pedido() { }

    public Pedido(LocalDate fecha, LocalTime hora, String estado, BigDecimal total, Mesa mesa, Mozo mozo) {
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.total = total;
        this.mesa = mesa;
        this.mozo = mozo;
        this.horaUltimoProducto = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Nuevo Getter/Setter
    public LocalDateTime getHoraEntrega() { return horaEntrega; }
    public void setHoraEntrega(LocalDateTime horaEntrega) { this.horaEntrega = horaEntrega; }

    public LocalDateTime getHoraComanda() { return horaComanda; }
    public void setHoraComanda(LocalDateTime horaComanda) { this.horaComanda = horaComanda; }
    public LocalDateTime getHoraUltimoProducto() { return horaUltimoProducto; }
    public void setHoraUltimoProducto(LocalDateTime horaUltimoProducto) { this.horaUltimoProducto = horaUltimoProducto; }

    // Resto igual...
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }
    public Mozo getMozo() { return mozo; }
    public void setMozo(Mozo mozo) { this.mozo = mozo; }
    public Integer getComensales() { return comensales; }
    public void setComensales(Integer comensales) { this.comensales = comensales; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}