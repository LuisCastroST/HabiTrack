package com.example.habitrack;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * Actividad principal para la gestion de habitos.
 * Permite listar habitos existentes divididos entre habitos normales y actividades fisicas,
 * ademas permite la creacion de nuevos habitos mediante dialogos interactivos donde el usuario
 * puede configurar recordatorios, seleccionar colores para cada habito, ambitos y la meta en dias.
 * Puede directamente elegir si desea mostrar un habito en la pantalla principal ademas de poder
 * eliminar registros.
 */

public class GestionarHabitos extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout layoutListaHabitos;
    private Button btnNuevoHabito;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private int habitoIdSeleccionadoParaGps = -1;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabiTrackPrefs";
    private TtsHelper ttsHelper;

    /**
     * Estructura de datos interna para representar un habito en la interfaz.
     */

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

    /**
     * Inicializa los componentes de la interfaz de usuario y carga la lista dinamica de habitos
     * almacenados en la base de datos local.
     *
     * @param savedInstanceState Estado previo guardado de la actividad, si existe.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_habitos);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        layoutListaHabitos = findViewById(R.id.layoutListaHabitos);
        btnNuevoHabito = findViewById(R.id.btnNuevoHabito);

        if (btnNuevoHabito != null) {
            btnNuevoHabito.setOnClickListener(v -> {
                hablarSiHabilitado("Nuevo habito");
                mostrarDialogoCrearHabito();
            });
        }

        sincronizarTtsConPreferencia();
        hablarSiHabilitado("Gestion de habitos cargada.");

        cargarListaHabitos();
    }

    /**
     * Consulta todos los habitos desde el DatabaseHelper, los clasifica entre habitos fisicos
     * y normales, y los dibuja dinamicamente en el contenedor visual.
     */

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
                int categoriaIndex = cursor.getColumnIndex("categoria_nombre");
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
                        if ("Actividad fisica".equalsIgnoreCase(categoria) || "Actividad Física".equalsIgnoreCase(categoria)) {
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
            tvHeaderFisicos.setText("--- Actividades Físicas ---");
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

    /**
     * Crea dinamicamente la fila grafica que le corresponde al habito, incluyendo botones de gps,
     * historial, switches de dashboard y boton de eliminacion.
     *
     * @param item Objeto con los datos del habito a renderizas.
     * @param esFisico Booleano que indica si el habito pertenece a una actividad fisica o no.
     * @return LinearLayout con la estructura visual de la fila completa.
     */

    private LinearLayout crearFilaHabito(HabitoItem item, boolean esFisico) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        fila.setPadding(0, 16, 0, 16);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvColor = new TextView(this);
        tvColor.setText("   ");
        try {
            tvColor.setBackgroundColor(Color.parseColor(item.colorHex));
        } catch (Exception e) {
            tvColor.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
        LinearLayout.LayoutParams paramsColor = new LinearLayout.LayoutParams(24, LinearLayout.LayoutParams.MATCH_PARENT);
        paramsColor.setMargins(0, 0, 12, 0);
        tvColor.setLayoutParams(paramsColor);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(item.nombre);
        tvInfo.setTextSize(15);
        LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvInfo.setLayoutParams(paramsText);
        tvInfo.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvInfo.setOnClickListener(v -> {
            hablarSiHabilitado("Configurar recordatorio");
            mostrarTimePicker(item.id, item.nombre);
        });

        fila.addView(tvColor);
        fila.addView(tvInfo);

        if (esFisico) {
            Button btnGps = new Button(this);
            btnGps.setText("GPS");
            btnGps.setTextSize(12);
            btnGps.setPadding(15, 0, 15, 0);
            btnGps.setOnClickListener(v -> {
                hablarSiHabilitado("Actividad fisica");
                Intent intent = new Intent(GestionarHabitos.this, PantallaActividadFisica.class);
                intent.putExtra("HABITO_ID", item.id);
                startActivity(intent);
            });
            fila.addView(btnGps);
        }

        Button btnCalendario = new Button(this);
        btnCalendario.setText("Hist.");
        btnCalendario.setTextSize(12);
        btnCalendario.setPadding(15, 0, 15, 0);
        btnCalendario.setOnClickListener(v -> {
            hablarSiHabilitado("Historial");
            Intent intent = new Intent(GestionarHabitos.this, CalendarioActivity.class);
            intent.putExtra("HABITO_ID", item.id);
            startActivity(intent);
        });
        fila.addView(btnCalendario);

        Switch switchDashboard = new Switch(this);
        switchDashboard.setChecked(item.enDashboard);
        switchDashboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hablarSiHabilitado(isChecked ? "Visible en dashboard" : "Oculto del dashboard");
            dbHelper.actualizarVisibilidadDashboard(item.id, isChecked);
        });
        fila.addView(switchDashboard);

        Button btnEliminar = new Button(this);
        btnEliminar.setText("X");
        btnEliminar.setTextSize(12);
        btnEliminar.setPadding(15, 0, 15, 0);
        btnEliminar.setOnClickListener(v -> {
            hablarSiHabilitado("Eliminar habito");
            mostrarDialogoConfirmacionEliminar(item.id, item.nombre);
        });
        fila.addView(btnEliminar);

        return fila;
    }

    /**
     * Configura o actualiza una alarma que se repite diariamente mediante el sistema
     * de AlarmManager para notificar al usuario sobre un habito en especifico.
     *
     * @param habitoId ID unico del habito asociado a la notificacion.
     * @param nombreHabito Nombre del habito que aparecera en el aviso.
     * @param nuevaHora Hora programada en formato de 24H.
     * @param nuevoMinuto Minuto programado para la alarma.
     */

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

    /**
     * Cancela y elimina la alarma programada asociada a un habito.
     *
     * @param habitoId ID unico del habito cuya alarma sera cancelada.
     */

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

    /**
     * Muestra un dialogo de confirmacion antes de permanentemente eliminar un habito del sistema.
     *
     * @param habitoId ID del habito a eliminar.
     * @param nombreHabito Nombre descriptivo del habito para ser mostrado en la alerta.
     */

    private void mostrarDialogoConfirmacionEliminar(int habitoId, String nombreHabito) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar habito")
                .setMessage("Estás seguro de que deseas eliminar '" + nombreHabito + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    cancelarAlarmaHabito(habitoId);
                    dbHelper.eliminarHabito(habitoId);
                    cargarListaHabitos();
                    Toast.makeText(this, "Habito eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Despliega un formulario interactivo en una ventana de dialogo flotante para la creacion
     * de un nuevo habito capturando el nombre, categoria, color y dias que durara el habito.
     */

    private void mostrarDialogoCrearHabito() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo Habito");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText edtNombre = new EditText(this);
        edtNombre.setHint("Nombre del nuevo habito");
        layout.addView(edtNombre);

        final Spinner spinnerCategoria = new Spinner(this);
        Cursor cursorCat = dbHelper.obtenerCategorias();
        List<String> nombresCategorias = new ArrayList<>();
        List<Integer> idsCategorias = new ArrayList<>();

        if (cursorCat != null && cursorCat.moveToFirst()) {
            int idIndex = cursorCat.getColumnIndex(HabitContract.CategoriaEntry._ID);
            int nombreIndex = cursorCat.getColumnIndex(HabitContract.CategoriaEntry.COLUMN_NOMBRE);
            do {
                if (idIndex != -1 && nombreIndex != -1) {
                    idsCategorias.add(cursorCat.getInt(idIndex));
                    nombresCategorias.add(cursorCat.getString(nombreIndex));
                }
            } while (cursorCat.moveToNext());
            cursorCat.close();
        }

        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresCategorias);
        spinnerCategoria.setAdapter(adapterCat);
        layout.addView(spinnerCategoria);

        final Spinner spinnerColor = new Spinner(this);
        String[] nombresColores = {"Verde", "Azul", "Rojo", "Naranja", "Morado"};
        final String[] valoresColores = {"#4CAF50", "#2196F3", "#F44336", "#FF9800", "#9C27B0"};
        ArrayAdapter<String> adapterColor = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresColores);
        spinnerColor.setAdapter(adapterColor);
        layout.addView(spinnerColor);

        final EditText edtDias = new EditText(this);
        edtDias.setHint("Días meta para completar el habito");
        edtDias.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(edtDias);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = edtNombre.getText().toString().trim();
            int posicionSeleccionada = spinnerCategoria.getSelectedItemPosition();
            int categoriaId = (idsCategorias.isEmpty() || posicionSeleccionada == -1) ? 1 : idsCategorias.get(posicionSeleccionada);

            String colorHex = valoresColores[spinnerColor.getSelectedItemPosition()];
            String diasStr = edtDias.getText().toString().trim();

            if (nombre.isEmpty() || diasStr.isEmpty()) {
                Toast.makeText(this, "Algunos campos están vacíos", Toast.LENGTH_SHORT).show();
                return;
            }

            int dias = Integer.parseInt(diasStr);

            if (dias <= 0 || dias > 365) {
                Toast.makeText(this, "Los días meta deben estar entre 1 y 365, womp womp", Toast.LENGTH_LONG).show();
                return;
            }

            long id = dbHelper.insertarHabito(nombre, categoriaId, dias, colorHex);

            if (id != -1) {
                Toast.makeText(this, "Habito guardado", Toast.LENGTH_SHORT).show();
                cargarListaHabitos();
                preguntarActivarNotificacion((int) id, nombre);
            } else {
                Toast.makeText(this, "Error al guardar el habito", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Le pregunta al usuario si desea configurar un recordatorio en el horario especifico
     * tras crear un habito.
     *
     * @param habitoId ID unico del habito recien creado.
     * @param nombreHabito Nombre del habito.
     */

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

    /**
     * Despliega un selector de hora para programar la alarma de notificacion del habito.
     *
     * @param habitoId ID unico del habito.
     * @param nombreHabito Nombre del habito.
     */

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

    /**
     * Sincroniza el estado del motor TTS con la preferencia guardada.
     */
    private void sincronizarTtsConPreferencia() {
        boolean vozHabilitada = sharedPreferences.getBoolean("voz_botones", false);

        if (vozHabilitada && ttsHelper == null) {
            ttsHelper = new TtsHelper(getApplicationContext());
        } else if (!vozHabilitada && ttsHelper != null) {
            ttsHelper.liberar();
            ttsHelper = null;
        }
    }

    /**
     * Reproduce un texto por voz de forma segura si el TTS se encuentra habilitado.
     */
    private void hablarSiHabilitado(String texto) {
        if (ttsHelper != null) {
            ttsHelper.hablar(texto);
        }
    }

    /**
     * Ciclo de vida Sincronizando las preferencias al reanudar la actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();
        sincronizarTtsConPreferencia();
    }

    /**
     * Cierra la conexion abierta con el DatabaseHelper al destruir la actividad para prevenir
     * fugas de recursos.
     */

    @Override
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.liberar();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}