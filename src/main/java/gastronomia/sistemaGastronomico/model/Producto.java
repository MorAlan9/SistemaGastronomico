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

    // RELACIÓN NUEVA: Un producto pertenece a una Categoría
    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    public Producto() { }

    public Producto(String nombre, BigDecimal precioActual, Categoria categoria) {
        this.nombre = nombre;
        this.precioActual = precioActual;
        this.categoria = categoria;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    @Override
    public String toString() {
        // Validación por si la categoría viene null (seguridad)
        String nombreCat = (categoria != null) ? categoria.getNombre() : "Sin Categoría";
        return nombre + " ($" + precioActual + ") [" + nombreCat + "]";
    }
}