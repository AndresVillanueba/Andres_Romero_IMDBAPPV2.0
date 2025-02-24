package com.example.romero_andresimdbappp.sync;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import com.example.romero_andresimdbappp.database.FavoritesManager;
import com.example.romero_andresimdbappp.models.Movie;

public class Favoritassync {

    // Instancia de Firestore
    private final FirebaseFirestore db;

    public Favoritassync() {
        db = FirebaseFirestore.getInstance();
    }

    // Añade una película favorita a Firestore
    public void addFavoriteToFirestore(Movie movie, String userId) {
        Map<String, Object> movieData = new HashMap<>();
        movieData.put("id", movie.getId());
        movieData.put("title", movie.getTitle());
        movieData.put("posterUrl", movie.getImageUrl());
        movieData.put("releaseDate", movie.getReleaseYear());
        movieData.put("rating", movie.getRating());
        db.collection("favorites")
                .document(userId)
                .collection("movies")
                .document(movie.getId())
                .set(movieData)
                .addOnSuccessListener(aVoid -> Log.d("Favoritassync", "Película agregada a Firestore."))
                .addOnFailureListener(e -> Log.e("Favoritassync", "Error al agregar película: " + e.getMessage()));
    }

    // Elimina una película favorita de Firestore
    public void removeFavoriteFromFirestore(String movieId, String userId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        db.collection("favorites")
                .document(uid)
                .collection("movies")
                .document(movieId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Favoritassync", "Película eliminada de Firestore."))
                .addOnFailureListener(e -> Log.e("Favoritassync", "Error al eliminar película: " + e.getMessage()));
    }

    // Descarga favoritos de Firestore y los guarda en la base local
    public void syncFavoritesWithLocalDatabase(FavoritesManager favoritesManager) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("Favoritassync", "No hay usuario autenticado.");
            return;
        }
        String userId = user.getUid();
        db.collection("favorites")
                .document(userId)
                .collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null) return;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Movie movie = new Movie();
                        movie.setId(doc.getString("id"));
                        movie.setTitle(doc.getString("title"));
                        movie.setImageUrl(doc.getString("posterUrl"));
                        movie.setReleaseYear(doc.getString("releaseDate"));
                        movie.setRating(doc.getString("rating"));
                        favoritesManager.addFavorite(movie, user.getEmail());
                    }
                })
                .addOnFailureListener(e -> Log.e("Favoritassync", "Error en sincronización: " + e.getMessage()));
    }
}
