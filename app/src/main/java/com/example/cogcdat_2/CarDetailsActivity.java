package com.example.cogcdat_2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CarDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Car car;

    // УДАЛЕНЫ: tvBrandModel, tvYearPlate, tvVin, tvInsurance
    private TextView tvName, tvDescription, tvFuelType, tvTankVolume, tvUnits;
    private ImageView ivCarImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        dbHelper = new DatabaseHelper(this);

        int carId = getIntent().getIntExtra("car_id", -1);
        if (carId != -1) {
            car = dbHelper.getCar(carId);
            if (car != null) {
                setupViews();
            } else {
                Log.e("CarDetails", "Car not found with ID: " + carId);
                finish();
            }
        } else {
            Log.e("CarDetails", "Invalid car ID provided.");
            finish();
        }
    }

    private void setupViews() {
        ivCarImage = findViewById(R.id.ivCarImage);
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvFuelType = findViewById(R.id.tvFuelType);
        tvTankVolume = findViewById(R.id.tvTankVolume);
        tvUnits = findViewById(R.id.tvUnits);

        tvName.setText(car.getName());
        tvDescription.setText(car.getDescription());
        tvFuelType.setText(car.getFuelType());

        // Форматируем объем бака
        String tankVolumeText = String.format(Locale.getDefault(), "%.1f %s", car.getTankVolume(), car.getFuelUnit());
        tvTankVolume.setText(tankVolumeText);

        // Форматируем единицы измерения
        String unitsText = String.format("Расстояние: %s\nТопливо: %s\nРасход: %s",
                car.getDistanceUnit(), car.getFuelUnit(), car.getFuelConsumptionUnit());
        tvUnits.setText(unitsText);


        // Загрузка изображения
        loadImageSafe(ivCarImage, car.getImagePath());
    }

    /**
     * Метод для безопасной загрузки изображения из локального пути.
     */
    private void loadImageSafe(ImageView imageView, String currentPhotoPath) {
        final int targetW = 500;
        final int targetH = 250;

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
        // Убедитесь, что ic_car_outline существует
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // Предполагается, что dark_bg - это существующий цвет
        imageView.setBackgroundColor(getResources().getColor(R.color.background));
    }
}