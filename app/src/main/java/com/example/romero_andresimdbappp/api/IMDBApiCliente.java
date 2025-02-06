package com.example.romero_andresimdbappp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IMDBApiCliente {
    private static final String BASE_URL = "https://imdb-com.p.rapidapi.com/";
    private static final String API_HOST = "imdb-com.p.rapidapi.com";
    private static IMDBApiService apiService;
    private static RapidApiKeyManager apiKeyManager = new RapidApiKeyManager();
    // Método para obtener el servicio API
    public static IMDBApiService getApiService() {
        if (apiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(IMDBApiService.class);
        }
        return apiService;
    }
    // Método para obtener la clave API actual
    public static String getApiKey() {
        return apiKeyManager.getCurrentKey();
    }
    // Método para cambiar a la siguiente clave API
    public static void switchApiKey() {
        apiKeyManager.switchToNextKey();
    }
    // Método para obtener el host de la API
    public static String getApiHost() {
        return API_HOST;
    }
}
