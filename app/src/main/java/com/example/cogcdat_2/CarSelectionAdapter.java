// Новый файл: CarSelectionAdapter.java
package com.example.cogcdat_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

public class CarSelectionAdapter extends RecyclerView.Adapter<CarSelectionAdapter.CarViewHolder> {

    private List<Car> cars;
    private int selectedPosition = -1;
    private OnCarSelectedListener listener;

    public interface OnCarSelectedListener {
        void onCarSelected(Car car);
    }

    public CarSelectionAdapter(List<Car> cars, OnCarSelectedListener listener) {
        this.cars = cars;
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

    public Car getSelectedCar() {
        if (selectedPosition >= 0 && selectedPosition < cars.size()) {
            return cars.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car_option, parent, false); // Новый layout, см. ниже
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);
        holder.tvCarName.setText(car.getName());

        String imagePath = car.getImagePath();
        if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(imagePath))
                    .circleCrop()
                    .placeholder(R.drawable.ic_car_outline)
                    .error(R.drawable.ic_car_outline)
                    .into(holder.ivCarIcon);
        } else {
            holder.ivCarIcon.setImageResource(R.drawable.ic_car_outline);
        }

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
                listener.onCarSelected(car);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cars != null ? cars.size() : 0;
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCarIcon;
        TextView tvCarName;
        MaterialCardView cardView;

        CarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivCarIcon = itemView.findViewById(R.id.iv_car_icon);
            tvCarName = itemView.findViewById(R.id.tv_car_name);
        }
    }
}