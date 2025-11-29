package com.example.cogcdat_2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

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
            } else {
                Log.e("CarDetails", "Car not found with ID: " + carId);
                finish(); // Закрываем, если не нашли машину
            }
        } else {
            Log.e("CarDetails", "Invalid car ID provided.");
            finish();
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

        // Загрузка изображения: используем локальный путь (не URI)
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

        // Вызываем безопасный метод декодирования файла
        setPic(imageView, imagePath);
    }

    /**
     * Метод для безопасного отображения больших изображений (с сэмплированием).
     */
    private void setPic(ImageView imageView, String currentPhotoPath) {
        // Получаем размеры ImageView
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Если View еще не отрисован, используем размер из XML (250dp)
        if (targetW <= 0) {
            float density = getResources().getDisplayMetrics().density;
            targetW = getResources().getDisplayMetrics().widthPixels; // Ширина экрана
            targetH = (int) (250 * density); // Высота 250dp
        }

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


    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}