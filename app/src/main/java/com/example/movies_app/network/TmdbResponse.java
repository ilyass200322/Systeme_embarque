package com.example.movies_app.network;

import com.example.movies_app.model.Movie;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbResponse {
    @SerializedName("results")
    private List<Movie> results;

    @SerializedName("page")
    private int page;

    @SerializedName("total_pages")
    private int totalPages;

    public List<Movie> getResults() { return results; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
}
