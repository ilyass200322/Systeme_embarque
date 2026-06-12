package com.example.movies_app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.movies_app.database.AppDatabase;
import com.example.movies_app.database.UserEntity;
import com.example.movies_app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.registerButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            // Enregistrement dans la base de données Room
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                UserEntity existingUser = db.userDao().getUserByEmail(email);

                if (existingUser != null) {
                    runOnUiThread(() -> Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show());
                } else {
                    UserEntity newUser = new UserEntity(name, email, password);
                    db.userDao().registerUser(newUser);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Compte créé avec succès pour " + name, Toast.LENGTH_SHORT).show();
                        finish(); // Retour au login
                    });
                }
            }).start();
        });

        binding.loginText.setOnClickListener(v -> finish());
    }
}
