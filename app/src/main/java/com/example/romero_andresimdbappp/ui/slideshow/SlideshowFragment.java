package com.example.romero_andresimdbappp.ui.slideshow;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.romero_andresimdbappp.MovieListActivity;
import com.example.romero_andresimdbappp.R;
import com.example.romero_andresimdbappp.models.TMDBMovie;
import com.example.romero_andresimdbappp.api.TMDBApiService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Fragmento que permite a los usuarios buscar películas por género y año.
// Mostramos un spinner con géneros obtenidos desde la API de TMDB
// y un campo de texto para ingresar el año.
public class SlideshowFragment extends Fragment {
    private Spinner Generospinner;
    private EditText Año;
    private Button Btnbuscar;
    private List<TMDBMovie.Genre> Listageneros = new ArrayList<>();
    private final String API_KEY = "e9f84dbecca6c65f600d95bee2badcf5";
    @Override
    public View onCreateView(LayoutInflater inflador, ViewGroup contenedor, Bundle datosGuardados) {
        View vistaRaiz = inflador.inflate(R.layout.fragment_slideshow, contenedor, false);
        // Inicializa las vistas
        Generospinner = vistaRaiz.findViewById(R.id.genre_spinner);
        Año = vistaRaiz.findViewById(R.id.year_edit_text);
        Btnbuscar = vistaRaiz.findViewById(R.id.search_button);
        Cargargeneros();
        // Configurar el botón de búsqueda
        Btnbuscar.setOnClickListener(v -> {
            try {
                String generoSeleccionado = Generospinner.getSelectedItem().toString();
                String anio = Año.getText().toString().trim();
                if (generoSeleccionado.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, selecciona un género.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (anio.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, introduce un año.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int anioInt;
                try {
                    anioInt = Integer.parseInt(anio);
                    if (anioInt < 1940 || anioInt > Calendar.getInstance().get(Calendar.YEAR)) {
                        Toast.makeText(requireContext(), "Introduce un año entre 1940 y el año actual.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Introduce un año válido.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int idGenero = Obteneridgenero(generoSeleccionado);
                if (idGenero == -1) {
                    Toast.makeText(requireContext(), "No se encontró el género seleccionado.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Iniciar MovieListActivity con los parámetros seleccionados
                Intent intent = new Intent(requireContext(), MovieListActivity.class);
                intent.putExtra("GENRE_ID", idGenero);
                intent.putExtra("YEAR", anio);
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Ocurrió un error durante la búsqueda.", Toast.LENGTH_SHORT).show();
            }
        });

        return vistaRaiz;
    }
    private void Cargargeneros() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TMDBApiService apiService = retrofit.create(TMDBApiService.class);
        Call<TMDBMovie.GenresResponse> llamada = apiService.getGenres(API_KEY, "es-ES");
        llamada.enqueue(new Callback<TMDBMovie.GenresResponse>() {
            @Override
            public void onResponse(Call<TMDBMovie.GenresResponse> llamada, Response<TMDBMovie.GenresResponse> respuesta) {
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    Listageneros = respuesta.body().getGenres();
                    List<String> nombresGeneros = new ArrayList<>();
                    for (TMDBMovie.Genre genero : Listageneros) {
                        nombresGeneros.add(genero.getName());
                    }
                    ArrayAdapter<String> adaptador = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, nombresGeneros);
                    adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    Generospinner.setAdapter(adaptador);
                } else {
                    Toast.makeText(requireContext(), "No se pudieron cargar los géneros.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TMDBMovie.GenresResponse> llamada, Throwable t) {
                Toast.makeText(requireContext(), "Error al cargar géneros: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int Obteneridgenero(String nombreGenero) {
        for (TMDBMovie.Genre genero : Listageneros) {
            if (genero.getName().equalsIgnoreCase(nombreGenero)) {
                return genero.getId();
            }
        }
        return -1; // Devuelve -1 si no se encuentra el género
    }
}
