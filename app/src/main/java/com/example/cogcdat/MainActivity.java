package com.example.cogcdat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etDistance, etFuelConsumption, etFuelPrice;
    private TextView tvFuelNeeded, tvTripCost, tvResults;
    private Button btnSave, btnCalculate, btnHistory;

    private CalculationResult currentResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etDistance = findViewById(R.id.etDistance);
        etFuelConsumption = findViewById(R.id.etFuelConsumption);
        etFuelPrice = findViewById(R.id.etFuelPrice);

        tvFuelNeeded = findViewById(R.id.tvFuelNeeded);
        tvTripCost = findViewById(R.id.tvTripCost);
        tvResults = findViewById(R.id.tvResults);

        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);
        btnHistory = findViewById(R.id.btnHistory);
    }

    private void setupClickListeners() {
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateFuel();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCalculation();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistory();
            }
        });
    }

    private void calculateFuel() {
        String distanceStr = etDistance.getText().toString();
        String consumptionStr = etFuelConsumption.getText().toString();
        String priceStr = etFuelPrice.getText().toString();

        if (distanceStr.isEmpty() || consumptionStr.isEmpty() || priceStr.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        try {
            double distance = Double.parseDouble(distanceStr);
            double consumption = Double.parseDouble(consumptionStr);
            double price = Double.parseDouble(priceStr);

            // Расчет расхода топлива
            double fuelNeeded = (distance * consumption) / 100;
            double tripCost = fuelNeeded * price;

            currentResult = new CalculationResult(distance, consumption, price, fuelNeeded, tripCost);

            displayResults(fuelNeeded, tripCost);

        } catch (NumberFormatException e) {
            showError("Проверьте правильность введенных данных");
        }
    }

    private void displayResults(double fuelNeeded, double tripCost) {
        tvResults.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);

        tvFuelNeeded.setText(String.format("Топливо: %.2f л", fuelNeeded));
        tvTripCost.setText(String.format("Стоимость: %.2f руб", tripCost));
    }

    private void saveCalculation() {
        if (currentResult != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.addCalculation(currentResult);
            showMessage("Расчет сохранен");
        }
    }

    private void openHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    private void showError(String message) {
        // Можно использовать Snackbar или Toast
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showMessage(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}