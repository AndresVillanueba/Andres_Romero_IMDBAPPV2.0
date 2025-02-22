package com.example.romero_andresimdbappp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.romero_andresimdbappp.models.Movie;
import com.example.romero_andresimdbappp.sync.Favoritassync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {

    private final FavoritesDatabaseHelper dbHelper;
    private final Favoritassync firebaseSync;

    public FavoritesManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
        firebaseSync = new Favoritassync();
        // Sincroniza inicialmente los favoritos locales con Firestore
        firebaseSync.syncFavoritesWithLocalDatabase(this);
    }

    /**
     * Agrega una película a la lista de favoritos del usuario autenticado.
     * Se utiliza el UID del usuario de Firebase como FK en la tabla favorites.
     */
    public void addFavorite(Movie movie, String email) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return;
        }
        String userId = currentUser.getUid();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        // COLUMN_ID es el id de la película y COLUMN_FAV_USER_ID es la FK que referencia al usuario
        values.put(FavoritesDatabaseHelper.COLUMN_ID, movie.getId());
        values.put(FavoritesDatabaseHelper.COLUMN_FAV_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_TITLE, movie.getTitle());
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE_URL, movie.getImageUrl());
        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, movie.getReleaseYear());
        values.put(FavoritesDatabaseHelper.COLUMN_RATING, movie.getRating());

        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        db.close();

        firebaseSync.addFavoriteToFirestore(movie, userId);
    }

    /**
     * Elimina una película de la lista de favoritos del usuario autenticado.
     */
    public void removeFavorite(String movieId, String correoUsuario) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return;
        }
        String userId = currentUser.getUid();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                FavoritesDatabaseHelper.COLUMN_ID + "=? AND " + FavoritesDatabaseHelper.COLUMN_FAV_USER_ID + "=?",
                new String[]{movieId, userId}
        );
        db.close();

        if (deletedRows > 0) {
            Log.d("FavoritesManager", "Película eliminada localmente para el usuario: " + userId);
            firebaseSync.removeFavoriteFromFirestore(movieId, userId);
        } else {
            Log.d("FavoritesManager", "No se encontró la película en favoritos.");
        }
    }

    /**
     * Recupera la lista de películas favoritas del usuario autenticado.
     */
    public List<Movie> getFavorites(String userEmail) {
        List<Movie> favoriteMovies = new ArrayList<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return favoriteMovies;
        }
        String userId = currentUser.getUid();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                new String[]{
                        FavoritesDatabaseHelper.COLUMN_ID,
                        FavoritesDatabaseHelper.COLUMN_TITLE,
                        FavoritesDatabaseHelper.COLUMN_IMAGE_URL,
                        FavoritesDatabaseHelper.COLUMN_RELEASE_DATE,
                        FavoritesDatabaseHelper.COLUMN_RATING
                },
                FavoritesDatabaseHelper.COLUMN_FAV_USER_ID + "=?",
                new String[]{userId},
                null, null, null
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