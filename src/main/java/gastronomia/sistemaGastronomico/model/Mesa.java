package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numero;
    private Integer capacidad;

    // RELACIÓN CON SECTOR (NUEVO)
    @ManyToOne
    @JoinColumn(name = "id_sector")
    private Sector sector;

    public Mesa() {
    }

    public Mesa(Integer numero, Integer capacidad) {
        this.numero = numero;
        this.capacidad = capacidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    // Getters y Setters de Sector
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
}