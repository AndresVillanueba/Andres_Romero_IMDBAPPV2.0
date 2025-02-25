package com.example.romero_andresimdbappp.api;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private List<String> apiKeys;
    private int currentIndex;

    public RapidApiKeyManager() {
        // Lista de API Keys
        apiKeys = new ArrayList<>();
        apiKeys.add("74c895ec50msh5642581f6a63ff0p1dd44fjsn0b0b34f8152a");
        apiKeys.add("c1cce6d145msh19937f212457748p163fd5jsnb262629a6f45");
        apiKeys.add("d1f8e6f145msh1b437f21c434648p203fd7jsna4e536a2a9a6");
        apiKeys.add("e8cc36e245msh11237f219122518p905fd1jsnd423822b4f82");
        currentIndex = 0; // Comienza con la primera clave
    }

    // Obtiene la clave API actual
    public String getCurrentKey() {
        if (apiKeys.isEmpty()) {
            Log.e("RapidApiKeyManager", "No hay claves API disponibles.");
            throw new IllegalStateException("No hay claves API disponibles.");
        }
        String currentKey = apiKeys.get(currentIndex);
        Log.d("RapidApiKeyManager", "Usando API Key: " + currentKey);
        return currentKey;
    }

    // Cambia a la siguiente clave API y lo registra en Logcat
    public void switchToNextKey() {
        if (apiKeys.isEmpty()) {
            Log.e("RapidApiKeyManager", "No hay claves API disponibles.");
            throw new IllegalStateException("No hay claves API disponibles.");
        }
        currentIndex = (currentIndex + 1) % apiKeys.size(); // Cambia a la siguiente clave
        Log.d("RapidApiKeyManager", "Cambiando a la siguiente API Key: " + apiKeys.get(currentIndex));
    }

    // Verifica si todas las claves han sido agotadas (mejorado)
    public boolean areAllKeysExhausted() {
        return currentIndex == apiKeys.size() - 1;
    }
}
