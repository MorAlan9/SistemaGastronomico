package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(name = "precio_actual")
    private BigDecimal precioActual;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    // --- NUEVO CAMPO ---
    @Column(name = "stock")
    private Integer stock;

    public Producto() { }

    public Producto(String nombre, BigDecimal precioActual, Categoria categoria) {
        this.nombre = nombre;
        this.precioActual = precioActual;
        this.categoria = categoria;
        this.stock = 0; // Valor por defecto
    }

    // ... (Tus Getters y Setters anteriores id, nombre, precio...) ...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    // --- NUEVOS GETTERS Y SETTERS PARA STOCK ---
    public Integer getStock() {
        return (stock != null) ? stock : 0; // Evita NullPointerException
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        String nombreCat = (categoria != null) ? categoria.getNombre() : "Sin Categoría";
        // Agregamos visualmente el stock al toString (opcional)
        return nombre + " ($" + precioActual + ") [Stock: " + getStock() + "]";
    }
}