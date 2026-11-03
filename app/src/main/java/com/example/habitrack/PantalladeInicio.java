package com.example.habitrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Pantalla de bienvenida o inicio, verifica si existe una sesion de usuario activa en FireBase,
 * de ser asi redirige automaticamente al @link MainActivity. De lo contrario, presenta la interfaz
 * incial para permitir al usuario acceder a la pantalla de inicio de sesion.
 */

public class PantalladeInicio extends AppCompatActivity {

    private Button btnIniciar;
    private FirebaseAuth mAuth;

    /**
     * Inicializa el componente de autentificacion en Firebase comprobando si el usuario ya ha
     * iniciado sesion previamente y, si no a iniciado sesion previamente, carga el diseño
     * visual configurando el boton de acceso hacia la pantalla de autenticacion.
     *
     * @param savedInstanceState Estado previo guardado de la actividad, si existe.
     */

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