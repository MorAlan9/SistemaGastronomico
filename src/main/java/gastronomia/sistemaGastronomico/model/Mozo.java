package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mozos")
public class Mozo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String dni;
    private String pin; // <--- CAMPO NUEVO

    public Mozo() {}

    // Constructor NUEVO (Con PIN)
    public Mozo(String nombre, String dni, String pin) {
        this.nombre = nombre;
        this.dni = dni;
        this.pin = pin;
    }

    // Constructor VIEJO (Sin PIN - Arregla el error de "no suitable constructor")
    public Mozo(String nombre, String dni) {
        this.nombre = nombre;
        this.dni = dni;
        this.pin = "0000"; // Pin por defecto para compatibilidad
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDni() { return dni; }
    public String getPin() { return pin; }

    @Override public String toString() { return nombre; }
}