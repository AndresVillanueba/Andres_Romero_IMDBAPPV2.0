package com.example.romero_andresimdbappp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.romero_andresimdbappp.MovieDetailsActivity;
import com.example.romero_andresimdbappp.R;
import com.example.romero_andresimdbappp.database.FavoritesManager;
import com.example.romero_andresimdbappp.models.Movie;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.VistaPeliculas> {

    private List<Movie> listaPeliculas;
    private Context contexto;
    private boolean modoFavoritos;

    public MovieAdapter(List<Movie> listaPeliculas, boolean modoFavoritos) {
        this.listaPeliculas = listaPeliculas;
        this.modoFavoritos = modoFavoritos;
    }

    @Override
    public VistaPeliculas onCreateViewHolder(ViewGroup padre, int tipoVista) {
        contexto = padre.getContext();
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_movie, padre, false);
        return new VistaPeliculas(vista);
    }

    @Override
    public void onBindViewHolder(VistaPeliculas holder, int posicion) {
        Movie pelicula = listaPeliculas.get(posicion);

        // Cargar imagen con Glide
        Glide.with(holder.itemView.getContext())
                .load(pelicula.getImageUrl())
                .into(holder.imagenPelicula);

        // Clic corto: abrir detalles
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(contexto, MovieDetailsActivity.class);
            intent.putExtra("MOVIE_DATA", pelicula);
            contexto.startActivity(intent);
        });

        // Clic largo: agregar o eliminar de favoritos
        holder.itemView.setOnLongClickListener(v -> {
            FavoritesManager gestorFavoritos = new FavoritesManager(contexto);
            String correoUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            if (modoFavoritos) {
                // Eliminar de favoritos
                gestorFavoritos.removeFavorite(pelicula.getId(), correoUsuario);
                listaPeliculas.remove(posicion);
                notifyItemRemoved(posicion);
                notifyItemRangeChanged(posicion, listaPeliculas.size());
                Toast.makeText(contexto,
                        "Eliminada de favoritos: " + pelicula.getTitle(),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Agregar a favoritos (solo llamamos a addFavorite; no mostramos Toast aquí)
                gestorFavoritos.addFavorite(pelicula, correoUsuario, contexto);
                // QUITAMOS el Toast de "Agregada..." aquí para evitar mensaje doble
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaPeliculas.size();
    }

    public static class VistaPeliculas extends RecyclerView.ViewHolder {
        ImageView imagenPelicula;

        public VistaPeliculas(View itemView) {
            super(itemView);
            imagenPelicula = itemView.findViewById(R.id.movie_image);
        }
    }
}
