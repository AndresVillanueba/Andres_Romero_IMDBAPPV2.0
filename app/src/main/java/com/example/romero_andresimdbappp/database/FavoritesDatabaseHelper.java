package com.example.romero_andresimdbappp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favoritas_db";
    private static final int DATABASE_VERSION = 8;

    // Tabla USERS
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LOGIN_TIME = "login_time";
    public static final String COLUMN_LOGOUT_TIME = "logout_time";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";

    // Tabla FAVORITES
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "id"; // id de la película
    public static final String COLUMN_FAV_USER_ID = "user_id"; // FK a users(user_id)
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_RELEASE_DATE = "release_date";
    public static final String COLUMN_RATING = "rating";

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_LOGIN_TIME + " TEXT, " +
                    COLUMN_LOGOUT_TIME + " TEXT, " +
                    COLUMN_ADDRESS + " TEXT, " +
                    COLUMN_PHONE + " TEXT, " +
                    COLUMN_IMAGE + " TEXT" +
                    ");";

    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " TEXT, " +
                    COLUMN_FAV_USER_ID + " TEXT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_RELEASE_DATE + " TEXT, " +
                    COLUMN_RATING + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_FAV_USER_ID + "), " +
                    "FOREIGN KEY (" + COLUMN_FAV_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE" +
                    ");";

    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
