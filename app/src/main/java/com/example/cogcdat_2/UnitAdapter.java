package com.example.cogcdat_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class UnitAdapter extends RecyclerView.Adapter<UnitAdapter.UnitViewHolder> {

    private List<String> units;
    private int selectedPosition = -1;
    private OnUnitSelectedListener listener;

    public interface OnUnitSelectedListener {
        void onUnitSelected(String unit);
    }

    public UnitAdapter(List<String> units, OnUnitSelectedListener listener) {
        this.units = units;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public String getSelectedUnit() {
        if (selectedPosition >= 0 && selectedPosition < units.size()) {
            return units.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public UnitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unit_option, parent, false);
        return new UnitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UnitViewHolder holder, int position) {
        String unit = units.get(position);
        holder.tvUnitName.setText(unit);
        
        // Применяем primary обводку для выбранного элемента
        boolean isSelected = position == selectedPosition;
        if (isSelected) {
            holder.cardView.setStrokeWidth(3);
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        holder.itemView.setOnClickListener(v -> {
            setSelectedPosition(position);
            if (listener != null) {
                listener.onUnitSelected(unit);
            }
        });
    }

    @Override
    public int getItemCount() {
        return units != null ? units.size() : 0;
    }

    static class UnitViewHolder extends RecyclerView.ViewHolder {
        TextView tvUnitName;
        MaterialCardView cardView;

        UnitViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvUnitName = itemView.findViewById(R.id.tv_unit_name);
        }
    }
}
