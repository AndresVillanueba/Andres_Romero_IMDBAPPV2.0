package com.example.romero_andresimdbappp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.romero_andresimdbappp.utils.KeyStoreManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Clase que gestiona las operaciones CRUD sobre la tabla 'users' en la base de datos local.
//Ajustada para forzar "" en vez de null en campos como address, phone, etc.

public class UsersDatabase {
    private final FavoritesDatabaseHelper dbHelper;

    public UsersDatabase(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
    }

    // Añade un nuevo usuario y lo sincroniza con Firestore
    public void addUser(String userId, String name, String email,
                        String loginTime, String logoutTime,
                        String address, String phone, String image) {

        // Forzar que no sean null antes de cifrar
        if (name == null)      name      = "";
        if (email == null)     email     = "";
        if (loginTime == null) loginTime = "";
        if (logoutTime == null)logoutTime= "";
        if (address == null)   address   = "";
        if (phone == null)     phone     = "";
        if (image == null)     image     = "";

        // Cifrar address y phone
        String encryptedAddress = KeyStoreManager.encryptData(address);
        String encryptedPhone   = KeyStoreManager.encryptData(phone);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, encryptedAddress);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, encryptedPhone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        db.close();

        // Sincroniza con Firestore
        syncUserToFirestore(userId, name, email, encryptedAddress, encryptedPhone, image, loginTime, logoutTime);
    }

    // Actualiza la información de un usuario
    public void updateUser(String userId,
                           String name,
                           String email,
                           String loginTime,
                           String logoutTime,
                           String address,
                           String phone,
                           String image) {

        // Obtenemos los datos actuales descifrados
        Map<String, String> currentData = getUser(userId);
        if (currentData == null) {
            Log.e("UsersManager", "No se encontraron datos para el usuario: " + userId);
            return;
        }
        // Si vienen nulos, usamos lo que había antes
        if (name == null)      name      = currentData.get(FavoritesDatabaseHelper.COLUMN_NAME);
        if (email == null)     email     = currentData.get(FavoritesDatabaseHelper.COLUMN_EMAIL);
        if (loginTime == null) loginTime = currentData.get(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME);
        if (logoutTime == null)logoutTime= currentData.get(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME);
        if (address == null)   address   = currentData.get(FavoritesDatabaseHelper.COLUMN_ADDRESS);
        if (phone == null)     phone     = currentData.get(FavoritesDatabaseHelper.COLUMN_PHONE);
        if (image == null)     image     = currentData.get(FavoritesDatabaseHelper.COLUMN_IMAGE);

        // Asegurarnos de que no sean null
        if (name == null)      name      = "";
        if (email == null)     email     = "";
        if (loginTime == null) loginTime = "";
        if (logoutTime == null)logoutTime= "";
        if (address == null)   address   = "";
        if (phone == null)     phone     = "";
        if (image == null)     image     = "";

        // Cifrar address y phone nuevamente
        String encryptedAddress = KeyStoreManager.encryptData(address);
        String encryptedPhone   = KeyStoreManager.encryptData(phone);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, encryptedAddress);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, encryptedPhone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.update(FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();

        // Sincronizar con Firestore
        syncUserToFirestore(userId, name, email, encryptedAddress, encryptedPhone, image, loginTime, logoutTime);
    }

    // Recupera los datos de un usuario (descifrando address y phone)
    public Map<String, String> getUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, String> userData = null;

        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                userData = new HashMap<>();
                String encAddress = cursor.getString(
                        cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ADDRESS));
                String encPhone = cursor.getString(
                        cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_PHONE));

                // Descifrar
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

                // Si descifrar devolvió null, ponemos "" para evitar nulos
                userData.put(FavoritesDatabaseHelper.COLUMN_ADDRESS,
                        decAddress != null ? decAddress : "");
                userData.put(FavoritesDatabaseHelper.COLUMN_PHONE,
                        decPhone != null ? decPhone : "");

                userData.put(FavoritesDatabaseHelper.COLUMN_IMAGE,
                        cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE)));
            }
            cursor.close();
        }
        db.close();
        return userData;
    }

    // Verifica si un usuario existe en la tabla
    public boolean userExists(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                new String[]{FavoritesDatabaseHelper.COLUMN_USER_ID},
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null
        );

        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    // Actualiza solo el login_time localmente
    public void updateLoginTime(String userId, String loginTime) {
        if (loginTime == null) loginTime = "";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
    }

    // Actualiza solo el logout_time localmente
    public void updateLogoutTime(String userId, String logoutTime) {
        if (logoutTime == null) logoutTime = "";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
    }

    // Sincroniza los datos del usuario a Firestore
    private void syncUserToFirestore(String userId,
                                     String name,
                                     String email,
                                     String address,
                                     String phone,
                                     String image,
                                     String loginTime,
                                     String logoutTime) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // address y phone vienen encriptados, pero no necesitas guardarlos en Firestore encriptados
        // Si prefieres guardarlos sin encriptar, deberías descifrarlos antes. Aquí se asume que
        // se guardan en Firestore “tal cual” se guardó en la DB local.
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name != null ? name : "");
        userData.put("email", email != null ? email : "");
        userData.put("address", address != null ? address : "");
        userData.put("phone", phone != null ? phone : "");
        userData.put("image", image != null ? image : "");
        userData.put("login_time", loginTime != null ? loginTime : "");
        userData.put("logout_time", logoutTime != null ? logoutTime : "");

        firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("UsersManager", "Usuario sincronizado con Firestore"))
                .addOnFailureListener(e ->
                        Log.e("UsersManager", "Error al sincronizar usuario en Firestore", e));
    }

    // Para listar todos los usuarios (ejemplo)
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
                String encAddress = cursor.getString(
                        cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ADDRESS));
                String encPhone = cursor.getString(
                        cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_PHONE));

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
                user.put(FavoritesDatabaseHelper.COLUMN_ADDRESS,
                        decAddress != null ? decAddress : "");
                user.put(FavoritesDatabaseHelper.COLUMN_PHONE,
                        decPhone != null ? decPhone : "");
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
