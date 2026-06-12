package com.example.movies_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.movies_app.BuildConfig;
import com.example.movies_app.model.Movie;
import com.example.movies_app.network.RetrofitClient;
import com.example.movies_app.network.TmdbResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieViewModel extends ViewModel {
    private final MutableLiveData<List<Movie>> movies = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private int currentPage = 1;
    private boolean isLastPage = false;

    public LiveData<List<Movie>> getMovies() {
        return movies;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchPopularMovies(int page) {
        if (isLoading.getValue() != null && isLoading.getValue()) return;
        if (isLastPage && page > 1) return;

        isLoading.setValue(true);
        RetrofitClient.getApiService().getPopularMovies(BuildConfig.TMDB_API_KEY, page)
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(Call<TmdbResponse> call, Response<TmdbResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> currentMovies = movies.getValue();
                            List<Movie> newMovies = response.body().getResults();
                            
                            if (page == 1) {
                                movies.setValue(newMovies);
                            } else if (currentMovies != null && newMovies != null) {
                                currentMovies.addAll(newMovies);
                                movies.setValue(currentMovies);
                            }
                            
                            currentPage = page;
                            if (newMovies == null || newMovies.isEmpty()) {
                                isLastPage = true;
                            }
                        } else {
                            errorMessage.setValue("Erreur lors de la récupération des films");
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Erreur réseau : " + t.getMessage());
                    }
                });
    }

    public void loadNextPage() {
        fetchPopularMovies(currentPage + 1);
    }
}
