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

public class CalendarioAdapter extends RecyclerView.Adapter<CalendarioAdapter.ViewHolder> {

    private List<EstadoDiaHabito> listaDias;

    public CalendarioAdapter(List<EstadoDiaHabito> listaDias) {
        this.listaDias = listaDias;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return listaDias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}