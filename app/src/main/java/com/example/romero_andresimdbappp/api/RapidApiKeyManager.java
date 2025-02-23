package com.example.romero_andresimdbappp.api;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private List<String> apiKeys;
    private int currentIndex;
    public RapidApiKeyManager() {
        // Lista de API Keys
        apiKeys = new ArrayList<>();
        apiKeys.add(0, "74c895ec50msh5642581f6a63ff0p1dd44fjsn0b0b34f8152a"); // Nueva clave al inicio
        apiKeys.add("c1cce6d145msh19937f212457748p163fd5jsnb262629a6f45");
        apiKeys.add("d1f8e6f145msh1b437f21c434648p203fd7jsna4e536a2a9a6");
        apiKeys.add("e8cc36e245msh11237f219122518p905fd1jsnd423822b4f82");
        currentIndex=0; // Comienza con la primera clave
    }
    // Obtiene la clave API actual
    public String getCurrentKey() {
        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("No hay claves API disponibles.");
        }
        return apiKeys.get(currentIndex);
    }
    // Cambia a la siguiente clave API
    public void switchToNextKey() {
        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("No hay claves API disponibles.");
        }
        currentIndex = (currentIndex + 1) % apiKeys.size(); // Cambia a la siguiente clave
    }
    // Verifica si todas las claves están agotadas
    public boolean areAllKeysExhausted() {
        return currentIndex == 0 && apiKeys.size() == 1; // Solo una clave en bucle
    }
}
