package com.example.romero_andresimdbappp.ui.home;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.romero_andresimdbappp.R;
import com.example.romero_andresimdbappp.adapters.MovieAdapter;
import com.example.romero_andresimdbappp.api.IMDBApiService;
import com.example.romero_andresimdbappp.models.Movie;
import com.example.romero_andresimdbappp.models.PopularMoviesResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {
    private List<Movie> Movielist = new ArrayList<>();
    private RecyclerView Recyclerview;
    private MovieAdapter Movieadapter;
    private boolean Controlarvistas = false; // Variable para controlar las llamadas
    private static final String API_KEY = "74c895ec50msh5642581f6a63ff0p1dd44fjsn0b0b34f8152a";
    private static final String API_HOST = "imdb-com.p.rapidapi.com";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        Recyclerview = root.findViewById(R.id.recyclerView);
        Recyclerview.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columnas
        Movieadapter = new MovieAdapter(Movielist, false);
        Recyclerview.setAdapter(Movieadapter);
        // Llamar a fetchMovies() solo si la lista está vacía
        if (Movielist.isEmpty() && !Controlarvistas) {
            fetchMovies();
        }

        return root;
    }

    //Realizamos la llamada a la API de IMDB para obtener las películas más populares.
    private void fetchMovies() {
        Controlarvistas = true;
        IMDBApiService apiService = new Retrofit.Builder()
                .baseUrl("https://imdb-com.p.rapidapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IMDBApiService.class);
        Call<PopularMoviesResponse> call = apiService.getTopMeter(API_KEY, API_HOST, "ALL");
        call.enqueue(new Callback<PopularMoviesResponse>() {
            @Override
            public void onResponse(Call<PopularMoviesResponse> call, Response<PopularMoviesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PopularMoviesResponse.Edge> edges = response.body().getData().getTopMeterTitles().getEdges();
                    // Elegimos los primeros 10 elementos
                    int limit = Math.min(edges.size(), 10);
                    for (int i = 0; i < limit; i++) {
                        PopularMoviesResponse.Node node = edges.get(i).getNode();
                        // Extraer los datos de la película
                        String id = node.getId();
                        String title = node.getTitleText() != null ? node.getTitleText().getText() : "Título desconocido";
                        String imageUrl = node.getPrimaryImage() != null ? node.getPrimaryImage().getUrl() : null;
                        String releaseYear = node.getReleaseYear() != null ? String.valueOf(node.getReleaseYear().getYear()) : "Fecha desconocida";
                        // Crear un objeto Movie con los datos básicos
                        Movie movie = new Movie();
                        movie.setId(id);
                        movie.setTitle(title);
                        movie.setImageUrl(imageUrl);
                        movie.setReleaseYear(releaseYear);
                        // Añadir la película a la lista
                        Movielist.add(movie);
                    }

                    // Notificar al adaptador para actualizar el RecyclerView
                    Movieadapter.notifyDataSetChanged();
                } else {
                    Controlarvistas = false; // Permitir intentar de nuevo si falla
                }
            }

            @Override
            public void onFailure(Call<PopularMoviesResponse> call, Throwable t) {
                Controlarvistas = false; // Permitir intentar de nuevo si falla
            }
        });
    }
}
