package com.example.habitrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnReiniciar, btnGestionarHabitos, btnVerCalendario, btnCerrarSesion;
    private Switch switchTema;
    private ProgressBar progressBarHabitos;
    private TextView tvRacha;
    private LinearLayout layoutContenedorHabitos;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabiTrackPrefs";
    private static final String KEY_MODO_OSCURO = "modo_oscuro";

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
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        tvRacha = findViewById(R.id.tvRacha);
        layoutContenedorHabitos = findViewById(R.id.layoutContenedorHabitos);

        boolean modoOscuroActivo = sharedPreferences.getBoolean(KEY_MODO_OSCURO, false);
        if (switchTema != null) {
            switchTema.setChecked(modoOscuroActivo);
            switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean(KEY_MODO_OSCURO, isChecked).apply();
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
            btnVerCalendario.setOnClickListener(v -> mostrarDialogoSeleccionarHabitoParaCalendario());
        }

        if (btnReiniciar != null) {
            btnReiniciar.setOnClickListener(v -> mostrarDialogoReiniciar());
        }

        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        solicitarPermisoNotificaciones();
        programarNotificacionDiaria();
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(MainActivity.this, PantallaLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarDialogoSeleccionarHabitoParaCalendario() {
        Cursor cursor = dbHelper.obtenerTodosLosHabitos();
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, "No hay hábitos registrados para mostrar", Toast.LENGTH_SHORT).show();
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        java.util.List<String> nombres = new java.util.ArrayList<>();
        java.util.List<Integer> ids = new java.util.ArrayList<>();

        int idIdx = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
        int nombreIdx = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_NOMBRE);

        do {
            if (idIdx != -1 && nombreIdx != -1) {
                ids.add(cursor.getInt(idIdx));
                nombres.add(cursor.getString(nombreIdx));
            }
        } while (cursor.moveToNext());
        cursor.close();

        String[] arrayNombres = nombres.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Seleccionar hábito")
                .setItems(arrayNombres, (dialog, which) -> {
                    int habitoIdSeleccionado = ids.get(which);
                    Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
                    intent.putExtra("HABITO_ID", habitoIdSeleccionado);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarTextoRacha();
        cargarHabitosDashboard();
    }

    private String obtenerFechaHoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
    private void mostrarDialogoCerrarSesion() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Cerrar Sesión")
                .setMessage("Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Cerrar Sesión", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarHabitosDashboard() {
        if (layoutContenedorHabitos == null) return;

        layoutContenedorHabitos.removeAllViews();
        String fechaHoy = obtenerFechaHoy();
        Cursor cursor = dbHelper.obtenerHabitosDashboard();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
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
            }
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

        if (cursor != null) {
            if (cursor.moveToFirst()) {
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
            }
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
        if (tvRacha == null) return;
        Cursor cursor = dbHelper.obtenerHabitosDashboard();
        int rachaMaxima = 0;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
                do {
                    if (idIndex != -1) {
                        int idHabito = cursor.getInt(idIndex);
                        int rachaHabito = dbHelper.calcularRachaHabito(idHabito);
                        if (rachaHabito > rachaMaxima) {
                            rachaMaxima = rachaHabito;
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        tvRacha.setText("Racha: " + rachaMaxima + " días");
    }

    private void mostrarDialogoReiniciar() {
        new AlertDialog.Builder(this)
                .setTitle("Reiniciar progreso")
                .setMessage("Deseas reiniciar todo el progrso?")
                .setPositiveButton("Reiniciar", (dialog, which) -> {
                    DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                    dbHelper.reiniciarTodo();
                    Toast.makeText(MainActivity.this, "Progreso reiniciado correctamente", Toast.LENGTH_SHORT).show();
                    recreate();
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

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void programarNotificacionDiaria() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }
}