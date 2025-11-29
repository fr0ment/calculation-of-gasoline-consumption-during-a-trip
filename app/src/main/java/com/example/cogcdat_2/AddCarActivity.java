package com.example.cogcdat_2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

import java.io.FileOutputStream;
import java.io.InputStream;
// Добавьте эти импорты:
import androidx.annotation.NonNull;
import android.util.Log;
import java.io.InputStream;

public class AddCarActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private EditText etName, etDescription, etFuelType, etTankVolume, etBrand,
            etModel, etYear, etLicensePlate, etVin, etInsurancePolicy;
    private Spinner spinnerDistanceUnit, spinnerFuelUnit, spinnerFuelConsumption;
    private Button btnSave, btnBack;
    private ImageView ivCarPhoto;
    private Button btnAddPhoto;

    private DatabaseHelper dbHelper;
    private boolean isFirstLaunch;
    private String selectedImagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        dbHelper = new DatabaseHelper(this);
        isFirstLaunch = getIntent().getBooleanExtra("isFirstLaunch", true);

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        spinnerDistanceUnit = findViewById(R.id.spinnerDistanceUnit);
        spinnerFuelUnit = findViewById(R.id.spinnerFuelUnit);
        spinnerFuelConsumption = findViewById(R.id.spinnerFuelConsumption);
        etFuelType = findViewById(R.id.etFuelType);
        etTankVolume = findViewById(R.id.etTankVolume);
        etBrand = findViewById(R.id.etBrand);
        etModel = findViewById(R.id.etModel);
        etYear = findViewById(R.id.etYear);
        etLicensePlate = findViewById(R.id.etLicensePlate);
        etVin = findViewById(R.id.etVin);
        etInsurancePolicy = findViewById(R.id.etInsurancePolicy);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        ivCarPhoto = findViewById(R.id.ivCarPhoto);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
    }

    private void setupSpinners() {
        // Единицы расстояния
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this,
                R.array.distance_units, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistanceUnit.setAdapter(distanceAdapter);

        // Единицы топлива
        ArrayAdapter<CharSequence> fuelAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_units, android.R.layout.simple_spinner_item);
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuelUnit.setAdapter(fuelAdapter);

        // Расход топлива
        ArrayAdapter<CharSequence> consumptionAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_consumption_units, android.R.layout.simple_spinner_item);
        consumptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuelConsumption.setAdapter(consumptionAdapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCar();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFirstLaunch) {
                    finishAffinity();
                } else {
                    finish();
                }
            }
        });

        btnAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });
    }

    private void openImageChooser() {
        // Для Android 13+ (API 33+) используем новые разрешения
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - используем READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10-12 - не требуется разрешение для ACTION_GET_CONTENT
            launchImagePicker();
            return;
        } else {
            // Android 9 и ниже - используем READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        }

        // Если разрешения есть, запускаем выбор изображения
        launchImagePicker();
    }
    private void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    // Добавьте этот метод для обработки разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, запускаем выбор изображения
                launchImagePicker();
            } else {
                Toast.makeText(this, "Разрешение необходимо для выбора изображения", Toast.LENGTH_SHORT).show();
                // Можно показать диалог с объяснением
                showPermissionExplanation();
            }
        }
    }
    // Метод для объяснения необходимости разрешения
    private void showPermissionExplanation() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Необходимо разрешение")
                .setMessage("Для выбора фотографий автомобиля необходимо разрешение на доступ к изображениям")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Снова запрашиваем разрешение
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    private String copyImageToInternalStorage(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(uri);
            String fileName = "car_image_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(getFilesDir(), fileName);

            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ImageDebug", "Error copying image: " + e.getMessage());
            return null;
        } finally {
            // Закрываем потоки в блоке finally чтобы гарантировать их закрытие
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            selectedImagePath = uri.toString();

            Log.d("ImageDebug", "Selected URI: " + uri.toString());

            // Предоставляем постоянные права доступа к URI
            try {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                Log.d("ImageDebug", "Persistable permission taken for URI: " + uri.toString());
            } catch (Exception e) {
                Log.e("ImageDebug", "Error taking persistable permission: " + e.getMessage());
            }

            try {
                // Загружаем изображение для предпросмотра
                InputStream inputStream = getContentResolver().openInputStream(uri);
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (inputStream != null) {
                    inputStream.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap != null) {
                            ivCarPhoto.setImageBitmap(bitmap);
                            ivCarPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Toast.makeText(AddCarActivity.this, "Изображение загружено", Toast.LENGTH_SHORT).show();
                            Log.d("ImageDebug", "Bitmap dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        } else {
                            Toast.makeText(AddCarActivity.this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
                            Log.e("ImageDebug", "Bitmap is null");
                            setDefaultImage(ivCarPhoto);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ImageDebug", "Error loading image: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddCarActivity.this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                        setDefaultImage(ivCarPhoto);
                    }
                });
            }
        }
    }
    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void saveCar() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите имя автомобиля", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ImageDebug", "Saving car. Image path: " + (selectedImagePath != null ? selectedImagePath : "NULL"));

        Car car = new Car(
                name,
                etDescription.getText().toString().trim(),
                selectedImagePath,
                spinnerDistanceUnit.getSelectedItem().toString(),
                spinnerFuelUnit.getSelectedItem().toString(),
                spinnerFuelConsumption.getSelectedItem().toString(),
                etFuelType.getText().toString().trim(),
                etTankVolume.getText().toString().isEmpty() ? 0 :
                        Double.parseDouble(etTankVolume.getText().toString()),
                etBrand.getText().toString().trim(),
                etModel.getText().toString().trim(),
                etYear.getText().toString().trim(),
                etLicensePlate.getText().toString().trim(),
                etVin.getText().toString().trim(),
                etInsurancePolicy.getText().toString().trim()
        );

        long carId = dbHelper.addCar(car);
        Log.d("ImageDebug", "Car saved with ID: " + carId);

        // Проверим, что автомобиль действительно сохранился
        Car savedCar = dbHelper.getCarById((int) carId);
        if (savedCar != null) {
            Log.d("ImageDebug", "Retrieved car image path: " + savedCar.getImagePath());
            Log.d("ImageDebug", "Retrieved car name: " + savedCar.getName());
        } else {
            Log.e("ImageDebug", "Failed to retrieve saved car");
        }

        if (isFirstLaunch) {
            // Сохраняем флаг первого запуска
            android.content.SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_launch", false);
            editor.apply();

            // Переходим на главную страницу
            Intent intent = new Intent(AddCarActivity.this, MainActivity.class);
            startActivity(intent);
        }
        finish();

        Toast.makeText(this, "Автомобиль сохранен", Toast.LENGTH_SHORT).show();
    }
}