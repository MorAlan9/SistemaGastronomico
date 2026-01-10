package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    // Usamos el Objeto Categoria (Relación ManyToOne)
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private BigDecimal precioActual;
    private Integer stock;
    private Boolean activo;

    // Campo nuevo para saber si demora
    @Column(name = "es_cocina")
    private boolean esCocina;

    public Producto() {}

    public Producto(String nombre, Categoria categoria, BigDecimal precio, Integer stock, Boolean activo, boolean esCocina) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioActual = precio;
        this.stock = stock;
        this.activo = activo;
        this.esCocina = esCocina;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean isEsCocina() { return esCocina; }
    public void setEsCocina(boolean esCocina) { this.esCocina = esCocina; }
}