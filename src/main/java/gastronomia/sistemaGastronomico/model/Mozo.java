package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mozos")
public class Mozo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String dni;
    private String pin;

    public Mozo() {}

    // Constructor COMPLETO (Nombre, DNI, PIN)
    public Mozo(String nombre, String dni, String pin) {
        this.nombre = nombre;
        this.dni = dni;
        this.pin = pin;
    }

    // Constructor DE COMPATIBILIDAD (Solo Nombre y DNI) <- ESTE ES EL QUE TE FALTA
    public Mozo(String nombre, String dni) {
        this.nombre = nombre;
        this.dni = dni;
        this.pin = "0000"; // PIN por defecto
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    @Override public String toString() { return nombre; }
}