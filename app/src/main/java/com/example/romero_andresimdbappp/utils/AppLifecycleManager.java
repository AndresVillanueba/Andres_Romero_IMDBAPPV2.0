package com.example.romero_andresimdbappp.utils;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    // Preferencias y tiempos
    private static final String PREF_NAME = "AppPrefs";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final long LOGOUT_DELAY = 3000;
    // Flags y contadores
    private boolean hasLoggedOut = false;
    private boolean hasLoggedIn = false;
    private boolean isActivityChangingConfigurations = false;
    private int activityReferences = 0;
    private boolean isAppClosed = false;
    // Handler y runnable para logout diferido
    private final Handler logoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable logoutRunnable = this::handleLogout;
    // Formato de fecha/hora
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final Context context;

    public AppLifecycleManager(Context context) {
        this.context = context;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Chequea si había un logout pendiente
        if (!activity.isChangingConfigurations()) {
            checkForPendingLogout();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Cancela cualquier logout programado
        logoutHandler.removeCallbacks(logoutRunnable);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Registra login si no se había hecho
        if (user != null && !hasLoggedIn) {
            logUserLogin();
        }
        hasLoggedIn = true;
        hasLoggedOut = false;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Programa el logout si la app queda en segundo plano
        logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // Incrementa contador de Activities en foreground
        if (!isActivityChangingConfigurations) {
            activityReferences++;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // Decrementa contador y si ya no hay Activities, programa logout
        if (!isActivityChangingConfigurations) {
            activityReferences--;
            if (activityReferences == 0 && !isAppClosed) {
                isAppClosed = true;
                logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // Verifica si se destruye por cambio de config
        isActivityChangingConfigurations = activity.isChangingConfigurations();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override
    public void onConfigurationChanged(Configuration newConfig) {}
    @Override
    public void onLowMemory() {}

    @Override
    public void onTrimMemory(int level) {
        // Logout si la UI se oculta
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user);
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putBoolean(PREF_IS_LOGGED_IN, false).apply();
        }
    }

    private void handleLogout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !hasLoggedOut) {
            registerUserLogout(user);
        }
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(PREF_IS_LOGGED_IN, false).apply();
        hasLoggedOut = true;
        hasLoggedIn = false;
    }

    private void logUserLogin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String loginTime = dateFormat.format(new Date());
            Map<String, Object> loginEntry = new HashMap<>();
            loginEntry.put("login_time", loginTime);
            loginEntry.put("logout_time", null);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("activity_log", FieldValue.arrayUnion(loginEntry))
                    .addOnSuccessListener(aVoid -> {
                        new UsersDatabase(context).updateLoginTime(user.getUid(), loginTime);
                        setLoggedInPreference(true);
                    });
        }
    }

    private void registerUserLogout(FirebaseUser user) {
        String logoutTime = dateFormat.format(new Date());
        UsersDatabase usersDB = new UsersDatabase(context);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("activity_log")) {
                        List<Map<String, Object>> activityLog = (List<Map<String, Object>>) doc.get("activity_log");
                        if (activityLog != null && !activityLog.isEmpty()) {
                            Map<String, Object> lastLogin = activityLog.get(activityLog.size() - 1);
                            if (lastLogin.get("logout_time") == null) {
                                lastLogin.put("logout_time", logoutTime);
                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(user.getUid())
                                        .update("activity_log", activityLog)
                                        .addOnSuccessListener(aVoid -> {
                                            usersDB.updateLogoutTime(user.getUid(), logoutTime);
                                            setLoggedInPreference(false);
                                        });
                            }
                        }
                    }
                });
    }

    private void checkForPendingLogout() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasLoggedIn = sp.getBoolean(PREF_IS_LOGGED_IN, false);
        if (wasLoggedIn) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                registerUserLogout(currentUser);
            }
            sp.edit().putBoolean(PREF_IS_LOGGED_IN, false).apply();
        }
    }

    private void setLoggedInPreference(boolean loggedIn) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(PREF_IS_LOGGED_IN, loggedIn).apply();
    }
}
