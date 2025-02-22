package com.example.romero_andresimdbappp.database;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Clase que gestiona las operaciones CRUD sobre la tabla "users" en la base de datos.
 */
public class UsersDatabase {
    private final FavoritesDatabaseHelper dbHelper;
    public UsersDatabase(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
    }
    // Añade un nuevo usuario y lo sincroniza con Firestore
    public void addUser(String userId, String name, String email, String loginTime, String logoutTime,
                        String address, String phone, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.insertWithOnConflict(FavoritesDatabaseHelper.TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();

        // Sincroniza en Firestore
        syncUserToFirestore(userId, name, email, address, phone, image);
    }

    // Actualiza la información de un usuario
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime,
                           String address, String phone, String image) {
        Map<String, String> currentData = getUser(userId);
        if (currentData == null) {
            Log.e("UsersManager", "No se encontraron datos para el usuario: " + userId);
            return;
        }
        if (loginTime == null) {
            loginTime = currentData.get(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME);
        }
        if (logoutTime == null) {
            logoutTime = currentData.get(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME);
        }
        if (address == null) {
            address = currentData.get(FavoritesDatabaseHelper.COLUMN_ADDRESS);
        }
        if (phone == null) {
            phone = currentData.get(FavoritesDatabaseHelper.COLUMN_PHONE);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();

        syncUserToFirestore(userId, name, email, address, phone, image);
    }

    // Recupera los datos de un usuario mediante su user_id
    public Map<String, String> getUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, String> userData = new HashMap<>();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String[] columns = {
                        FavoritesDatabaseHelper.COLUMN_USER_ID,
                        FavoritesDatabaseHelper.COLUMN_NAME,
                        FavoritesDatabaseHelper.COLUMN_EMAIL,
                        FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                        FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                        FavoritesDatabaseHelper.COLUMN_ADDRESS,
                        FavoritesDatabaseHelper.COLUMN_PHONE,
                        FavoritesDatabaseHelper.COLUMN_IMAGE
                };
                for (String column : columns) {
                    int index = cursor.getColumnIndexOrThrow(column);
                    userData.put(column, cursor.getString(index));
                }
            } else {
                Log.e("UsersManager", "No se encontraron datos para el usuario con ID: " + userId);
            }
            cursor.close();
        }
        db.close();
        return userData;
    }

    // Verifica si un usuario existe
    public boolean userExists(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                new String[]{FavoritesDatabaseHelper.COLUMN_USER_ID},
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    // Actualiza el login_time
    public void updateLoginTime(String userId, String loginTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
    }

    // Actualiza el logout_time
    public void updateLogoutTime(String userId, String logoutTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
    }

    // Sincroniza los datos del usuario a Firestore
    private void syncUserToFirestore(String userId, String name, String email, String address, String phone, String image) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("address", address);
        userData.put("phone", phone);
        userData.put("image", image);

        firestore.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("UsersManager", "Usuario sincronizado con Firestore"))
                .addOnFailureListener(e -> Log.e("UsersManager", "Error al sincronizar usuario en Firestore", e));
    }

    // Recupera la lista de todos los usuarios (opcional)
    public List<Map<String, String>> getAllUsers() {
        List<Map<String, String>> userList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                null,
                null,
                null,
                null,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Map<String, String> user = new HashMap<>();
                String[] columns = {
                        FavoritesDatabaseHelper.COLUMN_USER_ID,
                        FavoritesDatabaseHelper.COLUMN_NAME,
                        FavoritesDatabaseHelper.COLUMN_EMAIL,
                        FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                        FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                        FavoritesDatabaseHelper.COLUMN_ADDRESS,
                        FavoritesDatabaseHelper.COLUMN_PHONE,
                        FavoritesDatabaseHelper.COLUMN_IMAGE
                };
                for (String column : columns) {
                    user.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
                }
                userList.add(user);
            }
            cursor.close();
        }
        db.close();
        return userList;
    }
}
