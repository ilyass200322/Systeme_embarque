package com.example.movies_app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Movie {
    @SerializedName("id")
    private int id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("release_date")
    private String releaseDate;
    
    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;
    
    @SerializedName("overview")
    private String overview;

    @SerializedName("genre_ids")
    private List<Integer> genreIds;

    @SerializedName("vote_average")
    private double voteAverage;

    public Movie() {} // Required for Firestore toObject

    public Movie(int id, String title, String posterPath, String overview) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.overview = overview;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getReleaseDate() { return releaseDate; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public String getOverview() { return overview; }
    public List<Integer> getGenreIds() { return genreIds; }
    public double getVoteAverage() { return voteAverage; }
    
    public String getFullPosterPath() {
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    public String getFullBackdropPath() {
        return "https://image.tmdb.org/t/p/w780" + backdropPath;
    }
}
