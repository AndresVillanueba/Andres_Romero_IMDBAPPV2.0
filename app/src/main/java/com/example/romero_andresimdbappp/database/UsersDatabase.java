package com.example.romero_andresimdbappp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.romero_andresimdbappp.utils.KeyStoreManager; // <-- IMPORTANTE
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

        // 1) Ciframos 'address' y 'phone' antes de guardarlos en la BD local
        String encryptedAddress = KeyStoreManager.encryptData(address);
        String encryptedPhone = KeyStoreManager.encryptData(phone);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        // Guardamos cifrado en la base de datos
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, encryptedAddress);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, encryptedPhone);

        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.insertWithOnConflict(FavoritesDatabaseHelper.TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();

        // 2) Sincroniza en Firestore
        // Si quieres guardar en Firestore también cifrado, usa encryptedAddress y encryptedPhone.
        // Si prefieres verlo en claro en Firestore, usa address y phone.
        syncUserToFirestore(userId, name, email, encryptedAddress, encryptedPhone, image);
    }

    // Actualiza la información de un usuario
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime,
                           String address, String phone, String image) {

        // Obtenemos los datos actuales (descifrados) para no perder valores si alguno viene nulo
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
            // currentData.get(...) aquí ya está descifrado, porque getUser los descifra
            address = currentData.get(FavoritesDatabaseHelper.COLUMN_ADDRESS);
        }
        if (phone == null) {
            phone = currentData.get(FavoritesDatabaseHelper.COLUMN_PHONE);
        }

        // 1) Ciframos 'address' y 'phone' antes de guardarlos de nuevo
        String encryptedAddress = KeyStoreManager.encryptData(address);
        String encryptedPhone = KeyStoreManager.encryptData(phone);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        // Guardamos cifrados
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, encryptedAddress);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, encryptedPhone);

        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();

        // 2) Sincroniza con Firestore (decide si encriptado o en claro)
        syncUserToFirestore(userId, name, email, encryptedAddress, encryptedPhone, image);
    }

    // Recupera los datos de un usuario mediante su user_id, DESCIFRANDO 'address' y 'phone'
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
                // Obtenemos las columnas
                String encAddress = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ADDRESS));
                String encPhone   = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_PHONE));

                // Desciframos
                String decAddress = KeyStoreManager.decryptData(encAddress);
                String decPhone   = KeyStoreManager.decryptData(encPhone);

                userData.put(FavoritesDatabaseHelper.COLUMN_USER_ID,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID)));
                userData.put(FavoritesDatabaseHelper.COLUMN_NAME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_NAME)));
                userData.put(FavoritesDatabaseHelper.COLUMN_EMAIL,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_EMAIL)));
                userData.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME)));
                userData.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME)));

                // Guardamos en el Map la versión DESCIFRADA
                userData.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, decAddress != null ? decAddress : "");
                userData.put(FavoritesDatabaseHelper.COLUMN_PHONE, decPhone != null ? decPhone : "");

                userData.put(FavoritesDatabaseHelper.COLUMN_IMAGE,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE)));
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
        userData.put("address", address); // Cifrado o plano, según prefieras
        userData.put("phone", phone);     // Cifrado o plano, según prefieras
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
                String encAddress = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ADDRESS));
                String encPhone   = cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_PHONE));
                String decAddress = KeyStoreManager.decryptData(encAddress);
                String decPhone   = KeyStoreManager.decryptData(encPhone);

                Map<String, String> user = new HashMap<>();
                user.put(FavoritesDatabaseHelper.COLUMN_USER_ID,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_USER_ID)));
                user.put(FavoritesDatabaseHelper.COLUMN_NAME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_NAME)));
                user.put(FavoritesDatabaseHelper.COLUMN_EMAIL,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_EMAIL)));
                user.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME)));
                user.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME)));

                // Guardamos descifrado
                user.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, decAddress != null ? decAddress : "");
                user.put(FavoritesDatabaseHelper.COLUMN_PHONE, decPhone != null ? decPhone : "");

                user.put(FavoritesDatabaseHelper.COLUMN_IMAGE,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE)));

                userList.add(user);
            }
            cursor.close();
        }
        db.close();
        return userList;
    }
}
