package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

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

    // --- NUEVO: STOCK (Inventario) ---
    @Column(name = "stock")
    private Integer stock;

    // --- NUEVO: ACTIVO (Borrado lógico) ---
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    // ==========================================
    // 1. CONSTRUCTORES
    // ==========================================
    public Producto() {
        // Constructor vacío requerido por JPA
    }

    // Constructor completo para crear productos nuevos fácilmente
    public Producto(String nombre, BigDecimal precioActual, Categoria categoria) {
        this.nombre = nombre;
        this.precioActual = precioActual;
        this.categoria = categoria;
        this.stock = 0;       // Nace con 0 stock si no se especifica
        this.activo = true;   // Nace activo
    }

    // ==========================================
    // 2. GETTERS Y SETTERS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    // Getter inteligente: evita que devuelva 'null'
    public Integer getStock() {
        return (stock != null) ? stock : 0;
    }
    public void setStock(Integer stock) { this.stock = stock; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    // ==========================================
    // 3. MÉTODOS AUXILIARES
    // ==========================================
    @Override
    public String toString() {
        // Esto se muestra en las listas si no configuras una celda personalizada
        String cat = (categoria != null) ? categoria.getNombre() : "Sin Cat";
        return nombre + " ($" + precioActual + ") [Stock: " + getStock() + "]";
    }

    // Buena práctica: Comparar por ID para evitar duplicados en listas
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}