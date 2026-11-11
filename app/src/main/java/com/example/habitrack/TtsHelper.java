package com.example.habitrack;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Ayuda a reitulizar el gestionamiento del motor TTS de Android. Configura automaticamente
 * el idioma a español e implementa metodos seguros para la reproduccion de voz
 * y liberacion de recursos para evitar fugas.
 */

public class TtsHelper {
    private TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean released = false;
    private String textoPendiente = null;

    /**
     *Constructor que inicia el motor TTS y configura el idioma predeterminado en español.
     *@param context Contexto de la aplicacion o actividad actual.
     */

    public TtsHelper(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (released || tts == null) {
                return;
            }

            if (status == TextToSpeech.SUCCESS) {
                int resultado = tts.setLanguage(new Locale("es", "ES"));

                if (resultado == TextToSpeech.LANG_MISSING_DATA ||
                        resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = false;
                    return;
                }

                isInitialized = true;

                if (textoPendiente != null) {
                    String texto = textoPendiente;
                    textoPendiente = null;
                    hablar(texto);
                }
            }
        });
    }

    /**
     * Reproduce en voz alta el texto recibido por el parametro si el motor TTS se euncuentra iniciado.
     * @param texto Texto a sintetizar y reproducir en audio.
     */

    public void hablar(String texto) {
        if (released || texto == null || texto.trim().isEmpty()) {
            return;
        }

        if (!isInitialized) {
            textoPendiente = texto;
            return;
        }

        tts.speak(
                texto,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "HABITRACK_TTS"
        );
    }

    public void detener() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Detiene cualquer reproduccion en curso y libera recursos del motor TTS para prevenir
     * fugas de memoria al cerrar la actividad.
     */

    public void liberar() {
        released = true;
        isInitialized = false;
        textoPendiente = null;

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}