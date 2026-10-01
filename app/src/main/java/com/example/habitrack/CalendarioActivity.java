package com.example.habitrack;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarioActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private LinearLayout layoutListaHistorial;
    private TextView tvFechaSeleccionada;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        dbHelper = new DatabaseHelper(this);
        calendarView = findViewById(R.id.calendarView);
        layoutListaHistorial = findViewById(R.id.layoutListaHistorial);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);

        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (tvFechaSeleccionada != null) {
            tvFechaSeleccionada.setText("Historial del " + fechaHoy);
        }
        cargarHistorialDeFecha(fechaHoy);

        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                String fechaSeleccionada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, (month + 1), dayOfMonth);
                if (tvFechaSeleccionada != null) {
                    tvFechaSeleccionada.setText("Historial del " + fechaSeleccionada);
                }
                cargarHistorialDeFecha(fechaSeleccionada);
            });
        }
    }

    private void cargarHistorialDeFecha(String fecha) {
        if (layoutListaHistorial == null) return;

        layoutListaHistorial.removeAllViews();
        Cursor cursor = dbHelper.obtenerHabitosCompletadosPorFecha(fecha);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int nombreIdx = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_NOMBRE);
                int catIdx = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_CATEGORIA);
                int colorIdx = cursor.getColumnIndex(HabitContract.HabitEntry.COLUMN_COLOR);

                if (nombreIdx != -1) {
                    String nombre = cursor.getString(nombreIdx);
                    String categoria = catIdx != -1 ? cursor.getString(catIdx) : "General";
                    String colorHex = (colorIdx != -1 && cursor.getString(colorIdx) != null) ? cursor.getString(colorIdx) : "#4CAF50";

                    LinearLayout tarjeta = new LinearLayout(this);
                    tarjeta.setOrientation(LinearLayout.VERTICAL);
                    tarjeta.setPadding(24, 16, 24, 16);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.setMargins(0, 8, 0, 8);
                    tarjeta.setLayoutParams(layoutParams);

                    GradientDrawable shape = new GradientDrawable();
                    shape.setCornerRadius(12f);
                    try {
                        shape.setColor(Color.parseColor(colorHex));
                    } catch (Exception e) {
                        shape.setColor(Color.parseColor("#4CAF50"));
                    }
                    tarjeta.setBackground(shape);

                    TextView tvNombre = new TextView(this);
                    tvNombre.setText(nombre);
                    tvNombre.setTextSize(16);
                    tvNombre.setTextColor(Color.WHITE);
                    tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView tvCategoria = new TextView(this);
                    tvCategoria.setText(categoria);
                    tvCategoria.setTextSize(12);
                    tvCategoria.setTextColor(Color.WHITE);

                    tarjeta.addView(tvNombre);
                    tarjeta.addView(tvCategoria);

                    layoutListaHistorial.addView(tarjeta);
                }
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            TextView vacio = new TextView(this);
            vacio.setText("Sin hábitos completados en esta fecha.");
            vacio.setTextSize(14);
            vacio.setPadding(0, 12, 0, 12);
            layoutListaHistorial.addView(vacio);
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}