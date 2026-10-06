package com.example.habitrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PantalladeInicio extends AppCompatActivity {

    private Button btnIniciar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(PantalladeInicio.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_pantallade_inicio);

        btnIniciar = findViewById(R.id.btnPantallaInicioIniciar);

        btnIniciar.setOnClickListener(v -> {
            Intent intent = new Intent(PantalladeInicio.this, PantallaLogin.class);
            startActivity(intent);
        });
    }
}