package com.example.habitrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Actividad encargada de visualizar el calendario e historial dinamico de cumplimiento
 * Para un habito en especifico de la app.
 * Renderiza los dias transcurridos y su estado mediante un @link RecyclerView.
 */

public class CalendarioActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistorial;
    private TextView tvTituloHabito;
    private DatabaseHelper dbHelper;
    private int habitoId;

    /**
     * Inicializa los componentes de la interfaz, recupera el identificador del habito enviado
     * por el intent y desencadena la carga del historial dinamico.
     *
     * @param savedInstanceState Estado previo guardado de la actividad.
     */

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

    /**
     * Consulta el historial dinamico del habito actual a traves del DatabaseHelper
     * y configura el adaptador visual del recyclerView para mostrar el desglose de dias.
     */

    private void cargarHistorialHabito() {
        if (habitoId != -1) {
            List<EstadoDiaHabito> historial = dbHelper.obtenerHistorialDinamicoHabito(habitoId);
            CalendarioAdapter adapter = new CalendarioAdapter(historial);
            if (recyclerViewHistorial != null) {
                recyclerViewHistorial.setAdapter(adapter);
            }
        }
    }

    /**
     * Cierra de forma segura la instancia abierta del DatabaseHelper.
     * Al destruir la actividad se ayuda a prevenir fugas de memoria.
     */

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}