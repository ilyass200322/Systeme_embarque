package com.example.movies_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.movies_app.database.AppDatabase;
import com.example.movies_app.database.MovieEntity;
import com.example.movies_app.databinding.ActivityMovieDetailBinding;
import com.example.movies_app.model.Movie;
import com.example.movies_app.network.RetrofitClient;
import com.example.movies_app.network.VideoResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMovieDetailBinding binding;
    private GoogleMap mMap;
    private Movie currentMovie;
    private boolean isFavorite = false;
    private String trailerKey;
    private YouTubePlayer youtubePlayerInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovieDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getLifecycle().addObserver(binding.youtubePlayerView);

        int movieId = getIntent().getIntExtra("movieId", -1);
        if (movieId != -1) {
            fetchMovieDetails(movieId);
            checkFavoriteStatus(movieId);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.inflateMenu(R.menu.movie_detail_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                shareMovie();
                return true;
            }
            return false;
        });

        binding.fabFavorite.setOnClickListener(v -> toggleFavorite());
        
        binding.btnPlayTrailer.setOnClickListener(v -> {
            if (trailerKey != null) {
                Log.d("TRAILER_DEBUG", "Opening trailer with key: " + trailerKey);
                // On essaie d'ouvrir directement l'application YouTube (plus fiable)
                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + trailerKey));
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + trailerKey));
                
                try {
                    startActivity(appIntent);
                } catch (Exception e) {
                    // Si l'app YouTube n'est pas installée, on utilise le navigateur
                    startActivity(webIntent);
                }
            } else {
                Toast.makeText(this, "Trailer not available yet", Toast.LENGTH_SHORT).show();
            }
        });

        binding.youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youtubePlayerInstance = youTubePlayer;
                if (trailerKey != null) {
                    youTubePlayer.cueVideo(trailerKey, 0);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void fetchMovieDetails(int movieId) {
        RetrofitClient.getApiService().getMovieDetails(movieId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<Movie>() {
                    @Override
                    public void onResponse(@NonNull Call<Movie> call, @NonNull Response<Movie> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentMovie = response.body();
                            updateUI();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Movie> call, @NonNull Throwable t) {
                        Toast.makeText(MovieDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });

        RetrofitClient.getApiService().getMovieVideos(movieId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<VideoResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VideoResponse> call, @NonNull Response<VideoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (VideoResponse.Video v : response.body().getResults()) {
                                if ("Trailer".equals(v.getType()) && "YouTube".equals(v.getSite())) {
                                    trailerKey = v.getKey();
                                    Log.d("TRAILER_DEBUG", "Trailer key found: " + trailerKey);
                                    binding.videoCard.setVisibility(View.VISIBLE);
                                    if (youtubePlayerInstance != null) {
                                        youtubePlayerInstance.cueVideo(trailerKey, 0);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<VideoResponse> call, @NonNull Throwable t) {}
                });
    }

    private void updateUI() {
        binding.textName.setText(currentMovie.getTitle());
        binding.textDetails.setText(currentMovie.getOverview());
        binding.textRating.setText(String.format(Locale.getDefault(), "★ %.1f / 10", currentMovie.getVoteAverage()));

        Glide.with(this)
                .load(currentMovie.getFullPosterPath())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imagePoster);

        Glide.with(this)
                .load(currentMovie.getFullBackdropPath())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageBackdrop);
    }

    private void checkFavoriteStatus(int movieId) {
        AppDatabase.getInstance(this).movieDao().isFavorite(movieId).observe(this, favorite -> {
            isFavorite = favorite != null && favorite;
            binding.fabFavorite.setImageResource(isFavorite ? 
                    android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            
            int color = isFavorite ? 
                    ContextCompat.getColor(this, R.color.gold) : 
                    ContextCompat.getColor(this, R.color.text_secondary);
            binding.fabFavorite.setImageTintList(ColorStateList.valueOf(color));
        });
    }

    private void toggleFavorite() {
        if (currentMovie == null) return;
        
        MovieEntity entity = new MovieEntity(
                currentMovie.getId(),
                currentMovie.getTitle(),
                currentMovie.getReleaseDate(),
                currentMovie.getPosterPath(),
                currentMovie.getOverview()
        );

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(MovieDetailActivity.this);
            if (isFavorite) {
                db.movieDao().deleteFavorite(entity);
            } else {
                db.movieDao().insertFavorite(entity);
            }
        }).start();
    }

    private void shareMovie() {
        if (currentMovie == null) return;
        String shareText = "Check out this movie: " + currentMovie.getTitle() + "\n" +
                "Rating: ★ " + currentMovie.getVoteAverage() + "/10\n" +
                "Overview: " + currentMovie.getOverview();
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng casa = new LatLng(33.596460, -7.615480);
        mMap.addMarker(new MarkerOptions().position(casa).title("Cinema"));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casa, 12));
    }
}
