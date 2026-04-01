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

import com.example.cogcdat_2.sync.SyncManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditCarActivity extends BaseActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private TextInputEditText etName, etDescription, etFuelType, etTankVolume;
    private MaterialButton btnFuelUnit; // Только единицы топлива
    private Button btnSave, btnBack, btnAddPhoto;
    private android.widget.ImageView ivCarPhoto;
    private android.widget.TextView tvTitle;

    private DatabaseHelper dbHelper;
    private Car car;
    private String carId;
    private String selectedImagePath = null;

    // Текущие выбранные единицы измерения (только топливо)
    private String selectedFuelUnit;
    private String oldImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_car);

        dbHelper = new DatabaseHelper(this);

        carId = getIntent().getStringExtra("car_id");
        if (carId == null || carId.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_car_id_not_provided), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        car = dbHelper.getCar(carId);
        if (car == null) {
            Toast.makeText(this, getString(R.string.car_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Инициализируем выбранные единицы из объекта Car
        selectedFuelUnit = car.getFuelUnit();
        selectedImagePath = car.getImagePath();

        initViews();
        setupListeners();
        loadCarData();
    }
    private String getLocalizedFuelUnit(Car car) {
        if (car == null) return getString(R.string.fuel_unit_liter);
        return BaseActivity.getLocalizedFuelUnit(this, car.getFuelUnit());
    }
    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.edit_car_title));
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etFuelType = findViewById(R.id.etFuelType);
        etTankVolume = findViewById(R.id.etTankVolume);

        btnFuelUnit = findViewById(R.id.btnFuelUnit);
        // Скрываем ненужные кнопки
        MaterialButton btnDistanceUnit = findViewById(R.id.btnDistanceUnit);
        MaterialButton btnFuelConsumption = findViewById(R.id.btnFuelConsumption);
        btnDistanceUnit.setVisibility(View.GONE);
        btnFuelConsumption.setVisibility(View.GONE);

        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        ivCarPhoto = findViewById(R.id.ivCarPhoto);

        updateFuelUnitButtonText();
    }
    private void updateFuelUnitButtonText() {
        String displayText;
        if ("л".equalsIgnoreCase(selectedFuelUnit) || "L".equalsIgnoreCase(selectedFuelUnit)) {
            displayText = getString(R.string.fuel_unit_liter);
        } else if ("гал".equalsIgnoreCase(selectedFuelUnit) || "GAL".equalsIgnoreCase(selectedFuelUnit)) {
            displayText = getString(R.string.unit_gallon);
        } else if ("кВтч".equalsIgnoreCase(selectedFuelUnit) || "kWh".equalsIgnoreCase(selectedFuelUnit)) {
            displayText = getString(R.string.unit_kwh);
        } else if ("м³".equalsIgnoreCase(selectedFuelUnit) || "m³".equalsIgnoreCase(selectedFuelUnit)) {
            displayText = getString(R.string.unit_m3);
        } else {
            displayText = selectedFuelUnit;
        }
        btnFuelUnit.setText(displayText);
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
        oldImagePath = car.getImagePath();
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> updateCar());
        btnBack.setOnClickListener(v -> finish());
        btnAddPhoto.setOnClickListener(v -> checkPermissionAndOpenPicker());

        btnFuelUnit.setOnClickListener(v -> showUnitSelectorDialog(
                getResources().getStringArray(R.array.fuel_units),
                getString(R.string.Select_the_fuel_unit),
                selectedFuelUnit,
                unit -> {
                    // Нормализуем выбранную единицу
                    if (unit.toLowerCase().contains("гал") || unit.toLowerCase().contains("gal")) {
                        selectedFuelUnit = "GAL";
                    } else if (unit.toLowerCase().contains("квтч") || unit.toLowerCase().contains("kwh")) {
                        selectedFuelUnit = "kWh";
                    } else if (unit.toLowerCase().contains("м³") || unit.toLowerCase().contains("m³")) {
                        selectedFuelUnit = "m³";
                    } else {
                        selectedFuelUnit = "L";
                    }
                    updateFuelUnitButtonText();
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
        UnitAdapter adapter = new UnitAdapter(unitsList, null);

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
            etName.setError(getString(R.string.error_enter_car_name));
            return;
        }

        try {
            String volumeStr = etTankVolume.getText().toString().trim();
            volumeStr = volumeStr.replace(',', '.');
            if (!volumeStr.isEmpty()) {
                tankVolume = Double.parseDouble(volumeStr);
            }
        } catch (NumberFormatException e) {
            etTankVolume.setError(getString(R.string.error_invalid_tank_volume));
            return;
        }

        int oldVersion = car.getImageVersion();

        // Обновляем объект Car
        car.setName(name);
        car.setDescription(etDescription.getText().toString().trim());
        car.setFuelType(etFuelType.getText().toString().trim());
        car.setTankVolume(tankVolume);
        car.setFuelUnit(selectedFuelUnit);
        car.setImagePath(selectedImagePath);

        boolean success = dbHelper.updateCar(car);
        if (success) {
            // Если фото изменилось
            if (selectedImagePath != null && !selectedImagePath.isEmpty() &&
                    !selectedImagePath.equalsIgnoreCase(oldImagePath)) {

                // Увеличиваем версию!
                int newVersion = oldVersion + 1;
                car.setImageVersion(newVersion);
                dbHelper.updateCar(car);

                File imageFile = new File(selectedImagePath);
                if (imageFile.exists()) {
                    SyncManager.getInstance(this).uploadCarImage(car.getId(), imageFile);
                }
                Log.d("EditCar", "Image version increased from " + oldVersion + " to " + newVersion);
            }

            Toast.makeText(this, R.string.car_updated, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_updating_car, Toast.LENGTH_SHORT).show();
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
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                Toast.makeText(this, getString(R.string.error_gallery_permission_denied), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.photo_updated, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.error_saving_photo), Toast.LENGTH_SHORT).show();
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