package com.example.habitrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Actividad de registro de nuevos usuarios permitiendo crear una nueva cuenta mediante el correo
 * electronico y contraseña utilizando Firebase Authentication, validando longitudes de contraseña
 * minimas, luego de registrarse es ingresado directamente al dashboard principal.
 */

public class PantallaRegistro extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegistrar;
    private TextView tvVolverLogin;
    private FirebaseAuth mAuth;

    /**
     * Inicializa la instancia de Firebase Authentication vinculando todos los componentes de
     * la interfaz grafica y configura los listeners para el boton de registro y el enlace
     * de retorno al login.
     *
     * @param savedInstanceState Estado previo guardado de la actividad, si existe.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_registro);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmailRegistro);
        etPassword = findViewById(R.id.etPasswordRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        tvVolverLogin = findViewById(R.id.tvVolverLogin);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        tvVolverLogin.setOnClickListener(v -> finish());
    }

    /**
     * Valida que los campos de correo y contraseña no esten vacios, verifica que la contraseña
     * cumpla con el requisito minimo de longitud y procede a registrar al usuario en FireBase,
     * Si el registro es correcto, redirige el usuario al @link MainActivity, de lo contrario,
     * muestra un mensaje de error.
     */

    private void registrarUsuario() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Comleta todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(PantallaRegistro.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(PantallaRegistro.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}