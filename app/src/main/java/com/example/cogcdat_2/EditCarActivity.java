package com.example.cogcdat_2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditCarActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private TextInputEditText etName, etDescription, etFuelType, etTankVolume;
    private MaterialButton btnDistanceUnit, btnFuelUnit, btnFuelConsumption;
    private Button btnSave, btnBack, btnAddPhoto;
    private android.widget.ImageView ivCarPhoto;
    private android.widget.TextView tvTitle;

    private DatabaseHelper dbHelper;
    private Car car;
    private int carId;
    private String selectedImagePath = null;

    // Текущие выбранные единицы измерения (загружаются из БД)
    private String selectedDistanceUnit;
    private String selectedFuelUnit;
    private String selectedFuelConsumptionUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_car);

        dbHelper = new DatabaseHelper(this);

        carId = getIntent().getIntExtra("car_id", -1);
        if (carId == -1) {
            Toast.makeText(this, "Ошибка: ID автомобиля не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        car = dbHelper.getCar(carId);
        if (car == null) {
            Toast.makeText(this, "Автомобиль не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Инициализируем выбранные единицы из объекта Car
        selectedDistanceUnit = car.getDistanceUnit();
        selectedFuelUnit = car.getFuelUnit();
        selectedFuelConsumptionUnit = car.getFuelConsumptionUnit();
        selectedImagePath = car.getImagePath();

        initViews();
        setupListeners();
        loadCarData();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etFuelType = findViewById(R.id.etFuelType);
        etTankVolume = findViewById(R.id.etTankVolume);

        btnDistanceUnit = findViewById(R.id.btnDistanceUnit);
        btnFuelUnit = findViewById(R.id.btnFuelUnit);
        btnFuelConsumption = findViewById(R.id.btnFuelConsumption);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        ivCarPhoto = findViewById(R.id.ivCarPhoto);

        btnDistanceUnit.setText(selectedDistanceUnit);
        btnFuelUnit.setText(selectedFuelUnit);
        btnFuelConsumption.setText(selectedFuelConsumptionUnit);
    }

    private void loadCarData() {
        etName.setText(car.getName());
        etDescription.setText(car.getDescription());
        etFuelType.setText(car.getFuelType());

        if (car.getTankVolume() > 0) {
            etTankVolume.setText(String.format("%.2f", car.getTankVolume()).replace(',', '.'));
        }

        // Загрузка фото
        if (selectedImagePath != null && !selectedImagePath.isEmpty() && new File(selectedImagePath).exists()) {
            setPic(ivCarPhoto, selectedImagePath);
        } else {
            setDefaultImage(ivCarPhoto);
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> updateCar());
        btnBack.setOnClickListener(v -> finish());
        btnAddPhoto.setOnClickListener(v -> checkPermissionAndOpenPicker());

        btnDistanceUnit.setOnClickListener(v -> showUnitSelectorDialog(
                getResources().getStringArray(R.array.distance_units),
                "Выберите единицу расстояния",
                selectedDistanceUnit,
                unit -> {
                    selectedDistanceUnit = unit;
                    btnDistanceUnit.setText(unit);
                }));

        btnFuelUnit.setOnClickListener(v -> showUnitSelectorDialog(
                getResources().getStringArray(R.array.fuel_units),
                "Выберите единицу топлива",
                selectedFuelUnit,
                unit -> {
                    selectedFuelUnit = unit;
                    btnFuelUnit.setText(unit);
                }));

        btnFuelConsumption.setOnClickListener(v -> showUnitSelectorDialog(
                getResources().getStringArray(R.array.fuel_consumption_units),
                "Выберите единицу расхода топлива",
                selectedFuelConsumptionUnit,
                unit -> {
                    selectedFuelConsumptionUnit = unit;
                    btnFuelConsumption.setText(unit);
                }));
    }

    private void showUnitSelectorDialog(String[] units, String title, String currentSelection,
                                        UnitAdapter.OnUnitSelectedListener listener) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unit_selector, null);

        android.widget.TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RecyclerView rvUnits = dialogView.findViewById(R.id.rv_units);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_unit);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_unit);

        tvDialogTitle.setText(title);

        List<String> unitsList = new ArrayList<>(Arrays.asList(units));
        UnitAdapter adapter = new UnitAdapter(unitsList, null); // без немедленного применения

        int currentPosition = unitsList.indexOf(currentSelection);
        if (currentPosition >= 0) {
            adapter.setSelectedPosition(currentPosition);
        }

        rvUnits.setLayoutManager(new LinearLayoutManager(this));
        rvUnits.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String selectedUnit = adapter.getSelectedUnit();
            if (selectedUnit == null) {
                selectedUnit = currentSelection;
            }
            if (selectedUnit != null && listener != null) {
                listener.onUnitSelected(selectedUnit);
            }
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void updateCar() {
        String name = etName.getText().toString().trim();
        double tankVolume = 0.0;

        if (name.isEmpty()) {
            etName.setError("Введите название автомобиля.");
            return;
        }

        try {
            String volumeStr = etTankVolume.getText().toString().trim();
            volumeStr = volumeStr.replace(',', '.');
            if (!volumeStr.isEmpty()) {
                tankVolume = Double.parseDouble(volumeStr);
            }
        } catch (NumberFormatException e) {
            etTankVolume.setError("Некорректный объем бака.");
            return;
        }

        // Обновляем объект Car
        car.setName(name);
        car.setDescription(etDescription.getText().toString().trim());
        car.setFuelType(etFuelType.getText().toString().trim());
        car.setTankVolume(tankVolume);
        car.setDistanceUnit(selectedDistanceUnit);
        car.setFuelUnit(selectedFuelUnit);
        car.setFuelConsumptionUnit(selectedFuelConsumptionUnit);
        car.setImagePath(selectedImagePath);

        boolean success = dbHelper.updateCar(car);
        if (success) {
            Toast.makeText(this, "Автомобиль обновлён", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== РАБОТА С ФОТО ====================

    private void checkPermissionAndOpenPicker() {
        if (checkAndRequestPermissions()) {
            launchImagePicker();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return false;
            }
        }
        return true;
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                Toast.makeText(this, "Разрешение на чтение галереи отклонено.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String internalPath = copyImageToInternalStorage(uri);

            if (internalPath != null) {
                selectedImagePath = internalPath;
                setPic(ivCarPhoto, selectedImagePath);
                Toast.makeText(this, "Фото обновлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show();
                setDefaultImage(ivCarPhoto);
            }
        }
    }

    private void setPic(android.widget.ImageView imageView, String currentPhotoPath) {
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        if (targetW == 0 || targetH == 0) {
            targetW = 500;
            targetH = 500;
        }

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        if (scaleFactor < 1) scaleFactor = 1;

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        } else {
            setDefaultImage(imageView);
        }
    }

    private String copyImageToInternalStorage(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            String fileName = "car_edit_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(getFilesDir(), fileName);

            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ImageDebug", "Error copying image: " + e.getMessage());
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}
        }
    }

    private void setDefaultImage(android.widget.ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
    }
}