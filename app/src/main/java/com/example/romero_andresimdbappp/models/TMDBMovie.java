package com.example.romero_andresimdbappp.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

//Modelo de datos TMDB API.
public class TMDBMovie {
    public static class GenresResponse {
        @SerializedName("genres")
        private List<Genre> genres;
        public List<Genre> getGenres() { return genres; }
    }

    public static class Genre {
        @SerializedName("id") private int id;
        @SerializedName("name") private String name;
        public int getId() {
            return id;
        }
        public String getName() {
            return name; }
    }

    public static class MovieSearchResponse {
        @SerializedName("results")
        private List<MovieResult> results;
        public List<MovieResult> getResults() {
            return results; }
    }

    public static class MovieResult {
        @SerializedName("id") private int id;
        @SerializedName("title") private String title;
        @SerializedName("poster_path") private String posterPath;
        @SerializedName("release_date") private String releaseDate;
        @SerializedName("vote_average") private double voteAverage;
        @SerializedName("overview") private String overview;

        public int getId() {
            return id; }
        public String getTitle() {
            return title; }
        public String getPosterPath() {
            return posterPath; }
        public String getReleaseDate() {
            return releaseDate; }
        public double getVoteAverage() {
            return voteAverage; }
        public String getOverview() {
            return overview; }
    }
}
