package com.example.romero_andresimdbappp.models;

import android.os.Parcel;
import android.os.Parcelable;

//Clase que representa una película con atributos
public class Movie implements Parcelable {
    // Atributos de la película
    private String id;
    private String title;
    private String imageUrl;
    private String releaseYear;
    private String rating;
    private String overview;
    private String genreId;
    // Constructor vacío
    public Movie() {
    }
    // Constructor utilizado para Parcelable
    protected Movie(Parcel in) {
        id = in.readString();
        title = in.readString();
        imageUrl = in.readString();
        releaseYear = in.readString();
        rating = in.readString();
        overview = in.readString();
        genreId = in.readString();
    }

    // Implementación del Parcelable
    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(imageUrl);
        dest.writeString(releaseYear);
        dest.writeString(rating);
        dest.writeString(overview);
        dest.writeString(genreId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Métodos getter y setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }
}
