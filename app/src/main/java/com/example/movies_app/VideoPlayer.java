package com.example.movies_app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayer extends AppCompatActivity {

    private WebView webView;
    private String  videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Récupère l'URL envoyée depuis MovieDetailActivity
        videoUrl = getIntent().getStringExtra("videoUrl");

        // Initialise le WebView
        webView = findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Charge la vidéo YouTube
        webView.loadUrl(videoUrl);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (webView != null) webView.loadUrl(videoUrl);
    }
}