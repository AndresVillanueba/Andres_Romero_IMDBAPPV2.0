package com.example.romero_andresimdbappp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.romero_andresimdbappp.models.Movie;
import java.util.ArrayList;
import java.util.List;

// Maneja la base de datos de películas favoritas
public class FavoritesManager {
    private final FavoritesDatabaseHelper dbHelper;

    public FavoritesManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
    }
    // Agrega una película a favoritos si no está duplicada
    public void addFavorite(Movie movie, String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_ID, movie.getId());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL, userEmail);
        values.put(FavoritesDatabaseHelper.COLUMN_TITLE, movie.getTitle());
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE_URL, movie.getImageUrl());
        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, movie.getReleaseYear());
        values.put(FavoritesDatabaseHelper.COLUMN_RATING, movie.getRating());

        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE // Evita duplicados
        );
        db.close();
    }
    // Elimina una película de favoritos
    public void removeFavorite(String movieId, String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                FavoritesDatabaseHelper.COLUMN_ID + "=? AND " +
                        FavoritesDatabaseHelper.COLUMN_USER_EMAIL + "=?",
                new String[]{movieId, userEmail}
        );
        db.close();
    }
    // Obtiene todas las películas favoritas del usuario
    public List<Movie> getFavorites(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Movie> favoriteMovies = new ArrayList<>();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_EMAIL + "=?",
                new String[]{userEmail},
                null,
                null,
                null
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Movie movie = new Movie();
                movie.setId(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ID)));
                movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_TITLE)));
                movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE_URL)));
                movie.setReleaseYear(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE)));
                movie.setRating(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RATING)));
                favoriteMovies.add(movie);
            }
            cursor.close();
        }

        db.close();
        return favoriteMovies;
    }
}
