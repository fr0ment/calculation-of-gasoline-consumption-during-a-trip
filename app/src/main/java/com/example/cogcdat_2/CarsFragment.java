package com.example.cogcdat_2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
// import java.io.InputStream; // Удалили, так как не используем InputStream
import java.util.List;

public class CarsFragment extends Fragment {
    private RecyclerView recyclerView;
    private CarAdapter adapter;
    private List<Car> carList;
    private DatabaseHelper dbHelper;
    private LinearLayout emptyState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cars, container, false);

        dbHelper = new DatabaseHelper(getContext());
        recyclerView = view.findViewById(R.id.recyclerViewCars);
        emptyState = view.findViewById(R.id.emptyState);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // carList будет загружен в onResume

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCarActivity.class);
            intent.putExtra("isFirstLaunch", false);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carList = dbHelper.getAllCars();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (carList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);

            // Проверяем, существует ли адаптер, чтобы избежать создания нового
            if (adapter == null) {
                adapter = new CarAdapter(carList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(carList);
                adapter.notifyDataSetChanged();
            }
        }
    }

    // Вспомогательный метод для установки изображения по умолчанию
    private void setDefaultImage(ImageView imageView) {
        // Очистка старой картинки перед установкой новой заглушки
        imageView.setImageBitmap(null);
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    // --- ИСПРАВЛЕННЫЙ МЕТОД ЗАГРУЗКИ (СИНХРОННЫЙ, ИСПОЛЬЗУЕМ ФАЙЛ) ---

    private void loadImageSafe(ImageView imageView, String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultImage(imageView);
            return;
        }

        // Вызываем метод безопасного декодирования файла
        setPic(imageView, imagePath);
    }

    /**
     * Метод для безопасного отображения больших изображений (с сэмплированием).
     * Читает файл по абсолютному пути, масштабируя его под размер ImageView (200dp).
     */
    private void setPic(ImageView imageView, String currentPhotoPath) {
        // Размеры ImageView в item_car.xml: width=match_parent, height=200dp
        float density = getResources().getDisplayMetrics().density;
        int targetW = getResources().getDisplayMetrics().widthPixels;
        int targetH = (int) (200 * density); // Высота 200dp

        File file = new File(currentPhotoPath);
        if (!file.exists()) {
            Log.e("ImageDebug", "File not found at path: " + currentPhotoPath);
            setDefaultImage(imageView);
            return;
        }

        // Читаем размеры изображения (без загрузки в память)
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Определяем коэффициент уменьшения (inSampleSize)
        int scaleFactor = 1;
        if (photoW > targetW || photoH > targetH) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

        // Загружаем изображение с уменьшением
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                Log.e("ImageDebug", "Bitmap decoding failed (returned null).");
                setDefaultImage(imageView);
            }
        } catch (Exception e) {
            Log.e("ImageDebug", "Error decoding bitmap: " + e.getMessage());
            setDefaultImage(imageView);
        }
    }

    private class CarAdapter extends RecyclerView.Adapter<CarAdapter.ViewHolder> {
        private List<Car> cars;

        public CarAdapter(List<Car> cars) {
            this.cars = cars;
        }

        public void updateList(List<Car> newCars) {
            this.cars = newCars;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_car, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Car car = cars.get(position);

            // Очистка перед переиспользованием
            holder.ivCarImage.setImageBitmap(null);

            holder.tvName.setText(car.getName());
            holder.tvBrandModel.setText(car.getBrand() + " " + car.getModel() + " • " + car.getYear());
            holder.tvDescription.setText(car.getDescription());
            // Убедитесь, что эта строка корректна, поскольку вы не храните пробег в Car
            // Предполагаю, что вы имели в виду только тип топлива и объем бака.
            holder.tvSpecs.setText(car.getFuelType() + " • " + car.getTankVolume() + " " + car.getFuelUnit());

            // Загрузка изображения с использованием исправленного метода
            if (car.getImagePath() != null && !car.getImagePath().isEmpty()) {
                loadImageSafe(holder.ivCarImage, car.getImagePath());
            } else {
                setDefaultImage(holder.ivCarImage);
            }

            // Обработчик клика по карточке
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                intent.putExtra("car_id", car.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return cars.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCarImage;
            TextView tvName, tvBrandModel, tvDescription, tvSpecs;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCarImage = itemView.findViewById(R.id.ivCarImage);
                tvName = itemView.findViewById(R.id.tvCarName);
                tvBrandModel = itemView.findViewById(R.id.tvBrandModel);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvSpecs = itemView.findViewById(R.id.tvSpecs);
            }
        }
    }
}