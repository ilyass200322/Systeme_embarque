package com.example.movies_app;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movies_app.databinding.ActivityMainBinding;
import com.example.movies_app.model.Movie;
import com.example.movies_app.viewmodel.MovieViewModel;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MovieViewModel viewModel;
    private MyMovieAdapter myMovieAdapter;

    private SensorManager sensorManager;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    private final ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<String> spokenText = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (spokenText != null && !spokenText.isEmpty()) {
                        binding.editTextSearch.setText(spokenText.get(0));
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupViewModel();
        setupSearch();
        setupCategories();
        setupLogout();
        setupWelcomeText();
        setupAdvancedFeatures();

        viewModel.fetchPopularMovies(1);
    }

    private void setupAdvancedFeatures() {
        // MVP 2: Firebase Featured
        try {
            fetchFeaturedFromFirestore();
        } catch (Exception e) {
            // Show mock data if Firebase not configured
            binding.getRoot().postDelayed(() -> showFeaturedSection(null), 2000);
        }

        // MVP 3A: Voice Search
        binding.btnVoiceSearch.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                voiceLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Voice search not supported", Toast.LENGTH_SHORT).show();
            }
        });

        // MVP 3B: Shake suggest
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acceleration = 0f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        // MVP 4: AI Recommender
        binding.btnAiRecommender.setOnClickListener(v -> showAiDialog());

        // MVP 5: Profile
        binding.btnProfile.setOnClickListener(v -> 
            startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void fetchFeaturedFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("featured").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Movie> featuredList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    try {
                        Movie movie = doc.toObject(Movie.class);
                        if (movie.getTitle() != null) {
                            featuredList.add(movie);
                        }
                    } catch (Exception e) {
                        String title = doc.getString("title");
                        String path = doc.getString("poster_path");
                        if (title != null) {
                            featuredList.add(new Movie(0, title, path, ""));
                        }
                    }
                }
                showFeaturedSection(featuredList);
            }
        });
    }

    private void showFeaturedSection(List<Movie> list) {
        if (list == null || list.isEmpty()) {
            list = new ArrayList<>();
            List<Movie> current = viewModel.getMovies().getValue();
            if (current != null && current.size() > 5) {
                list.addAll(current.subList(0, 5));
            }
        }
        
        if (!list.isEmpty()) {
            binding.textFeatured.setVisibility(View.VISIBLE);
            binding.recyclerViewFeatured.setVisibility(View.VISIBLE);
            MyMovieAdapter featuredAdapter = new MyMovieAdapter(this);
            binding.recyclerViewFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.recyclerViewFeatured.setAdapter(featuredAdapter);
            featuredAdapter.setMovies(list);
        }
    }

    private void showAiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("AI Recommendation");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Tell me your mood (e.g. Scary sci-fi)");
        builder.setView(input);

        builder.setPositiveButton("Ask AI", (dialog, which) -> {
            String mood = input.getText().toString();
            generateAiRecommendation(mood);
        });
        builder.show();
    }

    private void generateAiRecommendation(String mood) {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            // Mock AI if key is missing for demonstration
            new AlertDialog.Builder(this)
                    .setTitle("AI Suggestion (Demo Mode)")
                    .setMessage("For your mood '" + mood + "', I highly recommend 'The Matrix' (1999). It perfectly fits the Moviez green & black aesthetic!")
                    .setPositiveButton("Cool!", null)
                    .show();
            return;
        }

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder().addText("Suggest a movie for this mood: " + mood).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Executor executor = ContextCompat.getMainExecutor(this);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("AI Suggestion")
                            .setMessage(result.getText())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "AI Error", Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            lastAcceleration = currentAcceleration;
            currentAcceleration = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = currentAcceleration - lastAcceleration;
            acceleration = acceleration * 0.9f + delta;
            if (acceleration > 5) { // More sensitive threshold
                acceleration = 0; // Prevent duplicate triggers
                suggestRandomMovie();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void suggestRandomMovie() {
        List<Movie> currentList = viewModel.getMovies().getValue();
        if (currentList != null && !currentList.isEmpty()) {
            Movie random = currentList.get(new Random().nextInt(currentList.size()));
            new AlertDialog.Builder(this)
                    .setTitle("Shake Suggestion!")
                    .setMessage("How about watching: " + random.getTitle() + "?\n\nIt matches the Moviez high-tech feed.")
                    .setPositiveButton("View Details", (d, w) -> {
                        Intent intent = new Intent(this, MovieDetailActivity.class);
                        intent.putExtra("movieId", random.getId());
                        startActivity(intent);
                    })
                    .setNegativeButton("Shake Again", null)
                    .show();
        } else {
            Toast.makeText(this, "Wait for movies to load, then shake!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(sensorListener, 
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(sensorListener);
    }

    private void setupWelcomeText() {
        String name = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_name", "");
        if (!name.isEmpty()) {
            binding.textWelcome.setText("Hello, " + name + "!");
        }
    }

    private void setupLogout() {
        binding.btnLogout.setOnClickListener(v -> {
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupRecyclerView() {
        myMovieAdapter = new MyMovieAdapter(this);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(myMovieAdapter);

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && 
                    layoutManager.findLastCompletelyVisibleItemPosition() == myMovieAdapter.getItemCount() - 1) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MovieViewModel.class);

        viewModel.getMovies().observe(this, movies -> {
            if (movies != null) {
                myMovieAdapter.setMovies(movies);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> 
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (myMovieAdapter != null) myMovieAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> 
            applyCurrentCategoryFilter()
        );
    }

    private void applyCurrentCategoryFilter() {
        if (myMovieAdapter == null) return;

        int checkedId = binding.chipGroupCategories.getCheckedChipId();
        
        if (checkedId == binding.chipFavories.getId()) {
            myMovieAdapter.filterByFavories();
        } else {
            int genreId = -1; // -1 pour "All"

            if (checkedId == binding.chipAction.getId()) genreId = 28;
            else if (checkedId == binding.chipComedy.getId()) genreId = 35;
            else if (checkedId == binding.chipDrama.getId()) genreId = 18;
            else if (checkedId == binding.chipRomance.getId()) genreId = 10749;
            else if (checkedId == binding.chipHorror.getId()) genreId = 27;

            myMovieAdapter.filterByCategory(genreId);
        }
    }
}
