package com.example.habitrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * Receptor de transmisiones encargado de interceptar las alarmas programadas
 * y disparar notificaciones locales en el dispositivo para hacer recordar al usuario
 * el cumplimiento de sus habitos.
 */

public class NotificationReceiver extends BroadcastReceiver {

    /**
     * Metodo ejecutado al recibirse la señal de la larma programada.
     * Crea el canal de notificacion, configura la intencion de apertura al pulsar la alerta
     * y emite la notificacion en el sistema.
     *
     * @param context Contexto de la aplicacion en el momento de recibir la transmision.
     * @param intent  Intento que contiene los parametros adjuntos como el nombre e ID del habito.
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        String nombreHabito = intent.getStringExtra("HABITO_NOMBRE");

        if (nombreHabito == null || nombreHabito.trim().isEmpty()) {
            nombreHabito = "Actividad Física";
        }

        int habitoId = intent.getIntExtra("HABITO_ID", 0);

        String channelId = "habitrack_canal_recordatorios";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Recordatorios de Hábitos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent tapIntent = new Intent(context, GestionarHabitos.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                habitoId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("Hora de tu hábito")
                .setContentText("Es momento de realizar: " + nombreHabito)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(habitoId, builder.build());
        }
    }
}