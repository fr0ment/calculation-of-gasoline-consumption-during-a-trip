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
import java.util.List;
import java.util.Locale;

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

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCarActivity.class);
            startActivity(intent);
        });

        loadCars();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCars();
    }

    private void loadCars() {
        carList = dbHelper.getAllCars();
        if (carList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new CarAdapter(carList);
            recyclerView.setAdapter(adapter);
        }
    }


    /**
     * Адаптер для списка автомобилей.
     */
    private class CarAdapter extends RecyclerView.Adapter<CarAdapter.ViewHolder> {
        private final List<Car> cars;

        public CarAdapter(List<Car> cars) {
            this.cars = cars;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Car car = cars.get(position);

            holder.tvName.setText(car.getName());
            holder.tvDescription.setText(car.getDescription());

            // Обновленная строка спецификаций: Тип топлива и Объем бака
            String specs = String.format(Locale.getDefault(), "%s • %.1f %s",
                    car.getFuelType(), car.getTankVolume(), car.getFuelUnit());
            holder.tvSpecs.setText(specs);


            // Загрузка изображения
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
            // УДАЛЕНО: tvBrandModel
            TextView tvName, tvDescription, tvSpecs;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCarImage = itemView.findViewById(R.id.ivCarImage);
                tvName = itemView.findViewById(R.id.tvCarName);
                // tvBrandModel = itemView.findViewById(R.id.tvBrandModel); // УДАЛЕНО
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvSpecs = itemView.findViewById(R.id.tvSpecs);
            }
        }
    }

    // ... (Методы loadImageSafe и setDefaultImage без изменений)
    private void loadImageSafe(ImageView imageView, String currentPhotoPath) {
        final int targetW = 500;
        final int targetH = 200;

        if (currentPhotoPath == null || currentPhotoPath.isEmpty() || !new File(currentPhotoPath).exists()) {
            setDefaultImage(imageView);
            return;
        }

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;
        if (photoW > targetW || photoH > targetH) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

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


    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // Предполагается, что dark_bg_card_image - это существующий цвет
        // imageView.setBackgroundColor(getResources().getColor(R.color.dark_bg_card_image));
    }
}