package com.example.romero_andresimdbappp.sync;

import android.net.Uri;
import android.util.Log;
import com.example.romero_andresimdbappp.database.FavoritesDatabaseHelper;
import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Userssync {
    // Instancias de Firestore y usuario actual
    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;
    private final SimpleDateFormat dateFormat;

    public Userssync() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    //Sincroniza datos básicos del usuario con Firestore.
     //Ajustado para evitar valores null, convirtiéndolos a "".

    public void syncBasicUserToFirestore(String userId, String name, String email,
                                         String address, String phone, String image) {
        // Forzar que no sean null
        name    = (name    != null) ? name    : "";
        // Si no hay email real, asignamos un placeholder
        email   = (email   != null && !email.isEmpty()) ? email : (userId + "@placeholder.com");
        address = (address != null) ? address : "";
        phone   = (phone   != null) ? phone   : "";
        image   = (image   != null) ? image   : "";

        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("address", address);
        userData.put("phone", phone);
        userData.put("image", image);

        db.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Userssync", "Usuario sincronizado con Firestore."))
                .addOnFailureListener(e ->
                        Log.e("Userssync", "Error al sincronizar usuario: " + e.getMessage()));
    }

    //Agrega un nuevo login a activity_log en Firestore.
    //Ajustado para convertir phone, address, image en "" cuando sean null.
    //Si el email es null, se asigna un placeholder.

    public void syncCurrentUserToFirestore() {
        if (currentUser == null) return;

        final String userId = currentUser.getUid();
        // Declaramos emailFinal para que sea final en la lambda
        final String emailFinal = (currentUser.getEmail() == null || currentUser.getEmail().isEmpty())
                ? userId + "@placeholder.com" : currentUser.getEmail();

        // Nombre provisional si el displayName es null
        String tmpName = currentUser.getDisplayName();
        if (tmpName == null || tmpName.isEmpty()) {
            tmpName = "Usuario_" + userId.substring(0, 5);
        }
        final String name = tmpName;

        // address, phone e image: forzamos a "" si son null
        final String address = currentUser.getTenantId() != null ? currentUser.getTenantId() : "";
        final String phone = currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "";
        final Uri imageUri = currentUser.getPhotoUrl();
        final String image = (imageUri != null) ? imageUri.toString() : "";

        final String loginTime = dateFormat.format(new Date());

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> activityLog = new ArrayList<>();
                    if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                        activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                        // Evitar duplicar login_time
                        for (Map<String, Object> entry : activityLog) {
                            if (entry.get("login_time").equals(loginTime)) {
                                return;
                            }
                        }
                    }

                    // Agregar registro de login
                    Map<String, Object> newLog = new HashMap<>();
                    newLog.put("login_time", loginTime);
                    newLog.put("logout_time", null);
                    activityLog.add(newLog);

                    // Armar datos de usuario
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("user_id", userId);
                    userData.put("name", name);
                    userData.put("email", emailFinal);
                    userData.put("activity_log", activityLog);
                    userData.put("address", address);
                    userData.put("phone", phone);
                    userData.put("image", image);

                    db.collection("users")
                            .document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid ->
                                    Log.d("Userssync", "Usuario sincronizado."))
                            .addOnFailureListener(e ->
                                    Log.e("Userssync", "Error al sincronizar usuario: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e("Userssync", "Error al recuperar datos: " + e.getMessage()));
    }

    //Registra logout_time en activity_log.
    public void updateLogoutTime() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        String logoutTime = dateFormat.format(new Date());

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                        List<Map<String, Object>> activityLog =
                                (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                        if (activityLog != null && !activityLog.isEmpty()) {
                            Map<String, Object> lastLogin = activityLog.get(activityLog.size() - 1);
                            if (lastLogin.get("logout_time") == null) {
                                lastLogin.put("logout_time", logoutTime);
                                db.collection("users")
                                        .document(userId)
                                        .update("activity_log", activityLog)
                                        .addOnSuccessListener(aVoid ->
                                                Log.d("Userssync", "Logout registrado."))
                                        .addOnFailureListener(e ->
                                                Log.e("Userssync", "Error al registrar logout: " + e.getMessage()));
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Userssync", "Error al obtener historial: " + e.getMessage()));
    }

    //Descarga usuario de Firestore y lo guarda en local.
     //También forzamos "" en lugar de null para address, phone, image.
     //Si el email es null, se usa placeholder.

    public void syncUsersWithFirestore(UsersDatabase usersManager) {
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        // Obtenemos datos locales para fallback
        Map<String, String> localUser = usersManager.getUser(userId);

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Nombre
                    String name = documentSnapshot.exists() ? documentSnapshot.getString("name") : null;
                    if (name == null || name.isEmpty()) {
                        String firebaseName = currentUser.getDisplayName();
                        if (firebaseName != null && !firebaseName.isEmpty()) {
                            name = firebaseName;
                        }
                    }
                    if (name == null || name.isEmpty()) {
                        name = localUser.containsKey(FavoritesDatabaseHelper.COLUMN_NAME)
                                ? localUser.get(FavoritesDatabaseHelper.COLUMN_NAME)
                                : "Usuario";
                    }

                    // Email
                    String email = documentSnapshot.exists() ? documentSnapshot.getString("email") : currentUser.getEmail();
                    if (email == null || email.isEmpty()) {
                        email = userId + "@placeholder.com";
                    }

                    // Address
                    String address = documentSnapshot.exists() ? documentSnapshot.getString("address") : "";
                    if (address == null || address.isEmpty()) {
                        address = localUser.containsKey(FavoritesDatabaseHelper.COLUMN_ADDRESS)
                                ? localUser.get(FavoritesDatabaseHelper.COLUMN_ADDRESS)
                                : "";
                    }

                    // Phone
                    String phone = documentSnapshot.exists() ? documentSnapshot.getString("phone") : "";
                    if (phone == null || phone.isEmpty()) {
                        phone = localUser.containsKey(FavoritesDatabaseHelper.COLUMN_PHONE)
                                ? localUser.get(FavoritesDatabaseHelper.COLUMN_PHONE)
                                : "";
                    }

                    // Image
                    String image = documentSnapshot.exists() ? documentSnapshot.getString("image") : "";
                    if (image == null || image.isEmpty()) {
                        if (currentUser.getPhotoUrl() != null) {
                            image = localUser.containsKey(FavoritesDatabaseHelper.COLUMN_IMAGE)
                                    ? localUser.get(FavoritesDatabaseHelper.COLUMN_IMAGE)
                                    : currentUser.getPhotoUrl().toString();
                        }
                    }

                    // Guardamos/actualizamos en BD local
                    usersManager.addUser(userId, name, email, null, null, address, phone, image);

                    // También aseguramos que Firestore no tenga campos nulos
                    syncBasicUserToFirestore(userId, name, email, address, phone, image);
                })
                .addOnFailureListener(e ->
                        Log.e("Userssync", "Error sincronizando Firestore: " + e.getMessage()));
    }
}
