package com.example.habitrack;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
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

    private SharedPreferences sharedPreferences;
    private TextView tvTituloHabito;
    private DatabaseHelper dbHelper;
    private int habitoId;

    private static final String PREFS_NAME = "HabiTrackPrefs";

    private TtsHelper ttsHelper;

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
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        habitoId = getIntent().getIntExtra("HABITO_ID", -1);

        recyclerViewHistorial = findViewById(R.id.recyclerCalendario);
        tvTituloHabito = findViewById(R.id.tvTituloCalendarioHabito);

        if (recyclerViewHistorial != null) {
            recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        }

        sincronizarTtsConPreferencia();
        hablarSiHabilitado("Historial de cumplimiento");

        cargarHistorialHabito();
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
     * Ciclo de vida: Sincroniza preferencias al reanudar la actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();
        sincronizarTtsConPreferencia();
    }

    /**
     * Cierra de forma segura la instancia abierta del DatabaseHelper y TTS.
     * Al destruir la actividad para prevenir fugas de memoria.
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