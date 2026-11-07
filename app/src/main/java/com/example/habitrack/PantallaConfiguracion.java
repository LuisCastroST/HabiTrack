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
 * Pantalla encargada de gestionar la configuraciOn general permitiendo administrar la cuenta
 * de usuario, preferencias, accesibilidad, permisos del sistema y el reinicio de la base
 * de datos local.
 */
public class PantallaConfiguracion extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private TextView tvUsuarioEmail, tvEstadoUbicacion;
    private Spinner spinnerThemeOptions;
    private Switch switchVozBotones, switchAltoContraste, switchNotificaciones;
    private Button btnGestionarPermisos, btnReiniciarDatos;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    /**
     * Inicializa los componentes de la actividad, vincula las vistas XML,
     * configura la persistencia y carga las preferencias iniciales del usuario.
     *
     * @param savedInstanceState Estado guardado previamente, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("HabiTrackPrefs", Context.MODE_PRIVATE);

        tvUsuarioEmail = findViewById(R.id.tvUsuarioEmail);
        tvEstadoUbicacion = findViewById(R.id.tvEstadoUbicacion);
        spinnerThemeOptions = findViewById(R.id.spinnerThemeOptions);
        switchVozBotones = findViewById(R.id.switchVozBotones);
        switchAltoContraste = findViewById(R.id.switchAltoContraste);
        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        btnGestionarPermisos = findViewById(R.id.btnGestionarPermisos);
        btnReiniciarDatos = findViewById(R.id.btnReiniciarDatos);

        cargarDatosUsuario();
        configurarSpinnerTemas();
        cargarPreferenciasGuardadas();
        configurarEventosListener();
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
        spinnerThemeOptions.setOnItemSelectedListener(this);
    }

    /**
     * Restaura los estados previos guardados en SharedPreferences para los switches y el spinner.
     */
    private void cargarPreferenciasGuardadas() {
        int temaSeleccionado = sharedPreferences.getInt("tema_app", 2);
        spinnerThemeOptions.setSelection(temaSeleccionado);

        boolean vozActiva = sharedPreferences.getBoolean("voz_botones", false);
        boolean altoContrasteActivo = sharedPreferences.getBoolean("alto_contraste", false);
        boolean notifActivas = sharedPreferences.getBoolean("notificaciones", true);

        switchVozBotones.setChecked(vozActiva);
        switchAltoContraste.setChecked(altoContrasteActivo);
        switchNotificaciones.setChecked(notifActivas);
    }

    /**
     * Configura los listeners para todos los componentes interactivos de la pantalla.
     */
    private void configurarEventosListener() {
        switchVozBotones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("voz_botones", isChecked).apply();
            Toast.makeText(this, isChecked ? "Lectura de voz activada" : "Lectura de voz desactivada", Toast.LENGTH_SHORT).show();
        });

        switchAltoContraste.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("alto_contraste", isChecked).apply();
            Toast.makeText(this, isChecked ? "Alto contraste activado" : "Alto contraste desactivado", Toast.LENGTH_SHORT).show();
        });

        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notificaciones", isChecked).apply();
            Toast.makeText(this, isChecked ? "Recordatorios activados" : "Recordatorios desactivados", Toast.LENGTH_SHORT).show();
        });

        btnGestionarPermisos.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        btnReiniciarDatos.setOnClickListener(v -> mostrarDialogoConfirmacionReinicio());
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
        sharedPreferences.edit().putInt("tema_app", position).apply();
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
                    Toast.makeText(this, "Base de datos local restablecida con éxito", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}