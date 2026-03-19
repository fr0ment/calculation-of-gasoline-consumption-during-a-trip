package com.example.cogcdat_2;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Trip trip;
    private String tripId;
    private String currentUserId;
    private UserSettings userSettings;

    private TextView tvTripName, tvTripDateTime, tvTripDistance, tvTripFuelSpent, tvTripFuelConsumption, tvTripDuration;
    private Button btnEditTrip, btnDeleteTrip;
    private Button btnBackTrip;

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_TIME_FORMAT = new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm", new Locale("ru", "RU"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        dbHelper = new DatabaseHelper(this);

        // Получаем ID текущего пользователя и его настройки
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
        } else {
            userSettings = new UserSettings(); // Настройки по умолчанию
        }

        tripId = getIntent().getStringExtra("trip_id");
        if (tripId != null && !tripId.isEmpty()) {
            initViews();
            setupListeners();
            loadTripData();
        } else {
            Log.e("TripDetails", "Invalid trip ID provided.");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возврате из редактирования
        if (tripId != null && !tripId.isEmpty()) {
            loadTripData();
        }
        // Обновляем настройки пользователя
        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
            // Обновляем UI с новыми настройками
            if (trip != null) {
                updateUI();
            }
        }
    }

    private void initViews() {
        tvTripName = findViewById(R.id.tvTripName);
        tvTripDateTime = findViewById(R.id.tvTripDateTime);
        tvTripDistance = findViewById(R.id.tvTripDistance);
        tvTripFuelSpent = findViewById(R.id.tvTripFuelSpent);
        tvTripFuelConsumption = findViewById(R.id.tvTripFuelConsumption);
        tvTripDuration = findViewById(R.id.tvTripDuration);
        btnEditTrip = findViewById(R.id.btnEditTrip);
        btnDeleteTrip = findViewById(R.id.btnDeleteTrip);
        btnBackTrip = findViewById(R.id.btnBackTrip);

        btnBackTrip.setOnClickListener(v -> finish());
    }

    private void loadTripData() {
        trip = getTripById(tripId);
        if (trip != null) {
            updateUI();
        } else {
            Log.e("TripDetails", "Trip not found with ID: " + tripId);
            Toast.makeText(this, "Поездка не найдена", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Trip getTripById(String id) {
        return dbHelper.getTrip(id);
    }

    private void updateUI() {
        tvTripName.setText(trip.getName());
        tvTripDateTime.setText(formatDateTime(trip.getStartDateTime()));

        Car car = dbHelper.getCar(trip.getCarId());
        String fuelUnit = car != null ? car.getFuelUnit() : "л";

        // Получаем единицы расстояния из настроек пользователя
        DistanceUnit distanceUnit = userSettings.getDistanceUnit();
        String distanceUnitSymbol = distanceUnit.getDisplayName();

        // Конвертируем расстояние в выбранные единицы
        double displayDistance = trip.getDistanceInUnit(distanceUnit);

        // Получаем расход топлива в нужных единицах
        double consumption = trip.getFuelConsumption();
        String consumptionUnit;

        if (distanceUnit == DistanceUnit.MI) {
            consumption = consumption * 1.60934; // л/100км -> л/100миль
            consumptionUnit = "л/100миль";
        } else {
            consumptionUnit = "л/100км";
        }

        tvTripDistance.setText(String.format(Locale.getDefault(), "%.2f %s",
                displayDistance, distanceUnitSymbol));
        tvTripFuelSpent.setText(String.format(Locale.getDefault(), "%.2f %s",
                trip.getFuelSpent(), fuelUnit));
        tvTripFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s",
                consumption, consumptionUnit));
        tvTripDuration.setText(formatDuration(trip.getStartDateTime(), trip.getEndDateTime()));
    }

    private void setupListeners() {
        btnEditTrip.setOnClickListener(v -> editTrip());
        btnDeleteTrip.setOnClickListener(v -> deleteTrip());
    }

    private void editTrip() {
        Intent intent = new Intent(TripDetailsActivity.this, EditTripActivity.class);
        intent.putExtra("trip_id", tripId);
        startActivity(intent);
    }

    private void deleteTrip() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_delete_confirmation, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dbHelper.deleteTrip(tripId);
            Toast.makeText(TripDetailsActivity.this, "Поездка удалена", Toast.LENGTH_SHORT).show();
            finish();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private String formatDateTime(String dbDateTime) {
        try {
            Date date = DB_DATE_FORMAT.parse(dbDateTime);
            return DISPLAY_DATE_TIME_FORMAT.format(date);
        } catch (ParseException e) {
            return dbDateTime;
        }
    }

    private String formatDuration(String start, String end) {
        try {
            Date startDate = DB_DATE_FORMAT.parse(start);
            Date endDate = DB_DATE_FORMAT.parse(end);
            long durationMs = endDate.getTime() - startDate.getTime();
            long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
            return String.format(Locale.getDefault(), "%d ч %02d мин", hours, minutes);
        } catch (Exception e) {
            return "--";
        }
    }
}