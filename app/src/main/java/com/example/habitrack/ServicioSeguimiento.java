package com.example.habitrack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class ServicioSeguimiento extends Service {

    private static final String CHANNEL_ID = "HabiTrackServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public static float distanciaTotalMetros = 0;
    public static long tiempoInicioMs = 0;
    private Location ubicacionAnterior = null;

    @Override
    public void onCreate() {
        super.onCreate();
        distanciaTotalMetros = 0;
        tiempoInicioMs = System.currentTimeMillis();
        ubicacionAnterior = null;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        crearCanalNotificacion();

        Intent notificationIntent = new Intent(this, PantallaActividadFisica.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HabiTrack")
                .setContentText("Actividad fisica en curso")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        iniciarUbicacion();
    }

    private void iniciarUbicacion() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location.hasAccuracy() && location.getAccuracy() <= 30.0f) {
                        if (ubicacionAnterior != null) {
                            float distanciaTramo = ubicacionAnterior.distanceTo(location);
                            if (distanciaTramo < 100.0f && distanciaTramo > 0.5f) {
                                distanciaTotalMetros += distanciaTramo;
                            }
                        }
                        ubicacionAnterior = location;

                        Intent intent = new Intent("ACTUALIZAR_UBICACION_GPS");
                        intent.putExtra("lat", location.getLatitude());
                        intent.putExtra("lng", location.getLongitude());
                        intent.putExtra("distancia", distanciaTotalMetros);
                        sendBroadcast(intent);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal de Seguimiento de Actividad",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}