package com.example.movies_app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MovieDao {
    @Query("SELECT * FROM favorites")
    LiveData<List<MovieEntity>> getAllFavorites();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(MovieEntity movie);

    @Delete
    void deleteFavorite(MovieEntity movie);

    @Query("SELECT EXISTS(SELECT * FROM favorites WHERE id = :id)")
    LiveData<Boolean> isFavorite(int id);

    @Query("SELECT EXISTS(SELECT * FROM favorites WHERE id = :id)")
    boolean isFavoriteSync(int id);

    @Query("SELECT id FROM favorites")
    List<Integer> getAllFavoriteIdsSync();
}
