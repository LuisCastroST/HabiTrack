package com.example.habitrack;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Ayuda a reitulizar el gestionamiento del motor TTS de Android. Configura automaticamente
 * el idioma a españo e implementa metodos seguros para la reproduccion de voz
 * y liberacion de recursos para evitar fugas.
 */

public class TtsHelper {
    private TextToSpeech tts;
    private boolean isInitialized = false;

    /**
     * Constructor que inicia el motor TTS y configura el idioma predeterminado en españo.
     *
     * @param context Contexto de la aplicacion o actividad actual.
     */

    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "ES"));
                isInitialized = true;
            }
        });
    }

    /**
     * Reproduce en voz alta el texto recibido por el parametro si el motor TTS se euncuentra iniciado.
     *
     * @param texto Texto a sintetizar y reproducir en audio.
     */

    public void hablar(String texto) {
        if (isInitialized && texto != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Detiene cualquer reproduccion en curso y libera recursos del motor TTS para prevenir
     * fugas de memoria al cerrar la actividad.
     */

    public void liberar() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}