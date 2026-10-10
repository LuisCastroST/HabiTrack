package com.example.habitrack;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GestionarHabitos extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout layoutListaHabitos;
    private Button btnNuevoHabito;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private int habitoIdSeleccionadoParaGps = -1;

    private static class HabitoItem {
        int id;
        String nombre;
        String categoria;
        String colorHex;
        boolean enDashboard;

        HabitoItem(int id, String nombre, String categoria, String colorHex, boolean enDashboard) {
            this.id = id;
            this.nombre = nombre;
            this.categoria = categoria;
            this.colorHex = colorHex;
            this.enDashboard = enDashboard;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_habitos);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        layoutListaHabitos = findViewById(R.id.layoutListaHabitos);
        btnNuevoHabito = findViewById(R.id.btnNuevoHabito);

        if (btnNuevoHabito != null) {
            btnNuevoHabito.setOnClickListener(v -> mostrarDialogoCrearHabito());
        }

        cargarListaHabitos();
    }

    private void cargarListaHabitos() {
        if (layoutListaHabitos == null) return;

        layoutListaHabitos.removeAllViews();
        Cursor cursor = dbHelper.obtenerTodosLosHabitos();

        List<HabitoItem> habitosFisicos = new ArrayList<>();
        List<HabitoItem> habitosNormales = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(HabitContract.HabitEntry._ID);
                int nombreIndex = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_NOMBRE);
                int categoriaIndex = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_CATEGORIA);
                int colorIndex = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_COLOR);
                int enDashboardIndex = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_EN_DASHBOARD);

                do {
                    if (idIndex != -1 && nombreIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String nombre = cursor.getString(nombreIndex);
                        String categoria = (categoriaIndex != -1) ? cursor.getString(categoriaIndex) : "General";
                        String colorHex = (colorIndex != -1 && cursor.getString(colorIndex) != null) ? cursor.getString(colorIndex) : "#4CAF50";
                        boolean enDashboard = (enDashboardIndex != -1) && (cursor.getInt(enDashboardIndex) == 1);

                        HabitoItem item = new HabitoItem(id, nombre, categoria, colorHex, enDashboard);
                        if ("Deporte".equalsIgnoreCase(categoria)) {
                            habitosFisicos.add(item);
                        } else {
                            habitosNormales.add(item);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        if (!habitosFisicos.isEmpty()) {
            TextView tvHeaderFisicos = new TextView(this);
            tvHeaderFisicos.setText("--- Hábitos Físicos ---");
            tvHeaderFisicos.setTextSize(15);
            tvHeaderFisicos.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderFisicos.setPadding(0, 8, 0, 8);
            layoutListaHabitos.addView(tvHeaderFisicos);

            for (HabitoItem item : habitosFisicos) {
                layoutListaHabitos.addView(crearFilaHabito(item, true));
            }
        }

        if (!habitosNormales.isEmpty()) {
            TextView tvHeaderNormales = new TextView(this);
            tvHeaderNormales.setText("--- Hábitos Normales ---");
            tvHeaderNormales.setTextSize(15);
            tvHeaderNormales.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderNormales.setPadding(0, 16, 0, 8);
            layoutListaHabitos.addView(tvHeaderNormales);

            for (HabitoItem item : habitosNormales) {
                layoutListaHabitos.addView(crearFilaHabito(item, false));
            }
        }
    }

    private LinearLayout crearFilaHabito(HabitoItem item, boolean esFisico) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        fila.setPadding(0, 16, 0, 16);

        TextView tvColor = new TextView(this);
        tvColor.setText("   ");
        try {
            tvColor.setBackgroundColor(Color.parseColor(item.colorHex));
        } catch (Exception e) {
            tvColor.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
        LinearLayout.LayoutParams paramsColor = new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.MATCH_PARENT);
        paramsColor.setMargins(0, 0, 16, 0);
        tvColor.setLayoutParams(paramsColor);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(item.nombre + " (" + item.categoria + ")");
        tvInfo.setTextSize(16);
        LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvInfo.setLayoutParams(paramsText);
        tvInfo.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvInfo.setOnClickListener(v -> mostrarTimePicker(item.id, item.nombre));

        Switch switchDashboard = new Switch(this);
        switchDashboard.setChecked(item.enDashboard);
        switchDashboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.actualizarVisibilidadDashboard(item.id, isChecked);
        });

        fila.addView(tvColor);
        fila.addView(tvInfo);

        if (esFisico) {
            Button btnGps = new Button(this);
            btnGps.setText("📍");
            btnGps.setOnClickListener(v -> {
                Intent intent = new Intent(GestionarHabitos.this, PantallaActividadFisica.class);
                intent.putExtra("HABITO_ID", item.id);
                startActivity(intent);
            });
            fila.addView(btnGps);
        }

        fila.addView(switchDashboard);

        Button btnEliminar = new Button(this);
        btnEliminar.setText("X");
        btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmacionEliminar(item.id, item.nombre));
        fila.addView(btnEliminar);

        return fila;
    }

    public void actualizarNotificacionHabito(int habitoId, String nombreHabito, int nuevaHora, int nuevoMinuto) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("HABITO_NOMBRE", nombreHabito);
        intent.putExtra("HABITO_ID", habitoId);

        PendingIntent pendingIntentAntiguo = PendingIntent.getBroadcast(
                this,
                habitoId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntentAntiguo != null && alarmManager != null) {
            alarmManager.cancel(pendingIntentAntiguo);
            pendingIntentAntiguo.cancel();
        }

        PendingIntent nuevoPendingIntent = PendingIntent.getBroadcast(
                this,
                habitoId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, nuevaHora);
        calendar.set(Calendar.MINUTE, nuevoMinuto);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    nuevoPendingIntent
            );
        }
    }

    private void cancelarAlarmaHabito(int habitoId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                habitoId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void mostrarDialogoConfirmacionEliminar(int habitoId, String nombreHabito) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar hábito")
                .setMessage("Estás seguro de que deseas eliminar '" + nombreHabito + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    cancelarAlarmaHabito(habitoId);
                    dbHelper.eliminarHabito(habitoId);
                    cargarListaHabitos();
                    Toast.makeText(this, "Hábito eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoCrearHabito() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo Hábito");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText edtNombre = new EditText(this);
        edtNombre.setHint("Nombre del nuevo hábito");
        layout.addView(edtNombre);

        final Spinner spinnerCategoria = new Spinner(this);
        String[] categorias = {"Salud", "Estudio", "Deporte", "Dinero", "General"};
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categorias);
        spinnerCategoria.setAdapter(adapterCat);
        layout.addView(spinnerCategoria);

        final Spinner spinnerColor = new Spinner(this);
        String[] nombresColores = {"Verde", "Azul", "Rojo", "Naranja", "Morado"};
        final String[] valoresColores = {"#4CAF50", "#2196F3", "#F44336", "#FF9800", "#9C27B0"};
        ArrayAdapter<String> adapterColor = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresColores);
        spinnerColor.setAdapter(adapterColor);
        layout.addView(spinnerColor);

        final EditText edtDias = new EditText(this);
        edtDias.setHint("Días meta para completar el hábito");
        edtDias.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(edtDias);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = edtNombre.getText().toString().trim();
            String categoria = spinnerCategoria.getSelectedItem().toString();
            String colorHex = valoresColores[spinnerColor.getSelectedItemPosition()];
            String diasStr = edtDias.getText().toString().trim();

            if (nombre.isEmpty() || diasStr.isEmpty()) {
                Toast.makeText(this, "Algunos campos están vacíos", Toast.LENGTH_SHORT).show();
                return;
            }

            int dias = Integer.parseInt(diasStr);
            long id = dbHelper.insertarHabito(nombre, categoria, dias, colorHex);

            if (id != -1) {
                Toast.makeText(this, "Hábito guardado", Toast.LENGTH_SHORT).show();
                cargarListaHabitos();
                preguntarActivarNotificacion((int) id, nombre);
            } else {
                Toast.makeText(this, "Error al guardar el hábito", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void preguntarActivarNotificacion(int habitoId, String nombreHabito) {
        new AlertDialog.Builder(this)
                .setTitle("Recordatorio")
                .setMessage("Quieres programar un recordatorio para '" + nombreHabito + "'?")
                .setPositiveButton("Elegir hora", (dialog, which) -> {
                    mostrarTimePicker(habitoId, nombreHabito);
                })
                .setNegativeButton("No por ahora", null)
                .setCancelable(false)
                .show();
    }

    private void mostrarTimePicker(int habitoId, String nombreHabito) {
        Calendar c = Calendar.getInstance();
        int horaActual = c.get(Calendar.HOUR_OF_DAY);
        int minutoActual = c.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            actualizarNotificacionHabito(habitoId, nombreHabito, hourOfDay, minute);
            Toast.makeText(this, "Recordatorio programado a las " + String.format("%02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
        }, horaActual, minutoActual, true);

        timePicker.show();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}