package com.example.habitrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarioActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistorial;
    private TextView tvTituloHabito;
    private DatabaseHelper dbHelper;
    private int habitoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        dbHelper = new DatabaseHelper(this);
        habitoId = getIntent().getIntExtra("HABITO_ID", -1);

        recyclerViewHistorial = findViewById(R.id.recyclerCalendario);
        tvTituloHabito = findViewById(R.id.tvTituloCalendarioHabito);

        if (recyclerViewHistorial != null) {
            recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        }

        cargarHistorialHabito();
    }

    private void cargarHistorialHabito() {
        if (habitoId != -1) {
            List<EstadoDiaHabito> historial = dbHelper.obtenerHistorialDinamicoHabito(habitoId);
            CalendarioAdapter adapter = new CalendarioAdapter(historial);
            if (recyclerViewHistorial != null) {
                recyclerViewHistorial.setAdapter(adapter);
            }
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