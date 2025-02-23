package com.example.romero_andresimdbappp.utils;
import android.app.Application;

public class MyApp extends Application {
    private AppLifecycleManager lifecycleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        lifecycleManager = new AppLifecycleManager(this);
        registerActivityLifecycleCallbacks(lifecycleManager);
        registerComponentCallbacks(lifecycleManager);
    }
}
