package com.example.habitrack;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

/**
 * Clase adaptadora para el RecyclerView encargado de mostrar el historial dinamico de cumplimiento
 * diario de los habitos en la vista de calendario.
 */

public class CalendarioAdapter extends RecyclerView.Adapter<CalendarioAdapter.ViewHolder> {

    private List<EstadoDiaHabito> listaDias;

    /**
     * Constructor del adaptador de calendario.
     *
     * @param listaDias Lista de objetos que contienen el estado diario del habito.
     */

    public CalendarioAdapter(List<EstadoDiaHabito> listaDias) {
        this.listaDias = listaDias;
    }

    /**
     * Infla y crea la vista para cada elemento del RecyClerView, utilizando layouts estandars.
     *
     * @param parent Grupo de vistas padra al que se añaden las nuevas vistas despues de su creacion.

     * @param viewType Tipo de vista del nuevo elemento.
     * @return Una nueva instancia con la vista del elemento inflada.
     */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Vincula los datos del estado diario de un habito con las vistas del contenedor
     * configurando textos dinamicos, tiempos, distancias y colores de fondo segun el cumplimiento.
     *
     * @param holder   Contenedor de vistas correspondiente al elemento actual.
     * @param position Contenedor de vistas correspondiente al elemento actual.
     */

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EstadoDiaHabito estado = listaDias.get(position);

        holder.text1.setText("Día " + (position + 1) + " - " + estado.getFecha());

        if (estado.isCompletado()) {
            if (estado.getDistancia() > 0 || estado.getTiempoSegundos() > 0) {
                long minutos = estado.getTiempoSegundos() / 60;
                long segundos = estado.getTiempoSegundos() % 60;
                holder.text2.setText(String.format(Locale.getDefault(), "Completado | %.1f m | %d min %d seg", estado.getDistancia(), minutos, segundos));
            } else {
                holder.text2.setText("Completado");
            }
            holder.itemView.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            holder.text2.setText("Pendiente / No realizado");
            holder.itemView.setBackgroundColor(Color.parseColor("#EF5350"));
        }
    }

    /**
     * Obtiene el numero total de dias contenidos en el historial.
     *
     * @return int Cantidad de elementos de la lista.
     */

    @Override
    public int getItemCount() {
        return listaDias.size();
    }

    /**
     * Clase contenedora de bistas para optimizar el rendimiento del RecycleView
     * al mapear los campos de textos estandares de la interfaz.
     */

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        /**
         * Inicia las referencias de los componentes de texto de la vista del elemento.
         *
         * @param itemView Vista inflada correspondiente a la vista fila del RecyclerView.
         */

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}