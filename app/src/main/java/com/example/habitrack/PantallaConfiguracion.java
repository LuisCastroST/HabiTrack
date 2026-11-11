package com.example.habitrack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Pantalla encargada de gestionar la configuracion general permitiendo administrar la cuenta
 * de usuario, preferencias, accesibilidad, permisos del sistema y el reinicio de la base de datos local.
 */
public class PantallaConfiguracion extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private TextView tvUsuarioEmail, tvEstadoUbicacion;
    private Spinner spinnerThemeOptions;
    private Switch switchVozBotones, switchNotificaciones;
    private Button btnGestionarPermisos, btnReiniciarDatos;
    private static final String KEY_TTS_PENDIENTE = "tts_anuncio_pendiente";

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabiTrackPrefs";
    private TtsHelper ttsHelper;

    /**
     * Inicializa los componentes de la actividad, vincula las vistas XML,
     * configura la persistencia y carga las preferencias iniciales del usuario.
     *
     * @param savedInstanceState Estado guardado previamente, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_configuracion);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvUsuarioEmail = findViewById(R.id.tvUsuarioEmail);
        tvEstadoUbicacion = findViewById(R.id.tvEstadoUbicacion);
        spinnerThemeOptions = findViewById(R.id.spinnerThemeOptions);
        switchVozBotones = findViewById(R.id.switchVozBotones);
        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        btnGestionarPermisos = findViewById(R.id.btnGestionarPermisos);
        btnReiniciarDatos = findViewById(R.id.btnReiniciarDatos);

        cargarDatosUsuario();
        configurarSpinnerTemas();
        cargarPreferenciasGuardadas();
        configurarEventosListener();

        sincronizarTtsConPreferencia();
        reproducirAnuncioPendiente();
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
     * Reproduce y limpia cualquier anuncio pendiente guardado antes de recrear la actividad.
     */
    private void reproducirAnuncioPendiente() {
        if (!sharedPreferences.getBoolean("voz_botones", false)) {
            sharedPreferences.edit()
                    .remove(KEY_TTS_PENDIENTE)
                    .apply();
            return;
        }

        String anuncio = sharedPreferences.getString(
                KEY_TTS_PENDIENTE,
                null
        );

        if (anuncio == null) {
            return;
        }

        sharedPreferences.edit()
                .remove(KEY_TTS_PENDIENTE)
                .apply();

        spinnerThemeOptions.post(() ->
                hablarSiHabilitado(anuncio)
        );
    }

    /**
     * Carga y muestra el correo electronico del usuario autenticado mediante Firebase Auth.
     * Muestra un estado predeterminado si no hay sesion activa.
     */
    private void cargarDatosUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvUsuarioEmail.setText(user.getEmail());
        } else {
            tvUsuarioEmail.setText("Modo Local / Sin sesión activa");
        }
    }

    /**
     * Configura el adaptador del Spinner para las opciones de tema.
     */
    private void configurarSpinnerTemas() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.opciones_tema,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThemeOptions.setAdapter(adapter);
    }

    /**
     * Restaura los estados previos guardados en SharedPreferences para los switches y el spinner.
     */
    private void cargarPreferenciasGuardadas() {
        int temaSeleccionado = sharedPreferences.getInt("tema_app", 2);
        spinnerThemeOptions.setSelection(temaSeleccionado);

        boolean vozActiva = sharedPreferences.getBoolean("voz_botones", false);
        boolean notifActivas = sharedPreferences.getBoolean("notificaciones", true);

        switchVozBotones.setChecked(vozActiva);
        switchNotificaciones.setChecked(notifActivas);

        spinnerThemeOptions.setOnItemSelectedListener(this);
    }

    /**
     * Configura los listeners para todos los componentes interactivos de la pantalla.
     */
    private void configurarEventosListener() {
        switchVozBotones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit()
                    .putBoolean("voz_botones", isChecked)
                    .apply();

            Toast.makeText(
                    this,
                    isChecked
                            ? "Lectura de voz activada"
                            : "Lectura de voz desactivada",
                    Toast.LENGTH_SHORT
            ).show();

            if (isChecked) {
                sincronizarTtsConPreferencia();
                hablarSiHabilitado("Lectura de voz activada.");
            } else {
                if (ttsHelper != null) {
                    ttsHelper.liberar();
                    ttsHelper = null;
                }
            }
        });

        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notificaciones", isChecked).apply();
            hablarSiHabilitado(isChecked ? "Recordatorios activados." : "Recordatorios desactivados.");
            Toast.makeText(this, isChecked ? "Recordatorios activados" : "Recordatorios desactivados", Toast.LENGTH_SHORT).show();
        });

        btnGestionarPermisos.setOnClickListener(v -> {
            hablarSiHabilitado("Configuración de permisos.");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        btnReiniciarDatos.setOnClickListener(v -> {
            hablarSiHabilitado("Advertencia de seguridad.");
            mostrarDialogoConfirmacionReinicio();
        });
    }

    /**
     * Maneja la selección de un elemento dentro del Spinner de temas.
     *
     * @param parent El AdapterView donde ocurrio la seleccion.
     * @param view La vista dentro del AdapterView que fue seleccionada.
     * @param position La posición de la vista en el adaptador.
     * @param id El ID de la fila del elemento seleccionado.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int temaGuardado = sharedPreferences.getInt("tema_app", 2);

        if (position == temaGuardado) {
            return;
        }

        String nombreTema = parent.getItemAtPosition(position).toString();

        sharedPreferences.edit()
                .putInt("tema_app", position)
                .putString(
                        KEY_TTS_PENDIENTE,
                        "Tema seleccionado: " + nombreTema
                )
                .apply();

        switch (position) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        spinnerThemeOptions.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                reproducirAnuncioPendiente();
            }
        }, 1200);
    }

    /**
     * Callback ejecutado cuando no se selecciona ningun elemento en el Spinner.
     *
     * @param parent El AdapterView que no contiene elementos seleccionados.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Muestra una ventana de alerta de seguridad para confirmar el restablecimiento total de la base de datos.
     */
    private void mostrarDialogoConfirmacionReinicio() {
        new AlertDialog.Builder(this)
                .setTitle("Advertencia de Seguridad")
                .setMessage("¿Estás seguro de restablecer todos los datos locales en SQLite? Se perderá el historial de hábitos.")
                .setPositiveButton("Sí, restablecer", (dialog, which) -> {
                    dbHelper.reiniciarTodo();
                    hablarSiHabilitado("Base de datos local restablecida con éxito.");
                    Toast.makeText(this, "Base de datos local restablecida con éxito", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    hablarSiHabilitado("Reinicio cancelado.");
                })
                .show();
    }

    /**
     * Ciclo de vida: Sincroniza preferencias al reanudar la actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();
        sincronizarTtsConPreferencia();
    }

    /**
     * Cierra la conexion abierta con el link @Databasehelper y libera el TTS al destruir
     * la actividad para prevenir una fuga de recursos.
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