package com.example.habitrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.core.content.ContextCompat;

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
    private Spinner spinnerModalidad;
    private Location ubicacionAnterior = null;
    private float distanciaTotalMetros = 0;
    private long tiempoInicioMs = 0;
    private boolean enEjecucion = false;

    private double latObjetivo = 0, lngObjetivo = 0;
    private boolean modoObjetivoFijado = false;
    private final float RADIO_LLEGADA_METROS = 15.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private boolean mapaListo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_actividad_fisica);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        habitoId = getIntent().getIntExtra("HABITO_ID", -1);

        tvCoordenadas = findViewById(R.id.tvCoordenadas);
        spinnerModalidad = findViewById(R.id.spinnerModalidad);
        Button btnFinalizar = findViewById(R.id.btnFinalizarTrote);
        webViewMapa = findViewById(R.id.webViewMapa);

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
                    iniciarModoNeutral();
                    verificarPermisosYObtenerUbicacion();
                }
            });

            webViewMapa.loadUrl("file:///android_asset/mapa.html");
        }

        if (spinnerModalidad != null) {
            String[] modalidades = {"Trotar", "Caminar"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modalidades);
            spinnerModalidad.setAdapter(adapter);
        }

        if (btnFinalizar != null) {
            btnFinalizar.setOnClickListener(v -> {
                finalizarModoNeutral();
            });
        }
    }

    private void iniciarModoNeutral() {
        distanciaTotalMetros = 0;
        ubicacionAnterior = null;
        tiempoInicioMs = System.currentTimeMillis();
        enEjecucion = true;
        Toast.makeText(this, "Seguimiento libre iniciado", Toast.LENGTH_SHORT).show();
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void onObjetivoSeleccionado(double lat, double lng) {
            runOnUiThread(() -> {
                latObjetivo = lat;
                lngObjetivo = lng;
                modoObjetivoFijado = true;
                Toast.makeText(PantallaActividadFisica.this,
                        "Objetivo fijado.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void finalizarModoNeutral() {
        enEjecucion = false;
        long tiempoTotalSegundos = (System.currentTimeMillis() - tiempoInicioMs) / 1000;
        long minutos = tiempoTotalSegundos / 60;
        long segundos = tiempoTotalSegundos % 60;

        String modalidadSeleccionada = (spinnerModalidad != null && spinnerModalidad.getSelectedItem() != null)
                ? spinnerModalidad.getSelectedItem().toString()
                : "Trotar";

        String resumen = String.format(Locale.getDefault(),
                "Actividad: %s\nDistancia: %.1f metros\nTiempo: %d min %d seg",
                modalidadSeleccionada, distanciaTotalMetros, minutos, segundos);

        if (habitoId != -1) {
            String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.guardarEstadoHabitoFecha(habitoId, fechaHoy, true);
        }

        new AlertDialog.Builder(this)
                .setTitle("Resumen de Actividad")
                .setMessage(resumen)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }

    private void mostrarDialogoVictoria(float distancia, long tiempoSegundos) {
        long minutos = tiempoSegundos / 60;
        long segundos = tiempoSegundos % 60;

        String mensaje = String.format(Locale.getDefault(),
                "Has llegado a tu meta \n\nDistancia total: %.1f metros\nTiempo empleado: %d min %d seg",
                distancia, minutos, segundos);

        if (habitoId != -1) {
            String fechaHoyStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.guardarEstadoHabitoFecha(habitoId, fechaHoyStr, true);
        }

        new AlertDialog.Builder(this)
                .setTitle("Objetivo Alcanzado")
                .setMessage(mensaje)
                .setPositiveButton("Finalizar", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void procesarUbicacionConFiltro(Location location) {
        if (!enEjecucion) return;

        if (location.hasAccuracy() && location.getAccuracy() <= 30.0f) {
            if (ubicacionAnterior != null) {
                float distanciaTramo = ubicacionAnterior.distanceTo(location);
                if (distanciaTramo < 100.0f && distanciaTramo > 0.5f) {
                    distanciaTotalMetros += distanciaTramo;
                }
            }
            ubicacionAnterior = location;

            if (tvCoordenadas != null) {
                if (modoObjetivoFijado) {
                    Location destino = new Location("destino");
                    destino.setLatitude(latObjetivo);
                    destino.setLongitude(lngObjetivo);
                    float distanciaAlObjetivo = location.distanceTo(destino);
                    tvCoordenadas.setText(String.format(Locale.getDefault(), "Meta a: %.1f m | Recorrido: %.1f m", distanciaAlObjetivo, distanciaTotalMetros));

                    if (distanciaAlObjetivo <= RADIO_LLEGADA_METROS) {
                        modoObjetivoFijado = false;
                        long tiempoTotalSegundos = (System.currentTimeMillis() - tiempoInicioMs) / 1000;
                        mostrarDialogoVictoria(distanciaTotalMetros, tiempoTotalSegundos);
                    }
                } else {
                    tvCoordenadas.setText(String.format(Locale.getDefault(), "Distancia libre: %.1f m", distanciaTotalMetros));
                }
            }

            if (mapaListo && webViewMapa != null) {
                webViewMapa.evaluateJavascript("javascript:actualizarUbicacion(" + location.getLatitude() + ", " + location.getLongitude() + ");", null);
            }

        } else {
            Toast.makeText(this, "Buscando mejor señal GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarPermisosYObtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                procesarUbicacionConFiltro(location);

                if (habitoId != -1) {
                    dbHelper.actualizarUbicacionHabito(habitoId, location.getLatitude(), location.getLongitude());
                }
            } else {
                Toast.makeText(this, "Buscando señal GPS...", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Permiso de ubicación denegado, por favor asegurate que la app tenga permisos especiales...", Toast.LENGTH_LONG).show();
            }
        }
    }
}