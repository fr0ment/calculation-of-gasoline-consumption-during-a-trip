package com.example.cogcdat_2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import java.io.InputStream;
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
        carList = dbHelper.getAllCars();

        updateEmptyState();

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddCarActivity.class);
                intent.putExtra("isFirstLaunch", false);
                startActivity(intent);
            }
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
            adapter = new CarAdapter(carList);
            recyclerView.setAdapter(adapter);
        }
    }

    // Вспомогательный метод для установки изображения по умолчанию
    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void loadImageSafe(ImageView imageView, String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultImage(imageView);
            return;
        }

        new Thread(() -> {
            try {
                Uri uri = Uri.parse(imagePath);
                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (inputStream != null) {
                    inputStream.close();
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Log.d("ImageDebug", "Image loaded successfully");
                        } else {
                            setDefaultImage(imageView);
                            Log.e("ImageDebug", "Failed to load image");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("ImageDebug", "Error in loadImageSafe: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> setDefaultImage(imageView));
                }
            }
        }).start();
    }

    private class CarAdapter extends RecyclerView.Adapter<CarAdapter.ViewHolder> {
        private List<Car> cars;

        public CarAdapter(List<Car> cars) {
            this.cars = cars;
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
            holder.tvName.setText(car.getName());
            holder.tvBrandModel.setText(car.getBrand() + " " + car.getModel() + " • " + car.getYear());
            holder.tvDescription.setText(car.getDescription());
            holder.tvSpecs.setText(car.getFuelType() + " • " + car.getTankVolume() + " л");

            // Загрузка изображения с отладкой
            Log.d("ImageDebug", "Loading image for car: " + car.getName());
            Log.d("ImageDebug", "Image path: " + car.getImagePath());

            if (car.getImagePath() != null && !car.getImagePath().isEmpty()) {
                loadImageSafe(holder.ivCarImage, car.getImagePath());
            } else {
                setDefaultImage(holder.ivCarImage);
            }

            // Обработчик клика по карточке
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                    intent.putExtra("car_id", car.getId());
                    startActivity(intent);
                }
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