package gastronomia.sistemaGastronomico.utils;

import javafx.application.HostServices;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleMaps {
    // Necesitamos pasarle el HostServices desde la Application principal
    public static void abrirMapa(HostServices hostServices, String direccion) {
        if (direccion == null || direccion.trim().isEmpty()) return;
        try {
            // Codificar la dirección para URL (espacios a %20, etc.)
            String query = URLEncoder.encode(direccion, StandardCharsets.UTF_8);
            String url = "https://www.google.com/maps/search/?api=1&query=" + query;
            hostServices.showDocument(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}