package com.example.movies_app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class MovieEntity {
    @PrimaryKey
    public int id;
    public String title;
    public String releaseDate;
    public String posterPath;
    public String overview;

    public MovieEntity(int id, String title, String releaseDate, String posterPath, String overview) {
        this.id = id;
        this.title = title;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.overview = overview;
    }
}
