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

import java.io.File;
import java.util.List;

public class CustomCarSpinnerAdapter extends ArrayAdapter<Car> {

    private final LayoutInflater inflater;
    private final List<Car> cars;

    public CustomCarSpinnerAdapter(@NonNull Context context, @NonNull List<Car> cars) {
        super(context, 0, cars);
        this.inflater = LayoutInflater.from(context);
        this.cars = cars;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, true);
    }

    private View createView(int position, View convertView, ViewGroup parent, boolean isDropdown) {
        final View view;
        final ViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(R.layout.spinner_item_car, parent, false);
            holder = new ViewHolder();
            holder.carName = view.findViewById(R.id.tv_car_name);
            holder.carIcon = view.findViewById(R.id.iv_car_icon);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        Car car = cars.get(position);

        if (car != null) {
            holder.carName.setText(car.getName());

            // --- НОВАЯ ЛОГИКА: ЗАГРУЗКА КАРТИНКИ ИЗ ПУТИ ---
            String imagePath = car.getImagePath();

            if (imagePath != null && !imagePath.isEmpty()) {
                // Загружаем сжатый Bitmap
                Bitmap bitmap = loadScaledBitmapFromPath(imagePath, 100); // Целевой размер 100dp

                if (bitmap != null) {
                    holder.carIcon.setImageBitmap(bitmap);
                } else {
                    // Файл не найден/невалиден
                    holder.carIcon.setImageResource(R.drawable.ic_car_outline); // Используем заглушку из CarsFragment
                }
            } else {
                // Путь пуст
                holder.carIcon.setImageResource(R.drawable.ic_car_outline);
            }
            // Устанавливаем ScaleType, чтобы маленькая иконка была по центру, а картинка - по размеру.
            holder.carIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        return view;
    }

    // --- МЕТОД ДЛЯ БЕЗОПАСНОЙ ЗАГРУЗКИ СЖАТОГО BITMAP ИЗ ФАЙЛА ---
    /**
     * Загружает Bitmap из файла, масштабируя его с использованием inSampleSize.
     * @param path Путь к файлу.
     * @param targetDpSize Целевой размер в DP (для примерного расчета масштабирования).
     * @return Масштабированный Bitmap или null в случае ошибки.
     */
    private Bitmap loadScaledBitmapFromPath(String path, int targetDpSize) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        // Получаем плотность экрана для перевода DP в пиксели
        float density = getContext().getResources().getDisplayMetrics().density;
        final int targetPixelSize = (int) (targetDpSize * density);

        try {
            // 1. Читаем размеры изображения (без загрузки в память)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int photoW = options.outWidth;
            int photoH = options.outHeight;

            // 2. Определяем коэффициент уменьшения (inSampleSize)
            int scaleFactor = 1;
            if (photoW > targetPixelSize || photoH > targetPixelSize) {
                // Используем минимальный фактор для сохранения большей части изображения
                scaleFactor = Math.min(photoW / targetPixelSize, photoH / targetPixelSize);
            }

            // 3. Загружаем изображение с уменьшением
            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            options.inPurgeable = true;

            return BitmapFactory.decodeFile(path, options);

        } catch (Exception e) {
            // Ошибка чтения/декодирования
            return null;
        }
    }

    private static class ViewHolder {
        TextView carName;
        ImageView carIcon;
    }
}