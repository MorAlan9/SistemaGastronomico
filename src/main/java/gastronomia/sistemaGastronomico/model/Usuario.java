package gastronomia.sistemaGastronomico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username; // Para loguearse
    private String password;
    private String nombreCompleto;
    private String direccion; // Para Google Maps
    private String telefono;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    public Usuario() {}
    public Usuario(String u, String p, String n, Rol r, String dir, String tel) {
        this.username=u; this.password=p; this.nombreCompleto=n; this.rol=r; this.direccion=dir; this.telefono=tel;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombreCompleto() { return nombreCompleto; }
    public Rol getRol() { return rol; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    // ... resto de setters
}