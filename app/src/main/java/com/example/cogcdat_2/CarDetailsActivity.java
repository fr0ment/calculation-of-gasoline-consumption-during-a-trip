package com.example.cogcdat_2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Locale;

public class CarDetailsActivity extends BaseActivity {

    private DatabaseHelper dbHelper;
    private Car car;
    private String carId;

    private TextView tvName, tvDescription, tvFuelType, tvTankVolume, tvFuelUnitInfo;
    private ImageView ivCarImage;
    private Button btnEdit, btnDelete;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        dbHelper = new DatabaseHelper(this);

        carId = getIntent().getStringExtra("car_id");
        if (carId != null && !carId.isEmpty()) {
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
        if (carId != null && !carId.isEmpty()) {
            loadCarData();
        }
    }

    private void initViews() {
        ivCarImage = findViewById(R.id.ivCarImage);
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvFuelType = findViewById(R.id.tvFuelType);
        tvTankVolume = findViewById(R.id.tvTankVolume);
        tvFuelUnitInfo = findViewById(R.id.tvFuelUnitInfo); // Новая TextView для единиц топлива
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCarData() {
        car = dbHelper.getCar(carId);
        if (car != null) {
            updateUI();
        } else {
            Log.e("CarDetails", "Car not found with ID: " + carId);
            Toast.makeText(this, getString(R.string.car_not_found), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
        tvName.setText(car.getName());
        tvDescription.setText(car.getDescription());
        tvFuelType.setText(car.getFuelType());

        // Форматируем объем бака
        String localizedFuelUnit = getLocalizedFuelUnit(car);
        String tankVolumeText = String.format(Locale.getDefault(), "%.1f %s",
                car.getTankVolume(), localizedFuelUnit);
        tvTankVolume.setText(tankVolumeText);
        tvFuelUnitInfo.setText(localizedFuelUnit);

        // Загрузка изображения
        loadImageSafe(ivCarImage, car.getImagePath());
    }
    private String getLocalizedFuelUnit(Car car) {
        if (car == null) return getString(R.string.fuel_unit_liter);
        String unit = car.getFuelUnit();
        if ("л".equals(unit) || "L".equals(unit)) {
            return getString(R.string.fuel_unit_liter);
        } else if ("гал".equals(unit) || "gal".equals(unit)) {
            return getString(R.string.unit_gallon);
        } else {
            return unit; // на случай других единиц
        }
    }
    private void setupListeners() {
        btnEdit.setOnClickListener(v -> editCar());
        btnDelete.setOnClickListener(v -> deleteCar());
    }

    private void editCar() {
        Intent intent = new Intent(CarDetailsActivity.this, EditCarActivity.class);
        intent.putExtra("car_id", carId);
        startActivity(intent);
    }

    private void deleteCar() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_car_delete_confirmation, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            boolean deleted = dbHelper.deleteCar(carId);
            if (deleted) {
                Toast.makeText(this, R.string.car_deleted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.error_deleting_car, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

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