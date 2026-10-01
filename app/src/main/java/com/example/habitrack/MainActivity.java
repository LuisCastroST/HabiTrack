package com.example.habitrack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnReiniciar, btnGestionarHabitos, btnVerCalendario;
    private Switch switchTema;
    private ProgressBar progressBarHabitos;
    private TextView tvRacha;
    private LinearLayout layoutContenedorHabitos;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabiTrackPrefs";
    private static final String KEY_RACHA = "racha_dias";
    private static final String KEY_LAST_DATE = "ultima_fecha";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        btnReiniciar = findViewById(R.id.btnReiniciar);
        btnGestionarHabitos = findViewById(R.id.btnAgregarHabito);
        btnVerCalendario = findViewById(R.id.btnVerCalendario);
        switchTema = findViewById(R.id.switchModoOscuro);
        progressBarHabitos = findViewById(R.id.progressBarHabitos);
        tvRacha = findViewById(R.id.tvRacha);
        layoutContenedorHabitos = findViewById(R.id.layoutContenedorHabitos);

        if (switchTema != null) {
            switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        if (btnGestionarHabitos != null) {
            btnGestionarHabitos.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, GestionarHabitos.class);
                startActivity(intent);
            });
        }

        if (btnVerCalendario != null) {
            btnVerCalendario.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
                startActivity(intent);
            });
        }

        if (btnReiniciar != null) {
            btnReiniciar.setOnClickListener(v -> mostrarDialogoReiniciar());
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

    @Override
    protected void onResume() {
        super.onResume();
        procesarCicloDiario();
        actualizarTextoRacha();
        cargarHabitosDashboard();
    }

    private String obtenerFechaHoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void procesarCicloDiario() {
        String fechaHoy = obtenerFechaHoy();
        String ultimaFecha = sharedPreferences.getString(KEY_LAST_DATE, "");

        if (ultimaFecha.isEmpty()) {
            sharedPreferences.edit().putString(KEY_LAST_DATE, fechaHoy).apply();
            return;
        }

        if (!ultimaFecha.equals(fechaHoy)) {
            evaluarCumplimientoFecha(ultimaFecha);
            sharedPreferences.edit().putString(KEY_LAST_DATE, fechaHoy).apply();
        }
    }

    private void evaluarCumplimientoFecha(String fecha) {
        Cursor cursor = dbHelper.obtenerHabitosDashboard();
        int total = 0;
        int completados = 0;

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
            do {
                if (idIndex != -1) {
                    int habitoId = cursor.getInt(idIndex);
                    total++;
                    if (dbHelper.estaCompletadoEnFecha(habitoId, fecha)) {
                        completados++;
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        int rachaActual = sharedPreferences.getInt(KEY_RACHA, 0);
        if (total > 0 && completados == total) {
            sharedPreferences.edit().putInt(KEY_RACHA, rachaActual + 1).apply();
        } else if (total > 0 && completados < total) {
            sharedPreferences.edit().putInt(KEY_RACHA, 0).apply();
        }
    }

    private void cargarHabitosDashboard() {
        if (layoutContenedorHabitos == null) return;

        layoutContenedorHabitos.removeAllViews();
        String fechaHoy = obtenerFechaHoy();
        Cursor cursor = dbHelper.obtenerHabitosDashboard();

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
            int nombreIndex = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_NOMBRE);

            do {
                if (idIndex != -1 && nombreIndex != -1) {
                    int idHabito = cursor.getInt(idIndex);
                    String nombreHabito = cursor.getString(nombreIndex);
                    boolean completadoHoy = dbHelper.estaCompletadoEnFecha(idHabito, fechaHoy);

                    CheckBox cb = new CheckBox(this);
                    cb.setText(nombreHabito);
                    cb.setChecked(completadoHoy);
                    cb.setTextSize(18);
                    cb.setPadding(0, 12, 0, 12);

                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        dbHelper.guardarEstadoHabitoFecha(idHabito, fechaHoy, isChecked);
                        actualizarBarraDeProgreso();
                    });

                    layoutContenedorHabitos.addView(cb);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        actualizarBarraDeProgreso();
    }

    private void actualizarBarraDeProgreso() {
        if (progressBarHabitos == null) return;

        String fechaHoy = obtenerFechaHoy();
        Cursor cursor = dbHelper.obtenerHabitosDashboard();
        int totalHabitos = 0;
        int habitosCompletados = 0;

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
            do {
                if (idIndex != -1) {
                    int idHabito = cursor.getInt(idIndex);
                    totalHabitos++;
                    if (dbHelper.estaCompletadoEnFecha(idHabito, fechaHoy)) {
                        habitosCompletados++;
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (totalHabitos == 0) {
            progressBarHabitos.setProgress(0);
            return;
        }

        int porcentaje = (habitosCompletados * 100) / totalHabitos;
        progressBarHabitos.setProgress(porcentaje);
    }

    private void actualizarTextoRacha() {
        int racha = sharedPreferences.getInt(KEY_RACHA, 0);
        if (tvRacha != null) {
            tvRacha.setText("Racha: " + racha + " días");
        }
    }

    private void mostrarDialogoReiniciar() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Reiniciar Progreso")
                .setMessage("Deseas reiniciar tu racha a 0?")
                .setPositiveButton("Reiniciar", (dialog, which) -> {
                    sharedPreferences.edit().putInt(KEY_RACHA, 0).apply();
                    actualizarTextoRacha();
                    Toast.makeText(MainActivity.this, "Progreso reiniciado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}