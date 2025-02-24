package com.example.romero_andresimdbappp.utils;

import android.app.Application;
import com.example.romero_andresimdbappp.R;
import com.google.android.libraries.places.api.Places;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Inicializa Places usando la API key definida en strings.xml
        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        // Registra el AppLifecycleManager para recibir callbacks del ciclo de vida de las Activities
        AppLifecycleManager lifecycleManager = new AppLifecycleManager(this);
        registerActivityLifecycleCallbacks(lifecycleManager);
        registerComponentCallbacks(lifecycleManager);
    }
}
