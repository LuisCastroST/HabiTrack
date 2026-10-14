package com.example.habitrack;

import android.Manifest;
import android.content.pm.PackageManager;
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

            webViewMapa.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    mapaListo = true;
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
                String modalidadSeleccionada = (spinnerModalidad != null && spinnerModalidad.getSelectedItem() != null)
                        ? spinnerModalidad.getSelectedItem().toString()
                        : "Trotar";
                String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                if (habitoId != -1) {
                    dbHelper.guardarEstadoHabitoFecha(habitoId, fechaHoy, true);
                    Toast.makeText(this, "Actividad de " + modalidadSeleccionada + " guardada", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error: Hábito no identificado, por favor intente de nuevo", Toast.LENGTH_SHORT).show();
                }
            });
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
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                if (tvCoordenadas != null) {
                    tvCoordenadas.setText("Lat: " + lat + " | Lng: " + lng);
                }

                if (mapaListo && webViewMapa != null) {
                    webViewMapa.evaluateJavascript("javascript:actualizarUbicacion(" + lat + ", " + lng + ");", null);
                }

                if (habitoId != -1) {
                    dbHelper.actualizarUbicacionHabito(habitoId, lat, lng);
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
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
            }
        }
    }
}