package com.example.habitrack;

/**
 * Modelo de datos que representa el estado de cumplimiento diario de un habito
 * incluye informacion sobre la fecha, el estado de completitud, distancia recorrida y tiempo
 * transcurrido en caso de que el habito sea una actividad fisica.
 */

public class EstadoDiaHabito {
    private String fecha;
    private boolean completado;
    private float distancia;
    private long tiempoSegundos;

    /**
     * Constructor para inicializar el registro del estado diario de un habito.
     *
     * @param fecha          Fecha correspondiente al dia evaluado en formato yyyy-MM-dd.
     * @param completado     Estado de cumplimiento.
     * @param distancia      Distancia recorrida en metros asociada a la actividad fisica.
     * @param tiempoSegundos Duracion total de la actividad física en segundos.
     */

    public EstadoDiaHabito(String fecha, boolean completado, float distancia, long tiempoSegundos) {
        this.fecha = fecha;
        this.completado = completado;
        this.distancia = distancia;
        this.tiempoSegundos = tiempoSegundos;
    }

    /**
     * Obtiene la fecha del dia evaluado.
     *
     * @return String con la fecha en formato yyyy-MM-dd.
     */

    public String getFecha() {
        return fecha;
    }

    /**
     * Indica si el habito fue completado en esta fecha.
     *
     * @return boolean True si se completo, false si no se completo.
     */

    public boolean isCompletado() {
        return completado;
    }

    /**
     * Obtiene la distancia recorrida registrada en la fecha.
     *
     * @return float para la distancia en metros.
     */

    public float getDistancia() {
        return distancia;
    }

    /**
     * Obtiene el tiempo transcurrido de la actividad en segundos.
     *
     * @return long para el tiempo total en segundos.
     */

    public long getTiempoSegundos() {
        return tiempoSegundos;
    }
}

