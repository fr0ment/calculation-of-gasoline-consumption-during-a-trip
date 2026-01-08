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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class EditCarActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private EditText etName, etDescription, etFuelType, etTankVolume;
    private Spinner spinnerDistanceUnit, spinnerFuelUnit, spinnerFuelConsumption;
    private Button btnSave, btnBack, btnAddPhoto;
    private ImageView ivCarPhoto;
    private TextView tvTitle;

    private DatabaseHelper dbHelper;
    private Car car;
    private int carId;
    private String selectedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        dbHelper = new DatabaseHelper(this);
        carId = getIntent().getIntExtra("car_id", -1);

        if (carId != -1) {
            car = dbHelper.getCar(carId);
            if (car == null) {
                Toast.makeText(this, "Автомобиль не найден", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Ошибка: ID автомобиля не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        loadCarData();
        setupListeners();
    }

    private void initViews() {
        // Установка заголовка
        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Редактировать автомобиль");

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etFuelType = findViewById(R.id.etFuelType);
        etTankVolume = findViewById(R.id.etTankVolume);

        spinnerDistanceUnit = findViewById(R.id.spinnerDistanceUnit);
        spinnerFuelUnit = findViewById(R.id.spinnerFuelUnit);
        spinnerFuelConsumption = findViewById(R.id.spinnerFuelConsumption);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        ivCarPhoto = findViewById(R.id.ivCarPhoto);
    }

    private void setupSpinners() {
        // Настройка спиннера для единиц расстояния
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this,
                R.array.distance_units, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistanceUnit.setAdapter(distanceAdapter);

        // Настройка спиннера для единиц топлива
        ArrayAdapter<CharSequence> fuelAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_units, android.R.layout.simple_spinner_item);
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuelUnit.setAdapter(fuelAdapter);

        // Настройка спиннера для единиц расхода топлива
        ArrayAdapter<CharSequence> consumptionAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_consumption_units, android.R.layout.simple_spinner_item);
        consumptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuelConsumption.setAdapter(consumptionAdapter);
    }

    private void loadCarData() {
        if (car != null) {
            // Устанавливаем значения из автомобиля
            etName.setText(car.getName());
            etDescription.setText(car.getDescription());
            etFuelType.setText(car.getFuelType());

            // Используем Locale.US для точки вместо запятой
            etTankVolume.setText(String.format(Locale.US, "%.1f", car.getTankVolume()));

            // Устанавливаем значения спиннеров
            setSpinnerToValue(spinnerDistanceUnit, car.getDistanceUnit());
            setSpinnerToValue(spinnerFuelUnit, car.getFuelUnit());
            setSpinnerToValue(spinnerFuelConsumption, car.getFuelConsumptionUnit());

            // Загружаем существующее изображение
            selectedImagePath = car.getImagePath();
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                File imageFile = new File(selectedImagePath);
                if (imageFile.exists()) {
                    setPic(ivCarPhoto, selectedImagePath);
                } else {
                    setDefaultImage(ivCarPhoto);
                }
            } else {
                setDefaultImage(ivCarPhoto);
            }
        }
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        if (value == null || spinner.getAdapter() == null) {
            return;
        }

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (spinner.getAdapter().getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> updateCar());
        btnBack.setOnClickListener(v -> finish());
        btnAddPhoto.setOnClickListener(v -> checkPermissionAndOpenPicker());
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
            // Заменяем запятую на точку для корректного парсинга
            volumeStr = volumeStr.replace(',', '.');
            if (!volumeStr.isEmpty()) {
                tankVolume = Double.parseDouble(volumeStr);
            }
        } catch (NumberFormatException e) {
            etTankVolume.setError("Некорректный объем бака.");
            return;
        }

        // Проверяем, что спиннеры не null и имеют выбранные элементы
        if (spinnerDistanceUnit.getSelectedItem() == null ||
                spinnerFuelUnit.getSelectedItem() == null ||
                spinnerFuelConsumption.getSelectedItem() == null) {
            Toast.makeText(this, "Пожалуйста, выберите все единицы измерения", Toast.LENGTH_SHORT).show();
            return;
        }

        String distanceUnit = spinnerDistanceUnit.getSelectedItem().toString();
        String fuelUnit = spinnerFuelUnit.getSelectedItem().toString();
        String fuelConsumptionUnit = spinnerFuelConsumption.getSelectedItem().toString();

        // Обновляем объект автомобиля
        car.setName(name);
        car.setDescription(etDescription.getText().toString().trim());
        car.setFuelType(etFuelType.getText().toString().trim());
        car.setTankVolume(tankVolume);
        car.setDistanceUnit(distanceUnit);
        car.setFuelUnit(fuelUnit);
        car.setFuelConsumptionUnit(fuelConsumptionUnit);
        car.setImagePath(selectedImagePath); // Обновляем путь к изображению

        boolean updated = dbHelper.updateCar(car);
        if (updated) {
            Toast.makeText(this, "Автомобиль обновлен", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
        }
    }

    // Методы для работы с изображениями (аналогично AddCarActivity)

    private void checkPermissionAndOpenPicker() {
        if (checkAndRequestPermissions()) launchImagePicker();
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ использует READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return false;
            }
        } else {
            // До Android 13 использует READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return false;
            }
        }
        return true;
    }

    private void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
            Log.d("ImageDebug", "Selected URI: " + uri.toString());

            // Копируем файл во внутреннее хранилище
            String internalPath = copyImageToInternalStorage(uri);

            if (internalPath != null) {
                // Сохраняем ПУТЬ К ФАЙЛУ
                selectedImagePath = internalPath;
                Log.d("ImageDebug", "Saved internal path: " + selectedImagePath);

                // Отображаем изображение
                setPic(ivCarPhoto, selectedImagePath);

                Toast.makeText(this, "Фото обновлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show();
                setDefaultImage(ivCarPhoto);
            }
        }
    }

    /**
     * Метод для безопасного отображения изображений.
     */
    private void setPic(ImageView imageView, String currentPhotoPath) {
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
                setDefaultImage(imageView);
            }
        } catch (Exception e) {
            Log.e("ImageDebug", "Error decoding bitmap: " + e.getMessage());
            setDefaultImage(imageView);
        }
    }

    private String copyImageToInternalStorage(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(uri);
            // Создаем уникальное имя файла
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
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_car_outline);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}