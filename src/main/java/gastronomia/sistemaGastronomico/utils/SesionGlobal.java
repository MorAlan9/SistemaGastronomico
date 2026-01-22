package gastronomia.sistemaGastronomico.utils;

import gastronomia.sistemaGastronomico.model.Mozo;     // <--- IMPORTANTE: Usamos Mozo
import gastronomia.sistemaGastronomico.model.Usuario;

public class SesionGlobal {
    public static Usuario usuarioActual; // El Admin/Cajero logueado
    public static Mozo mozoActual;       // El Mozo del PIN <--- ESTE ES EL QUE FALTA
}