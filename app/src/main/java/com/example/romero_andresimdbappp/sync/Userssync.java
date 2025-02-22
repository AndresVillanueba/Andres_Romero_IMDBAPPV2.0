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

    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;
    private final SimpleDateFormat dateFormat;

    public Userssync() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public void syncBasicUserToFirestore(String userId, String name, String email, String address, String phone, String image) {
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
                .addOnSuccessListener(aVoid -> Log.d("Userssync", "Usuario sincronizado con Firestore."))
                .addOnFailureListener(e -> Log.e("Userssync", "Error al sincronizar usuario: " + e.getMessage()));
    }

    public void syncCurrentUserToFirestore() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        String name = currentUser.getDisplayName();
        String address = currentUser.getTenantId();
        String phone = currentUser.getPhoneNumber();
        Uri image = currentUser.getPhotoUrl();
        if (email == null || name == null) return;
        String loginTime = dateFormat.format(new Date());

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> activityLog = new ArrayList<>();
            if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                for (Map<String, Object> entry : activityLog) {
                    if (entry.get("login_time").equals(loginTime)) return;
                }
            }
            Map<String, Object> newLog = new HashMap<>();
            newLog.put("login_time", loginTime);
            newLog.put("logout_time", null);
            activityLog.add(newLog);

            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", userId);
            userData.put("name", name);
            userData.put("email", email);
            userData.put("activity_log", activityLog);
            userData.put("address", address);
            userData.put("phone", phone);
            userData.put("image", image);

            db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> Log.d("Userssync", "Usuario sincronizado."))
                    .addOnFailureListener(e -> Log.e("Userssync", "Error al sincronizar usuario: " + e.getMessage()));
        }).addOnFailureListener(e -> Log.e("Userssync", "Error al recuperar datos: " + e.getMessage()));
    }

    public void updateLogoutTime() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        String logoutTime = dateFormat.format(new Date());

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                List<Map<String, Object>> activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
                if (activityLog != null && !activityLog.isEmpty()) {
                    Map<String, Object> lastLogin = activityLog.get(activityLog.size() - 1);
                    if (lastLogin.get("logout_time") == null) {
                        lastLogin.put("logout_time", logoutTime);
                        db.collection("users").document(userId)
                                .update("activity_log", activityLog)
                                .addOnSuccessListener(aVoid -> Log.d("Userssync", "Logout registrado."))
                                .addOnFailureListener(e -> Log.e("Userssync", "Error al registrar logout: " + e.getMessage()));
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e("Userssync", "Error al obtener historial: " + e.getMessage()));
    }

    public void syncUsersWithFirestore(UsersDatabase usersManager) {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        Map<String, String> localUser = usersManager.getUser(userId);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
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
                    if ("Usuario".equals(name) && localUser.containsKey(FavoritesDatabaseHelper.COLUMN_NAME)) {
                        name = localUser.get(FavoritesDatabaseHelper.COLUMN_NAME);
                    }
                    String email = documentSnapshot.exists() ? documentSnapshot.getString("email") : currentUser.getEmail();
                    String address = documentSnapshot.exists() ? documentSnapshot.getString("address")
                            : (localUser.containsKey(FavoritesDatabaseHelper.COLUMN_ADDRESS) ? localUser.get(FavoritesDatabaseHelper.COLUMN_ADDRESS) : "");
                    String phone = documentSnapshot.exists() ? documentSnapshot.getString("phone")
                            : (localUser.containsKey(FavoritesDatabaseHelper.COLUMN_PHONE) ? localUser.get(FavoritesDatabaseHelper.COLUMN_PHONE) : "");
                    String image = documentSnapshot.exists() ? documentSnapshot.getString("image") : "";
                    if (image == null || image.isEmpty()) {
                        if (currentUser.getPhotoUrl() != null) {
                            image = localUser.containsKey(FavoritesDatabaseHelper.COLUMN_IMAGE)
                                    ? localUser.get(FavoritesDatabaseHelper.COLUMN_IMAGE)
                                    : currentUser.getPhotoUrl().toString();
                        }
                    }

                    usersManager.addUser(userId, name, email, null, null, address, phone, image);
                    syncBasicUserToFirestore(userId, name, email, address, phone, image);
                })
                .addOnFailureListener(e -> Log.e("Userssync", "Error sincronizando Firestore: " + e.getMessage()));
    }
}
