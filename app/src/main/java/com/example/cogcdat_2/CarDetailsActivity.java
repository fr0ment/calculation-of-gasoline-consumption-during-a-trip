package com.example.cogcdat_2;

import static android.app.PendingIntent.getActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;

public class CarDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Car car;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        dbHelper = new DatabaseHelper(this);

        int carId = getIntent().getIntExtra("car_id", -1);
        if (carId != -1) {
            car = dbHelper.getCarById(carId);
            if (car != null) {
                setupViews();
            }
        }
    }

    private void setupViews() {
        ImageView ivCarImage = findViewById(R.id.ivCarImage);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvBrandModel = findViewById(R.id.tvBrandModel);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvDetails = findViewById(R.id.tvDetails);

        tvName.setText(car.getName());
        tvBrandModel.setText(car.getBrand() + " " + car.getModel());
        tvDescription.setText(car.getDescription());

        // Загрузка изображения
        if (car.getImagePath() != null && !car.getImagePath().isEmpty()) {
            loadImageSafe(ivCarImage, car.getImagePath());
        } else {
            setDefaultImage(ivCarImage);
        }

        // Детальная информация
        StringBuilder details = new StringBuilder();
        details.append("Год: ").append(car.getYear()).append("\n\n");
        details.append("Тип топлива: ").append(car.getFuelType()).append("\n\n");
        details.append("Объем бака: ").append(car.getTankVolume()).append(" л\n\n");
        details.append("Номерные знаки: ").append(car.getLicensePlate()).append("\n\n");
        details.append("VIN: ").append(car.getVin()).append("\n\n");
        details.append("Страховой полис: ").append(car.getInsurancePolicy()).append("\n\n");
        details.append("Единицы расстояния: ").append(car.getDistanceUnit()).append("\n\n");
        details.append("Единицы топлива: ").append(car.getFuelUnit()).append("\n\n");
        details.append("Расход топлива: ").append(car.getFuelConsumptionUnit());

        tvDetails.setText(details.toString());
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

    private Activity getActivity() {
        return null;
    }

    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}