package com.example.habitrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
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

import java.util.Calendar;

public class GestionarHabitos extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout layoutListaHabitos;
    private Button btnNuevoHabito;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_habitos);

        dbHelper = new DatabaseHelper(this);
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
                        String categoria = cursor.getString(categoriaIndex);
                        String colorHex = (colorIndex != -1 && cursor.getString(colorIndex) != null) ? cursor.getString(colorIndex) : "#4CAF50";
                        boolean enDashboard = cursor.getInt(enDashboardIndex) == 1;

                        LinearLayout fila = new LinearLayout(this);
                        fila.setOrientation(LinearLayout.HORIZONTAL);
                        fila.setPadding(0, 16, 0, 16);

                        TextView tvColor = new TextView(this);
                        tvColor.setText("   ");
                        try {
                            tvColor.setBackgroundColor(Color.parseColor(colorHex));
                        } catch (Exception e) {
                            tvColor.setBackgroundColor(Color.parseColor("#4CAF50"));
                        }
                        LinearLayout.LayoutParams paramsColor = new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.MATCH_PARENT);
                        paramsColor.setMargins(0, 0, 16, 0);
                        tvColor.setLayoutParams(paramsColor);

                        TextView tvInfo = new TextView(this);
                        tvInfo.setText(nombre + " (" + categoria + ")");
                        tvInfo.setTextSize(16);
                        LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                        tvInfo.setLayoutParams(paramsText);

                        Switch switchDashboard = new Switch(this);
                        switchDashboard.setChecked(enDashboard);
                        switchDashboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            dbHelper.actualizarVisibilidadDashboard(id, isChecked);
                        });

                        Button btnEliminar = new Button(this);
                        btnEliminar.setText("X");
                        btnEliminar.setOnClickListener(v -> {
                            dbHelper.eliminarHabito(id);
                            cargarListaHabitos();
                        });

                        fila.addView(tvColor);
                        fila.addView(tvInfo);
                        fila.addView(switchDashboard);
                        fila.addView(btnEliminar);

                        layoutListaHabitos.addView(fila);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
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

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private void preguntarActivarNotificacion(int habitoId, String nombreHabito) {
        new AlertDialog.Builder(this)
                .setTitle("Recordatorio")
                .setMessage("¿Quieres programar un recordatorio para '" + nombreHabito + "'?")
                .setPositiveButton("Sí, elegir hora", (dialog, which) -> {
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
            programarAlarmaHabito(habitoId, nombreHabito, hourOfDay, minute);
            Toast.makeText(this, "Recordatorio programado a las " + String.format("%02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
        }, horaActual, minutoActual, true);

        timePicker.show();
    }

    private void programarAlarmaHabito(int habitoId, String nombreHabito, int hora, int minuto) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);

        intent.putExtra("HABITO_NOMBRE", nombreHabito);
        intent.putExtra("HABITO_ID", habitoId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                habitoId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);
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