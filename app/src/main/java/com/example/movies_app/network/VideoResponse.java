package com.example.movies_app.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VideoResponse {
    @SerializedName("results")
    private List<Video> results;

    public List<Video> getResults() { return results; }

    public static class Video {
        @SerializedName("key")
        private String key;
        @SerializedName("type")
        private String type;
        @SerializedName("site")
        private String site;

        public String getKey() { return key; }
        public String getType() { return type; }
        public String getSite() { return site; }
    }
}
