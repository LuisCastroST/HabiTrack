package com.example.habitrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
    private Spinner spinnerModalidad;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

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

        if (spinnerModalidad != null) {
            String[] modalidades = {"Trotar", "Caminar"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modalidades);
            spinnerModalidad.setAdapter(adapter);
        }

        obtenerUbicacion();

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

    private void obtenerUbicacion() {
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

                if (habitoId != -1) {
                    dbHelper.actualizarUbicacionHabito(habitoId, lat, lng);
                }
            } else {
                Toast.makeText(this, "Buscando señal GPS...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
            }
        }
    }
}