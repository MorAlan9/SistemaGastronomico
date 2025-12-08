package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private BigDecimal total;

    // --- CAMBIO IMPORTANTE: AHORA SON OBJETOS REALES ---

    @ManyToOne // Muchos pedidos pueden ser en UNA mesa
    @JoinColumn(name = "id_mesa") // La columna FK en SQL
    private Mesa mesa;

    @ManyToOne // Muchos pedidos pueden ser de UN mozo
    @JoinColumn(name = "id_mozo") // La columna FK en SQL
    private Mozo mozo;

    // --- Constructores ---
    public Pedido() { }

    // Actualizamos el constructor para pedir OBJETOS en vez de números
    public Pedido(LocalDate fecha, LocalTime hora, String estado, BigDecimal total, Mesa mesa, Mozo mozo) {
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.total = total;
        this.mesa = mesa;
        this.mozo = mozo;
    }

    private Integer comensales;
    private String comentarios;

    // --- Getters y Setters ---
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

    // Nuevos Getters y Setters de Objetos
    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }
    public Mozo getMozo() { return mozo; }
    public void setMozo(Mozo mozo) { this.mozo = mozo; }


    public Integer getComensales() { return comensales; }
    public void setComensales(Integer comensales) { this.comensales = comensales; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }

    @Column(name = "metodo_pago")
    private String metodoPago;

    // Getters y Setters nuevos
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}