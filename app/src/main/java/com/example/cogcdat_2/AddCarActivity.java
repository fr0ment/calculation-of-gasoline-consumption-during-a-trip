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
import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AddCarActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    // УДАЛЕНЫ: etBrand, etModel, etYear, etLicensePlate, etVin, etInsurancePolicy
    private EditText etName, etDescription, etFuelType, etTankVolume;
    private MaterialButton btnDistanceUnit, btnFuelUnit, btnFuelConsumption;
    private Button btnSave, btnBack, btnAddPhoto;
    private ImageView ivCarPhoto;

    private DatabaseHelper dbHelper;
    private String selectedImagePath = null;
    private boolean isFirstLaunch = false;
    
    // Выбранные единицы измерения
    private String selectedDistanceUnit = "км";
    private String selectedFuelUnit = "л";
    private String selectedFuelConsumptionUnit = "л/100км";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Используем более простой макет
        setContentView(R.layout.activity_add_car);

        dbHelper = new DatabaseHelper(this);

        isFirstLaunch = getIntent().getBooleanExtra("is_first_launch", false);

        initViews();
        setupListeners();
        setupUnitButtons();

        if (isFirstLaunch) {
            btnBack.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Добавить автомобиль");
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
        
        // Устанавливаем начальные значения на кнопках
        btnDistanceUnit.setText(selectedDistanceUnit);
        btnFuelUnit.setText(selectedFuelUnit);
        btnFuelConsumption.setText(selectedFuelConsumptionUnit);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveCar());
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

    private void setupUnitButtons() {
        // Метод оставлен для совместимости, но больше не используется
    }
    
    private void showUnitSelectorDialog(String[] units, String title, String currentSelection, UnitAdapter.OnUnitSelectedListener listener) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unit_selector, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RecyclerView rvUnits = dialogView.findViewById(R.id.rv_units);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_unit);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_unit);
        
        tvTitle.setText(title);
        
        List<String> unitsList = new ArrayList<>(Arrays.asList(units));
        // Создаем адаптер без listener, чтобы выбор не применялся сразу
        UnitAdapter adapter = new UnitAdapter(unitsList, null);
        
        // Находим позицию текущего выбора
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
            // Если ничего не выбрано, используем текущий выбор
            if (selectedUnit == null) {
                selectedUnit = currentSelection;
            }
            if (selectedUnit != null && listener != null) {
                listener.onUnitSelected(selectedUnit);
            }
            dialog.dismiss();
        });
        
        dialog.show();

        // Настройка стиля диалога
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void saveCar() {
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

        // Используем упрощенный конструктор Car
        Car car = new Car(
                name,
                etDescription.getText().toString().trim(),
                selectedImagePath,
                selectedDistanceUnit,
                selectedFuelUnit,
                selectedFuelConsumptionUnit,
                etFuelType.getText().toString().trim(),
                tankVolume
        );

        long carId = dbHelper.addCar(car);

        if (carId > 0) {
            Toast.makeText(this, "Автомобиль сохранен", Toast.LENGTH_SHORT).show();

            if (isFirstLaunch) {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("first_launch", false)
                        .apply();
                startActivity(new Intent(AddCarActivity.this, MainActivity.class));
            }
            finish();
        } else {
            Toast.makeText(this, "Ошибка сохранения в БД", Toast.LENGTH_SHORT).show();
        }
    }

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

    private void openImageChooser() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            // Для Android 9 и ниже нужно разрешение, для 10-12 обычно не нужно для ACTION_GET_CONTENT,
            // но проверка не повредит
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        }
        launchImagePicker();
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

    // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ЗДЕСЬ ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Log.d("ImageDebug", "Selected URI: " + uri.toString());

            // 1. Копируем файл во внутреннее хранилище
            String internalPath = copyImageToInternalStorage(uri);

            if (internalPath != null) {
                // 2. Сохраняем ПУТЬ К ФАЙЛУ в переменную
                selectedImagePath = internalPath;
                Log.d("ImageDebug", "Saved internal path: " + selectedImagePath);

                // 3. Отображаем с помощью безопасного метода (чтобы не было серого фона)
                setPic(ivCarPhoto, selectedImagePath);

                Toast.makeText(this, "Фото загружено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show();
                setDefaultImage(ivCarPhoto);
            }
        }
    }

    /**
     * Метод для безопасного отображения больших изображений.
     * Предотвращает ошибку OutOfMemory и "серый фон".
     */
    private void setPic(ImageView imageView, String currentPhotoPath) {
        // Получаем размеры ImageView
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Если View еще не отрисован, задаем стандартный размер для декодирования
        if (targetW == 0 || targetH == 0) {
            targetW = 500; // примерный размер
            targetH = 500;
        }

        // Читаем размеры изображения (без загрузки в память)
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Вычисляем коэффициент уменьшения
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Загружаем изображение с уменьшением
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            setDefaultImage(imageView);
        }
    }

    private String copyImageToInternalStorage(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(uri);
            // Создаем уникальное имя файла
            String fileName = "car_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(getFilesDir(), fileName);

            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096]; // Буфер 4KB
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            return outputFile.getAbsolutePath(); // Возвращаем полный путь к файлу
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
        imageView.setImageResource(R.drawable.ic_car_outline); // Убедитесь, что этот ресурс существует
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

}