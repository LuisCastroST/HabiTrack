package com.example.habitrack;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_actividad_fisica);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        habitoId = getIntent().getIntExtra("HABITO_ID", -1);
        String nombreHabito = dbHelper.obtenerNombreHabito(habitoId);

        Button btnVolver = findViewById(R.id.btnVolverAtras);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
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
            btnFinalizar.setOnClickListener(v -> finalizarEntrenamiento());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("ACTUALIZAR_UBICACION_GPS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ubicacionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(ubicacionReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(ubicacionReceiver);
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void onObjetivoSeleccionado(double lat, double lng) {
            runOnUiThread(() -> {
                latObjetivo = lat;
                lngObjetivo = lng;
                modoObjetivoFijado = true;
                Toast.makeText(PantallaActividadFisica.this, "Objetivo fijado", Toast.LENGTH_SHORT).show();
            });
        }
    }

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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}