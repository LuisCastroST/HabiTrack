package com.example.habitrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
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
    ProgressBar progressBarHabitos;
    TextView tvRacha;

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        checkAgua = findViewById(R.id.checkBoxTomarAgua);
        checkLeer = findViewById(R.id.checkBoxLeer);
        checkCaminar = findViewById(R.id.checkBoxCaminar);
        btnAceptar = findViewById(R.id.btnAceptar);
        btnReiniciar = findViewById(R.id.btnReiniciar);
        switchTema = findViewById(R.id.switchModoOscuro);
        progressBarHabitos = findViewById(R.id.progressBarHabitos);
        tvRacha = findViewById(R.id.tvRacha);

        if (checkAgua != null) checkAgua.setChecked(dbHelper.obtenerEstadoHabito("Agua"));
        if (checkLeer != null) checkLeer.setChecked(dbHelper.obtenerEstadoHabito("Leer"));
        if (checkCaminar != null) checkCaminar.setChecked(dbHelper.obtenerEstadoHabito("Caminar"));

        actualizarBarraDeProgreso();

        if (switchTema != null) {
            switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int id = buttonView.getId();
                if (id == R.id.checkBoxTomarAgua) {
                    dbHelper.actualizarEstadoHabito("Agua", isChecked);
                } else if (id == R.id.checkBoxLeer) {
                    dbHelper.actualizarEstadoHabito("Leer", isChecked);
                } else if (id == R.id.checkBoxCaminar) {
                    dbHelper.actualizarEstadoHabito("Caminar", isChecked);
                }

                actualizarBarraDeProgreso();
            }
        };

        if (checkAgua != null) checkAgua.setOnCheckedChangeListener(checkListener);
        if (checkLeer != null) checkLeer.setOnCheckedChangeListener(checkListener);
        if (checkCaminar != null) checkCaminar.setOnCheckedChangeListener(checkListener);

        if (btnAceptar != null && checkAgua != null && checkLeer != null && checkCaminar != null) {
            btnAceptar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StringBuilder mensaje = new StringBuilder("Hábitos guardados:\n");
                    boolean hayHabitos = false;

                    if (checkAgua.isChecked()) { mensaje.append("- Tomar Agua\n"); hayHabitos = true; }
                    if (checkLeer.isChecked()) { mensaje.append("- Leer\n"); hayHabitos = true; }
                    if (checkCaminar.isChecked()) { mensaje.append("- Caminar\n"); hayHabitos = true; }

                    if (!hayHabitos) {
                        mensaje = new StringBuilder("No as completado ningun habito hoy");
                    } else {
                        if (progressBarHabitos != null && progressBarHabitos.getProgress() == 100) {
                            if (tvRacha != null) tvRacha.setText("Racha: 4 días");
                            mensaje.append("\n Dia completado 100%");
                        }
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
                            .setMessage("¿Realmente quieres reiniciar tus habitos?")
                            .setPositiveButton("Reiniciar", new android.content.DialogInterface.OnClickListener() {
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    dbHelper.reiniciarTodosHabitos();

                                    if (checkAgua != null) checkAgua.setChecked(false);
                                    if (checkLeer != null) checkLeer.setChecked(false);
                                    if (checkCaminar != null) checkCaminar.setChecked(false);

                                    Toast.makeText(MainActivity.this, "Progreso eliminado", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
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

    private void actualizarBarraDeProgreso() {
        if (progressBarHabitos == null || checkAgua == null || checkLeer == null || checkCaminar == null) return;

        int totalHabitos = 3;
        int habitosCompletados = 0;

        if (checkAgua.isChecked()) habitosCompletados++;
        if (checkLeer.isChecked()) habitosCompletados++;
        if (checkCaminar.isChecked()) habitosCompletados++;

        int porcentaje = (habitosCompletados * 100) / totalHabitos;
        progressBarHabitos.setProgress(porcentaje);
    }
}