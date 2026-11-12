package com.example.habitrack;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Actividad encargada del seguimiento de actividades fisicas y geolocalizacion.
 * Gestiona un servicio en primer plano para el rastreo GPS, interactua con un mapa renderizado
 * en un @link WebView mediante JavaScript Bridge y calcula la distancia hacia objetivos o metas libres.
 */

public class PantallaActividadFisica extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper dbHelper;
    private int habitoId;
    private TextView tvCoordenadas;
    private WebView webViewMapa;
    private float distanciaTotalMetros = 0;
    private boolean enEjecucion = false;

    private boolean esPrimerFoco = true;
    private double latObjetivo = 0, lngObjetivo = 0;
    private boolean modoObjetivoFijado = false;
    private final float RADIO_LLEGADA_METROS = 15.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private boolean mapaListo = false;

    private TtsHelper ttsHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabiTrackPrefs";

    /**
     * Receptor de transmisiones para actualizar coordenadas y la distancia recorrida en tiempo
     * real enviadas por el servicio de ubicacion.
     */

    private BroadcastReceiver ubicacionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            float distancia = intent.getFloatExtra("distancia", 0);
            distanciaTotalMetros = distancia;

            actualizarInterfazUbicacion(lat, lng);
        }
    };

    /**
     * Inicia los componentes de la interfaz de actividad fisica, configura el @link WebView
     * con soporte de JavaScript e inicia el servicio de rastreo GPS en segundo plano.
     *
     * @param savedInstanceState Estado previo guardado de la actividad, si existe.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_actividad_fisica);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        sincronizarTtsConPreferencia();
        hablarSiHabilitado("Iniciando pantalla de actividad física. GPS activo.");

        int temaSeleccionado = sharedPreferences.getInt("tema_app", 2);
        switch (temaSeleccionado) {
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

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        habitoId = getIntent().getIntExtra("HABITO_ID", -1);
        String nombreHabito = dbHelper.obtenerNombreHabito(habitoId);

        Button btnVolver = findViewById(R.id.btnVolverAtras);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                hablarSiHabilitado("Volviendo a gestionar hábitos.");
                Intent intent = new Intent(PantallaActividadFisica.this, GestionarHabitos.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        TextView tvTitulo = findViewById(R.id.tvTituloActividadFisica);
        if (tvTitulo != null) {
            tvTitulo.setText(nombreHabito);
        }

        tvCoordenadas = findViewById(R.id.tvCoordenadas);
        Button btnFinalizar = findViewById(R.id.btnFinalizarTrote);
        webViewMapa = findViewById(R.id.webViewMapa);

        Intent serviceIntent = new Intent(this, ServicioSeguimiento.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (webViewMapa != null) {
            WebSettings webSettings = webViewMapa.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 8.0; HUAWEI RNE-L03) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36");
            webViewMapa.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

            webViewMapa.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    mapaListo = true;
                    enEjecucion = true;
                    verificarPermisosYObtenerUbicacion();
                }
            });

            webViewMapa.loadUrl("file:///android_asset/mapa.html");
        }

        if (btnFinalizar != null) {
            btnFinalizar.setOnClickListener(v -> {
                hablarSiHabilitado("Finalizando entrenamiento.");
                finalizarEntrenamiento();
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
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
     * Registra el receptor dinamico de actualizaciones GPS al reanudarse la actividad.
     */

    @Override
    protected void onResume() {
        super.onResume();
        sincronizarTtsConPreferencia();
        IntentFilter filter = new IntentFilter("ACTUALIZAR_UBICACION_GPS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ubicacionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(ubicacionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    /**
     * Anula el registro del receptor GPS al pausarse la actividad.
     */

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(ubicacionReceiver);
    }

    /**
     * Interfaz de comunicacion bidireccional mediante puente JavaScript (JavascriptInterface)
     * para interactuar con los eventos del mapa HTML.
     */

    public class WebAppInterface {

        /**
         * Captura la seleccion de un punto objetivo en el mapa web.
         *
         * @param lat Latitud del objetivo seleccionado.
         * @param lng Longitud del objetivo seleccionado.
         */
        @android.webkit.JavascriptInterface
        public void onObjetivoSeleccionado(double lat, double lng) {
            runOnUiThread(() -> {
                latObjetivo = lat;
                lngObjetivo = lng;
                modoObjetivoFijado = true;
                hablarSiHabilitado("Objetivo fijado en el mapa.");
                Toast.makeText(PantallaActividadFisica.this, "Objetivo fijado", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Actualiza los textos de la interfaz con la distancia, coordenadas y control de llegada a meta.
     *
     * @param lat Latitud actual obtenida por GPS.
     * @param lng Longitud actual obtenida por GPS.
     */

    private void actualizarInterfazUbicacion(double lat, double lng) {
        if (!enEjecucion) return;

        if (tvCoordenadas != null) {
            if (modoObjetivoFijado) {
                Location destino = new Location("destino");
                destino.setLatitude(latObjetivo);
                destino.setLongitude(lngObjetivo);

                Location actual = new Location("actual");
                actual.setLatitude(lat);
                actual.setLongitude(lng);

                float distanciaAlObjetivo = actual.distanceTo(destino);
                tvCoordenadas.setText(String.format(Locale.getDefault(), "Meta a: %.1f m | Recorrido: %.1f m", distanciaAlObjetivo, distanciaTotalMetros));

                if (distanciaAlObjetivo <= RADIO_LLEGADA_METROS) {
                    modoObjetivoFijado = false;
                    long tiempoTotalSegundos = (System.currentTimeMillis() - ServicioSeguimiento.tiempoInicioMs) / 1000;
                    hablarSiHabilitado("Has llegado a tu meta.");
                    mostrarDialogoVictoria(distanciaTotalMetros, tiempoTotalSegundos);
                }
            } else {
                tvCoordenadas.setText(String.format(Locale.getDefault(), "Distancia libre: %.1f m", distanciaTotalMetros));
            }
        }

        if (mapaListo && webViewMapa != null) {
            webViewMapa.evaluateJavascript("javascript:actualizarUbicacion(" + lat + ", " + lng + ", " + esPrimerFoco + ");", null);

            esPrimerFoco = false;
        }
    }

    /**
     * Muestra un dialogo de exito cuando el usuario alcanza el objetivo geografico fijado
     * guardando los detalles en la base de datos local.
     *
     * @param distancia Distancia total recorrida en metros.
     * @param tiempoSegundos Tiempo total empleado en segundos.
     */

    private void mostrarDialogoVictoria(float distancia, long tiempoSegundos) {
        long minutos = tiempoSegundos / 60;
        long segundos = tiempoSegundos % 60;

        String mensaje = String.format(Locale.getDefault(),
                "Has llegado a tu meta\n\nDistancia total: %.1f metros\nTiempo empleado: %d min %d seg",
                distancia, minutos, segundos);

        if (habitoId != -1) {
            String fechaHoyStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.guardarEstadoHabitoFechaConDetalle(habitoId, fechaHoyStr, true, distancia, tiempoSegundos);
        }

        new AlertDialog.Builder(this)
                .setTitle("Objetivo Alcanzado")
                .setMessage(mensaje)
                .setPositiveButton("Finalizar", (dialog, which) -> {
                    stopService(new Intent(this, ServicioSeguimiento.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Finaliza manualmente el entrenamiento fisico, detiene el servicio GPS,
     * calcula los totales y persiste los datos en la base de datos local.
     */

    private void finalizarEntrenamiento() {
        stopService(new Intent(this, ServicioSeguimiento.class));

        long tiempoTotalSegundos = (System.currentTimeMillis() - ServicioSeguimiento.tiempoInicioMs) / 1000;
        long minutos = tiempoTotalSegundos / 60;
        long segundos = tiempoTotalSegundos % 60;
        float distanciaFinal = distanciaTotalMetros;

        String nombreHabito = dbHelper.obtenerNombreHabito(habitoId);

        String resumen = String.format(Locale.getDefault(),
                "Resumen de sesion:\n\n" +
                        "Actividad: %s\n" +
                        "Distancia total: %.1f metros\n" +
                        "Tiempo empleado: %d minutos y %d segundos",
                nombreHabito, distanciaFinal, minutos, segundos);

        if (habitoId != -1) {
            String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.guardarEstadoHabitoFechaConDetalle(habitoId, fechaHoy, true, distanciaFinal, tiempoTotalSegundos);
        }

        new AlertDialog.Builder(this)
                .setTitle("Registro de Actividad")
                .setMessage(resumen)
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }

    /**
     * Verifica los permisos de ubicacion en tiempo de ejecucion y obtiene la ultima posicion conocida.
     */

    private void verificarPermisosYObtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && habitoId != -1) {
                dbHelper.actualizarUbicacionHabito(habitoId, location.getLatitude(), location.getLongitude());
            }
        });
    }

    /**
     * Maneja el resultado de la solicitud de permisos de ubicacion al usuario.
     *
     * @param requestCode Codigo de solicitud del permiso.
     * @param permissions Lista de permisos solicitados.
     * @param grantResults Resultados de concesion de cada permiso.
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarPermisosYObtenerUbicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicacion denegado", Toast.LENGTH_LONG).show();
            }
        }
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