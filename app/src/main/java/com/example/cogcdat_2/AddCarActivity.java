package com.example.cogcdat_2;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddCarActivity extends AppCompatActivity {
    private EditText etName, etDescription, etFuelType, etTankVolume, etBrand,
            etModel, etYear, etLicensePlate, etVin, etInsurancePolicy;
    private Spinner spinnerDistanceUnit, spinnerFuelUnit, spinnerFuelConsumption;
    private Button btnSave, btnBack;
    private DatabaseHelper dbHelper;
    private boolean isFirstLaunch;

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
                    // Выход из приложения при первом запуске
                    finishAffinity();
                } else {
                    finish();
                }
            }
        });
    }

    private void saveCar() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите имя автомобиля", Toast.LENGTH_SHORT).show();
            return;
        }

        Car car = new Car(
                name,
                etDescription.getText().toString().trim(),
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

        dbHelper.addCar(car);

        if (isFirstLaunch) {
            // Сохраняем флаг первого запуска
            SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
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