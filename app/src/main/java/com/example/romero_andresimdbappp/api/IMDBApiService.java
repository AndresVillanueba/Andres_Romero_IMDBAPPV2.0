package com.example.romero_andresimdbappp.api;
import com.example.romero_andresimdbappp.models.MovieOverviewResponse;
import com.example.romero_andresimdbappp.models.PopularMoviesResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
public interface IMDBApiService {
    //Obtenemos una lista de películas más populares desde la API.
    @GET("title/get-top-meter")
    Call<PopularMoviesResponse> getTopMeter(
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String host,
            @Query("topMeterTitlesType") String type
    );
    //Obtenemos información detallada de una película específica desde la API.
    @GET("title/get-overview")
    Call<MovieOverviewResponse> getOverview(
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String host,
            @Query("tconst") String tconst
    );
}
