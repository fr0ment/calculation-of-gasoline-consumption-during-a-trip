package com.example.cogcdat_2;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Locale;

public class CarDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Car car;
    private int carId;

    private TextView tvName, tvDescription, tvFuelType, tvTankVolume, tvUnits;
    private ImageView ivCarImage;
    private Button btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        dbHelper = new DatabaseHelper(this);

        carId = getIntent().getIntExtra("car_id", -1);
        if (carId != -1) {
            initViews();
            setupListeners();
            loadCarData();
        } else {
            Log.e("CarDetails", "Invalid car ID provided.");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возврате из редактирования
        if (carId != -1) {
            loadCarData();
        }
    }

    private void initViews() {
        ivCarImage = findViewById(R.id.ivCarImage);
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvFuelType = findViewById(R.id.tvFuelType);
        tvTankVolume = findViewById(R.id.tvTankVolume);
        tvUnits = findViewById(R.id.tvUnits);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void loadCarData() {
        car = dbHelper.getCar(carId);
        if (car != null) {
            updateUI();
        } else {
            Log.e("CarDetails", "Car not found with ID: " + carId);
            Toast.makeText(this, "Автомобиль не найден", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
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

    private void setupListeners() {
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editCar();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCar();
            }
        });
    }

    private void editCar() {
        Intent intent = new Intent(CarDetailsActivity.this, EditCarActivity.class);
        intent.putExtra("car_id", carId);
        startActivity(intent);
        // Используйте startActivityForResult, если хотите получить результат редактирования
        // startActivityForResult(intent, 1);
    }

    private void deleteCar() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление автомобиля")
                .setMessage("Вы уверены, что хотите удалить автомобиль \"" + car.getName() + "\"?")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean deleted = dbHelper.deleteCar(carId);
                        if (deleted) {
                            Toast.makeText(CarDetailsActivity.this, "Автомобиль удален", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CarDetailsActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
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
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(getResources().getColor(R.color.background));
    }
}