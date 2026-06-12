package com.example.movies_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.movies_app.database.AppDatabase;
import com.example.movies_app.database.UserEntity;
import com.example.movies_app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier si l'utilisateur est déjà connecté
        if (getSharedPreferences("user_prefs", MODE_PRIVATE).getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérification des identifiants dans Room
            new Thread(() -> {
                UserEntity user = AppDatabase.getInstance(this).userDao().login(email, password);
                if (user != null) {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                            .putBoolean("is_logged_in", true)
                            .putString("user_name", user.fullName)
                            .apply();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Bienvenue " + user.fullName, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        binding.registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
