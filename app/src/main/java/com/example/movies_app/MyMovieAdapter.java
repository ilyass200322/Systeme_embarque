package com.example.movies_app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.movies_app.database.AppDatabase;
import com.example.movies_app.database.MovieEntity;
import com.example.movies_app.databinding.ActivityMovieItemListBinding;
import com.example.movies_app.model.Movie;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyMovieAdapter extends RecyclerView.Adapter<MyMovieAdapter.ViewHolder>
        implements Filterable {

    private final List<Movie> originalMovies = new ArrayList<>();
    private List<Movie> filteredMovies = new ArrayList<>();
    private final Context context;
    private String currentSearchText = "";
    private int currentGenreId = -1; // -1 means All
    private boolean showOnlyFavories = false;
    private final Set<Integer> favoriteIds = new HashSet<>();

    public MyMovieAdapter(Context context) {
        this.context = context;
        updateFavorites();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ActivityMovieItemListBinding binding = ActivityMovieItemListBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie movie = filteredMovies.get(position);

        holder.binding.textName.setText(movie.getTitle());
        
        // Format date and rating
        String releaseYear = movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4 
                ? movie.getReleaseDate().substring(0, 4) : "N/A";
        holder.binding.textdate.setText(String.format("%s • ★ %.1f", releaseYear, movie.getVoteAverage()));
        
        holder.binding.textOverview.setText(movie.getOverview());

        Glide.with(context)
                .load(movie.getFullPosterPath())
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.binding.imageview);

        // Gérer l'état du bouton favori
        boolean favorite = favoriteIds.contains(movie.getId());
        holder.binding.btnFavoriteItem.setImageResource(
                favorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
        
        int color = favorite ? 
                ContextCompat.getColor(context, R.color.gold) : 
                ContextCompat.getColor(context, R.color.text_secondary);
        holder.binding.btnFavoriteItem.setImageTintList(ColorStateList.valueOf(color));

        holder.binding.btnFavoriteItem.setOnClickListener(v -> {
            toggleFavorite(movie);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getId());
            context.startActivity(intent);
        });
    }

    private void toggleFavorite(Movie movie) {
        MovieEntity entity = new MovieEntity(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getPosterPath(),
                movie.getOverview()
        );

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            boolean alreadyFavorite = favoriteIds.contains(movie.getId());
            if (alreadyFavorite) {
                db.movieDao().deleteFavorite(entity);
                favoriteIds.remove(movie.getId());
            } else {
                db.movieDao().insertFavorite(entity);
                favoriteIds.add(movie.getId());
            }
            ((AppCompatActivity) context).runOnUiThread(this::notifyDataSetChanged);
        }).start();
    }

    @Override
    public int getItemCount() {
        return filteredMovies.size();
    }

    public void setMovies(List<Movie> movies) {
        this.originalMovies.clear();
        this.originalMovies.addAll(movies);
        updateFavorites();
        applyFilters();
    }

    private void updateFavorites() {
        new Thread(() -> {
            List<Integer> ids = AppDatabase.getInstance(context).movieDao().getAllFavoriteIdsSync();
            if (ids != null) {
                ((AppCompatActivity) context).runOnUiThread(() -> {
                    favoriteIds.clear();
                    favoriteIds.addAll(ids);
                    notifyDataSetChanged();
                });
            }
        }).start();
    }

    private void applyFilters() {
        getFilter().filter(currentSearchText);
    }

    public void filterByCategory(int genreId) {
        this.currentGenreId = genreId;
        this.showOnlyFavories = false;
        getFilter().filter(currentSearchText);
    }

    public void filterByFavories() {
        this.showOnlyFavories = true;
        this.currentGenreId = -1;
        getFilter().filter(currentSearchText);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                currentSearchText = (constraint != null) ? constraint.toString().toLowerCase().trim() : "";
                List<Movie> list = new ArrayList<>();
                AppDatabase db = AppDatabase.getInstance(context);
                List<Integer> favoriteIds = db.movieDao().getAllFavoriteIdsSync();
                
                for (Movie m : originalMovies) {
                    boolean matchesText = currentSearchText.isEmpty() || m.getTitle().toLowerCase().contains(currentSearchText);
                    boolean matchesGenre = currentGenreId == -1 || (m.getGenreIds() != null && m.getGenreIds().contains(currentGenreId));
                    boolean matchesFavorite = !showOnlyFavories || (favoriteIds != null && favoriteIds.contains(m.getId()));
                    
                    if (matchesText && matchesGenre && matchesFavorite) {
                        list.add(m);
                    }
                }

                FilterResults r = new FilterResults();
                r.values = list;
                return r;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMovies = (List<Movie>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ActivityMovieItemListBinding binding;

        public ViewHolder(ActivityMovieItemListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
