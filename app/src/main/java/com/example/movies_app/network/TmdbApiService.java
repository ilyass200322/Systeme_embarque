package com.example.movies_app.network;

import com.example.movies_app.model.Movie;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {
    @GET("movie/popular")
    Call<TmdbResponse> getPopularMovies(@Query("api_key") String apiKey, @Query("page") int page);

    @GET("movie/{movie_id}")
    Call<Movie> getMovieDetails(@Path("movie_id") int movieId, @Query("api_key") String apiKey);

    @GET("movie/{movie_id}/videos")
    Call<VideoResponse> getMovieVideos(@Path("movie_id") int movieId, @Query("api_key") String apiKey);
}
