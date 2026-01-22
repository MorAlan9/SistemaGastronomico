package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    private String nombreCompleto;
    private String direccion;
    private String telefono;
    private String pin; // <--- CAMPO NUEVO

    @Enumerated(EnumType.STRING)
    private Rol rol;

    public Usuario() {}

    // Constructor COMPLETO (7 parámetros)
    public Usuario(String u, String p, String n, Rol r, String dir, String tel, String pin) {
        this.username=u; this.password=p; this.nombreCompleto=n; this.rol=r;
        this.direccion=dir; this.telefono=tel; this.pin=pin;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombreCompleto() { return nombreCompleto; }
    public Rol getRol() { return rol; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getPin() { return pin; } // Getter del PIN

    // Setters...
    public void setPin(String pin) { this.pin = pin; }
}