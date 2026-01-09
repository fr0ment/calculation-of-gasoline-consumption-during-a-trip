package com.example.cogcdat_2;

import android.content.Intent;
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
    private int tripId;

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

        tripId = getIntent().getIntExtra("trip_id", -1);
        if (tripId != -1) {
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
        if (tripId != -1) {
            loadTripData();
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

    private Trip getTripById(int id) {
        return dbHelper.getTrip(id);
    }

    private void updateUI() {
        tvTripName.setText(trip.getName());
        tvTripDateTime.setText(formatDateTime(trip.getStartDateTime()));

        Car car = dbHelper.getCar(trip.getCarId());
        String distanceUnit = car != null ? car.getDistanceUnit() : "км";
        String fuelUnit = car != null ? car.getFuelUnit() : "л";
        String consumptionUnit = car != null ? car.getFuelConsumptionUnit() : "л/100км";

        tvTripDistance.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getDistance(), distanceUnit));
        tvTripFuelSpent.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelSpent(), fuelUnit));
        tvTripFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelConsumption(), consumptionUnit));
        tvTripDuration.setText(formatDuration(trip.getStartDateTime(), trip.getEndDateTime()));
    }

    private void setupListeners() {
        // Кнопка "Редактировать"
        btnEditTrip.setOnClickListener(v -> editTrip());

        // Кнопка "Удалить"
        btnDeleteTrip.setOnClickListener(v -> deleteTrip());
    }

    private void editTrip() {
        Intent intent = new Intent(TripDetailsActivity.this, EditTripActivity.class);
        intent.putExtra("trip_id", tripId);
        startActivity(intent);
    }

    private void deleteTrip() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_delete_confirmation, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
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

        // Устанавливаем прозрачный фон для поддержки закругленных углов из drawable
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
