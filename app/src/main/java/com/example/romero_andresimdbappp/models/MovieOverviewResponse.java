package com.example.romero_andresimdbappp.models;

import com.google.gson.annotations.SerializedName;

// Clase para manejar la respuesta de la API sobre detalles de una película
public class MovieOverviewResponse {

    @SerializedName("data")
    public Data data;

    // Devuelve los datos de la película
    public Data getData() {
        return data;
    }

    // Contiene la información del título de la película
    public static class Data {
        @SerializedName("title")
        public Title title;

        public Title getTitle() {
            return title;
        }
    }

    // Contiene el título, fecha de lanzamiento, calificación y sinopsis
    public static class Title {
        @SerializedName("titleText")
        public TitleText titleText;

        @SerializedName("releaseDate")
        public ReleaseDate releaseDate;

        @SerializedName("ratingsSummary")
        public RatingsSummary ratingsSummary;

        @SerializedName("plot")
        public Plot plot;

        public TitleText getTitleText() {
            return titleText;
        }

        public ReleaseDate getReleaseDate() {
            return releaseDate;
        }

        public RatingsSummary getRatingsSummary() {
            return ratingsSummary;
        }

        public Plot getPlot() {
            return plot;
        }
    }

    // Representa el texto del título de la película
    public static class TitleText {
        @SerializedName("text")
        public String text;

        public String getText() {
            return text;
        }
    }

    // Representa la fecha de lanzamiento
    public static class ReleaseDate {
        @SerializedName("day")
        public Integer day;

        @SerializedName("month")
        public Integer month;

        @SerializedName("year")
        public Integer year;

        public Integer getDay() {
            return day;
        }

        public Integer getMonth() {
            return month;
        }

        public Integer getYear() {
            return year;
        }
    }

    // Representa la calificación de la película
    public static class RatingsSummary {
        @SerializedName("aggregateRating")
        public Double aggregateRating;

        public Double getAggregateRating() {
            return aggregateRating;
        }
    }

    // Contiene la sinopsis de la película
    public static class Plot {
        @SerializedName("plotText")
        public PlotText plotText;

        public PlotText getPlotText() {
            return plotText;
        }
    }

    // Representa el texto de la sinopsis
    public static class PlotText {
        @SerializedName("plainText")
        public String plainText;

        public String getPlainText() {
            return plainText;
        }
    }
}
