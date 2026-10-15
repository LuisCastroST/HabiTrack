package com.example.habitrack;

public class EstadoDiaHabito {
    private String fecha;
    private boolean completado;
    private float distancia;
    private long tiempoSegundos;

    public EstadoDiaHabito(String fecha, boolean completado, float distancia, long tiempoSegundos) {
        this.fecha = fecha;
        this.completado = completado;
        this.distancia = distancia;
        this.tiempoSegundos = tiempoSegundos;
    }

    public String getFecha() {
        return fecha;
    }

    public boolean isCompletado() {
        return completado;
    }

    public float getDistancia() {
        return distancia;
    }

    public long getTiempoSegundos() {
        return tiempoSegundos;
    }
}