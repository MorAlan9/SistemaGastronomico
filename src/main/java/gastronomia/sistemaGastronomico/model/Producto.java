package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto") // Aseguramos que busque la tabla correcta
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private BigDecimal precioActual;
    private Integer stock;

    // --- CAMBIO CLAVE: Usar Boolean (Objeto) en vez de boolean (primitivo) ---
    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean activo = true;

    @Column(name = "es_cocina")
    private Boolean esCocina = false;

    public Producto() {}

    public Producto(String nombre, Categoria categoria, BigDecimal precio, Integer stock, Boolean activo, Boolean esCocina) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioActual = precio;
        this.stock = stock;
        this.activo = activo != null ? activo : true;
        this.esCocina = esCocina != null ? esCocina : false;
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

    // --- GETTERS BLINDADOS PARA EVITAR NULOS EN LA VISTA ---

    public Boolean isActivo() {
        return activo != null ? activo : true; // Si es null, devuelve true por defecto
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean isEsCocina() {
        return esCocina != null ? esCocina : false; // Si es null, devuelve false
    }

    public void setEsCocina(Boolean esCocina) {
        this.esCocina = esCocina;
    }
}