package com.example.romero_andresimdbappp.utils;

import android.app.Application;
import android.content.Context;

import com.example.romero_andresimdbappp.R;
import com.example.romero_andresimdbappp.database.FavoritesDatabaseHelper;
import com.google.android.libraries.places.api.Places;

public class MyApp extends Application {
    // Instancia única del helper
    private static FavoritesDatabaseHelper dbHelper;
    private static MyApp instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Inicializa Places usando la API key definida en strings.xml
        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        // Crea la instancia única del helper al iniciar la app
        dbHelper = new FavoritesDatabaseHelper(this);
        // Registra el AppLifecycleManager para recibir callbacks del ciclo de vida de las Activities
        AppLifecycleManager lifecycleManager = new AppLifecycleManager(this);
        registerActivityLifecycleCallbacks(lifecycleManager);
        registerComponentCallbacks(lifecycleManager);
    }

    // Método para obtener la instancia del helper desde cualquier parte de la app
    public static FavoritesDatabaseHelper getDbHelper() {
        return dbHelper;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Cierra la base de datos cuando se termina la aplicación
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
