package com.example.cogcdat_2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide; // Добавить эту строку

import java.io.File;
import java.util.List;

public class CarSpinnerAdapter extends ArrayAdapter<Car> {

    private final Context context;
    private final List<Car> cars;

    public CarSpinnerAdapter(@NonNull Context context, List<Car> cars) {
        super(context, R.layout.spinner_item_car, cars);
        this.context = context;
        this.cars = cars;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item_car, parent, false);
            holder = new ViewHolder();
            holder.carIcon = convertView.findViewById(R.id.iv_car_icon);
            holder.carName = convertView.findViewById(R.id.tv_car_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Car car = cars.get(position);

        // Устанавливаем данные
        holder.carName.setText(car.getName());

        // Загрузка изображения с использованием Glide
        if (car.getId() == -1) {
            // Для "Все автомобили" используем стандартную иконку
            holder.carIcon.setImageResource(R.drawable.ic_car_outline);
        } else {
            // Для конкретного автомобиля загружаем изображение
            loadCarImage(holder.carIcon, car.getImagePath());
        }

        return convertView;
    }

    private void loadCarImage(ImageView imageView, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                // Используем Glide для загрузки изображения
                Glide.with(context)
                        .load(imageFile)
                        .placeholder(R.drawable.ic_car_outline) // Заглушка на время загрузки
                        .error(R.drawable.ic_car_outline)       // Заглушка при ошибке
                        .centerCrop()                           // Обрезка по центру
                        .into(imageView);
            } else {
                // Файл не существует - используем стандартную иконку
                imageView.setImageResource(R.drawable.ic_car_outline);
            }
        } else {
            // Нет пути к изображению - используем стандартную иконку
            imageView.setImageResource(R.drawable.ic_car_outline);
        }
    }

    private static class ViewHolder {
        ImageView carIcon;
        TextView carName;
    }
}