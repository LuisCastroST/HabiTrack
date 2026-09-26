package com.example.habitrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    CheckBox checkAgua, checkLeer, checkCaminar;
    Button btnAceptar, btnReiniciar;
    Switch switchTema;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        checkAgua = findViewById(R.id.checkBoxTomarAgua);
        checkLeer = findViewById(R.id.checkBoxLeer);
        checkCaminar = findViewById(R.id.checkBoxCaminar);
        btnAceptar = findViewById(R.id.btnAceptar);

        btnReiniciar = findViewById(R.id.btnReiniciar);
        switchTema = findViewById(R.id.switchModoOscuro);

        if (switchTema != null) {
            switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }
        if (btnAceptar != null && checkAgua != null && checkLeer != null && checkCaminar != null) {
            btnAceptar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StringBuilder mensaje = new StringBuilder("Hábitos de hoy:\n");
                    boolean hayHabitos = false;

                    if (checkAgua.isChecked()) {
                        mensaje.append("- Tomar Agua\n");
                        hayHabitos = true;
                    }
                    if (checkLeer.isChecked()) {
                        mensaje.append("- Leer\n");
                        hayHabitos = true;
                    }
                    if (checkCaminar.isChecked()) {
                        mensaje.append("- Caminar\n");
                        hayHabitos = true;
                    }

                    if (!hayHabitos) {
                        mensaje = new StringBuilder("¡Aún no has completado hábitos hoy! Anímate.");
                    }
                    Toast.makeText(MainActivity.this, mensaje.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
        if (btnReiniciar != null) {
            btnReiniciar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Reiniciar Hábitos")
                            .setMessage("¿Estás seguro de que deseas desmarcar todos los hábitos de hoy?")
                            .setPositiveButton("Sí, reiniciar", new android.content.DialogInterface.OnClickListener() {
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    if (checkAgua != null) checkAgua.setChecked(false);
                                    if (checkLeer != null) checkLeer.setChecked(false);
                                    if (checkCaminar != null) checkCaminar.setChecked(false);

                                    Toast.makeText(MainActivity.this, "Hábitos reiniciados", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancelar", new android.content.DialogInterface.OnClickListener() {
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
        }
        View vistaPrincipal = findViewById(R.id.main);
        if (vistaPrincipal != null) {
            ViewCompat.setOnApplyWindowInsetsListener(vistaPrincipal, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }
}