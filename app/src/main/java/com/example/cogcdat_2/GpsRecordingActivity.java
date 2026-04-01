package com.example.cogcdat_2;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GpsRecordingActivity extends BaseActivity {

    private static final String TAG = "GpsRecordingActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    // UI элементы
    private LinearLayout layoutPreStart, layoutRecording, layoutControls;
    private EditText etTripName, etInitialFuel;
    private TextView tvDistanceValue, tvDurationValue, tvFuelRechargedValue, tvUnitInfo;
    private Button btnStart, btnPauseResume, btnRefuel, btnStop, btnCancel;
    private TextView tvCurrentStatus;

    private DatabaseHelper dbHelper;
    private Car selectedCar;
    private String carId;
    private String currentUserId;
    private UserSettings userSettings;

    // Состояние поездки
    private boolean isRecording = false;
    private double initialFuelLevel = 0.0;

    // --- Observer для LiveData ---
    private final Observer<TripRecordingData> tripUpdateObserver = tripData -> {
        if (tripData != null) {
            if (tripData.getDistance() < 0) {
                return;
            }

            // Получаем единицы расстояния из настроек пользователя
            DistanceUnit distanceUnit = userSettings != null ?
                    userSettings.getDistanceUnit() : DistanceUnit.KM;

            // Конвертируем расстояние для отображения
            double displayDistance = tripData.getDistance();
            if (distanceUnit == DistanceUnit.MI) {
                displayDistance = tripData.getDistance() / 1.60934; // км -> мили
            }

            // Обновление статистики
            tvDistanceValue.setText(String.format(Locale.getDefault(), "%.1f", displayDistance));
            tvDurationValue.setText(formatDuration(tripData.getDurationMs()));
            tvFuelRechargedValue.setText(String.format(Locale.getDefault(), "%.1f", tripData.getFuelRecharged()));

            // Обновление кнопки Пауза/Возобновить
            if (tripData.isPaused()) {
                btnPauseResume.setText(getString(R.string.btn_resume));
                tvCurrentStatus.setText(getString(R.string.status_recording_paused));
            } else {
                btnPauseResume.setText(getString(R.string.btn_pause));
                tvCurrentStatus.setText(getString(R.string.status_recording_active));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_recording);

        new Handler(Looper.getMainLooper()).post(() -> {
            if (etTripName != null) hideKeyboard(etTripName);
        });

        // Получение ID автомобиля
        carId = getIntent().getStringExtra("car_id");
        if (carId == null || carId.isEmpty()) {
            Toast.makeText(this, R.string.error_car_not_selected, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        selectedCar = dbHelper.getCar(carId);

        if (selectedCar == null) {
            Toast.makeText(this, R.string.car_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Получаем ID текущего пользователя и его настройки
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
        } else {
            userSettings = new UserSettings(); // Настройки по умолчанию
        }

        // Инициализация репозитория
        TripRecordingRepository.initialize(getApplicationContext());

        initViews();
        setupListeners();
        updateUnitInfo();

        // Восстановление состояния, если запись уже идет
        if (TripRecordingRepository.getInstance().isRecording()) {
            isRecording = true;
            layoutPreStart.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            layoutRecording.setVisibility(View.VISIBLE);
            layoutControls.setVisibility(View.VISIBLE);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    showCancelConfirmationDialog();
                }
            });
        }

        // Подписка на LiveData
        TripRecordingRepository.getInstance().getTripUpdates().observe(this, tripUpdateObserver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        carId = intent.getStringExtra("car_id") != null ? intent.getStringExtra("car_id") : carId;
        selectedCar = dbHelper.getCar(carId);

        // Обновляем настройки пользователя
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);
        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
        }

        updateUnitInfo();

        if (TripRecordingRepository.getInstance().isRecording()) {
            isRecording = true;
            layoutPreStart.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            layoutRecording.setVisibility(View.VISIBLE);
            layoutControls.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        layoutPreStart = findViewById(R.id.layout_pre_start);
        etTripName = findViewById(R.id.et_trip_name);
        etInitialFuel = findViewById(R.id.et_initial_fuel);
        tvUnitInfo = findViewById(R.id.tv_unit_info);
        btnStart = findViewById(R.id.btn_start_recording);

        layoutRecording = findViewById(R.id.layout_recording);
        tvCurrentStatus = findViewById(R.id.tv_current_status);

        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvDurationValue = findViewById(R.id.tv_duration_value);
        tvFuelRechargedValue = findViewById(R.id.tv_fuel_recharged_value);

        layoutControls = findViewById(R.id.layout_controls);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnRefuel = findViewById(R.id.btn_refuel);
        btnStop = findViewById(R.id.btn_stop_recording);
        btnCancel = findViewById(R.id.btn_cancel_recording);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> {
            handleStartRecording();
            hideKeyboard(v);
        });
        btnPauseResume.setOnClickListener(v -> {
            handlePauseResume();
            hideKeyboard(v);
        });
        btnRefuel.setOnClickListener(v -> {
            if (isRecording) {
                showRefuelDialog();
            }
            hideKeyboard(v);
        });
        btnCancel.setOnClickListener(v -> {
            showCancelConfirmationDialog();
            hideKeyboard(v);
        });
        btnStop.setOnClickListener(v -> {
            if (isRecording) {
                showSaveTripDialog();
            }
            hideKeyboard(v);
        });
    }

    private void showCancelConfirmationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_gps_recording, null);

        Button btnNo = dialogView.findViewById(R.id.btn_cancel_final_fuel);
        Button btnYes = dialogView.findViewById(R.id.btn_save_trip);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            stopRecordingService();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void handleStartRecording() {
        String initialFuelStr = etInitialFuel.getText().toString().trim();
        if (initialFuelStr.isEmpty()) {
            etInitialFuel.setError(getString(R.string.enter_initial_fuel));
            return;
        }
        try {
            initialFuelLevel = Double.parseDouble(initialFuelStr);
            if (initialFuelLevel < 0) {
                etInitialFuel.setError(getString(R.string.error_value_must_be_non_negative));
                return;
            }
        } catch (NumberFormatException e) {
            etInitialFuel.setError(getString(R.string.error_invalid_numeric_value));
            return;
        }

        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        TripRecordingRepository.getInstance().setInitialFuelLevel(initialFuelLevel);

        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        serviceIntent.putExtra("car_id", carId);
        startService(serviceIntent);

        isRecording = true;
        layoutPreStart.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
        layoutRecording.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> hideKeyboard(layoutRecording), 100);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCancelConfirmationDialog();
            }
        });

        Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard(View view) {
        if (view == null) return;

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (etTripName != null) etTripName.clearFocus();
        if (etInitialFuel != null) etInitialFuel.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        getWindow().getDecorView().clearFocus();
    }

    private void showRefuelDialog() {
        String fuelUnit = selectedCar.getFuelUnit();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_refuel, null);
        builder.setView(dialogView);

        TextView tvRefuelUnit = dialogView.findViewById(R.id.tv_refuel_unit);
        EditText etRefuelAmount = dialogView.findViewById(R.id.et_refuel_amount);
        Button btnAddRefuel = dialogView.findViewById(R.id.btn_add_refuel);
        Button btnCancelRefuel = dialogView.findViewById(R.id.btn_cancel_refuel);

        tvRefuelUnit.setText(String.format(Locale.getDefault(), getString(R.string.unit_measurement), fuelUnit));

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnAddRefuel.setOnClickListener(v -> {
            String amountStr = etRefuelAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                etRefuelAmount.setError(getString(R.string.enter_fuel_amount));
                return;
            }
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    etRefuelAmount.setError(getString(R.string.error_amount_must_be_positive));
                    return;
                }

                TripRecordingRepository.getInstance().addRefuel(amount);

                Toast.makeText(this, getString(R.string.fuel_added, amount, fuelUnit), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etRefuelAmount.setError(getString(R.string.error_invalid_numeric_value));
            }
        });

        btnCancelRefuel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showSaveTripDialog() {
        if (!TripRecordingRepository.getInstance().isRecording()) {
            Toast.makeText(this, getString(R.string.recording_not_active), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String fuelUnit = selectedCar.getFuelUnit();
        double maxPossibleFuel = initialFuelLevel + TripRecordingRepository.getInstance().getTotalFuelRecharged();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_trip, null);
        builder.setView(dialogView);

        TextView tvSummary = dialogView.findViewById(R.id.tv_final_fuel_summary);
        TextView tvUnit = dialogView.findViewById(R.id.tv_final_fuel_unit);
        EditText etFinalFuel = dialogView.findViewById(R.id.et_final_fuel);
        Button btnSave = dialogView.findViewById(R.id.btn_save_trip);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_final_fuel);

        tvUnit.setText(String.format(Locale.getDefault(), getString(R.string.unit_measurement_remaining_fuel), fuelUnit));
        tvSummary.setText(String.format(Locale.getDefault(), getString(R.string.enter_remaining_fuel_prompt), fuelUnit, maxPossibleFuel, fuelUnit));

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String finalFuelStr = etFinalFuel.getText().toString().trim();
            if (finalFuelStr.isEmpty()) {
                etFinalFuel.setError(getString(R.string.enter_remaining_fuel));
                return;
            }
            try {
                double finalFuelLevel = Double.parseDouble(finalFuelStr);
                if (finalFuelLevel < 0) {
                    etFinalFuel.setError(getString(R.string.error_value_must_be_non_negative));
                    return;
                }

                if (finalFuelLevel > maxPossibleFuel) {
                    etFinalFuel.setError(getString(R.string.error_remaining_exceeds_max, maxPossibleFuel, fuelUnit));
                    return;
                }

                saveTripToDatabase(finalFuelLevel);
                stopRecordingService();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etFinalFuel.setError(getString(R.string.error_invalid_numeric_value));
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void saveTripToDatabase(double finalFuelLevel) {
        TripRecordingRepository repository = TripRecordingRepository.getInstance();

        double distance = repository.getCurrentDistanceKm(); // Всегда в километрах
        double totalFuelRecharged = repository.getTotalFuelRecharged();
        double initialFuel = repository.getInitialFuelLevel();
        String name = etTripName.getText().toString().trim();
        String startDateTime = repository.getStartDateTime();
        String endDateTime = repository.getCurrentDateTime();

        if (initialFuel <= 0) {
            Log.e(TAG, "Initial fuel not set correctly! Value: " + initialFuel);
        }

        double fuelSpent = (initialFuel + totalFuelRecharged) - finalFuelLevel;

        String tripName = name.isEmpty() ? getString(R.string.Trip) + DISPLAY_DATE_FORMAT.format(new Date()) : name;

        // Создаем поездку (расстояние уже в километрах)
        Trip newTrip = new Trip(
                carId,
                tripName,
                startDateTime,
                endDateTime,
                distance, // Всегда в километрах!
                fuelSpent
        );

        String result = dbHelper.addTrip(newTrip);

        if (result != null && !result.isEmpty()) {
            Toast.makeText(this, R.string.trip_saved_successfully, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.error_saving_trip, Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecordingService() {
        TripRecordingRepository.getInstance().reset();
        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        stopService(serviceIntent);
        isRecording = false;
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard(getCurrentFocus());
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (TripRecordingRepository.getInstance() != null) {
            TripRecordingRepository.getInstance().getTripUpdates().removeObserver(tripUpdateObserver);
        }
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        return String.format(Locale.getDefault(), getString(R.string.duration_format_full), hours, minutes, seconds);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted_start, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.error_gps_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handlePauseResume() {
        Intent intent = new Intent(GpsRecordingService.ACTION_PAUSE_RESUME);
        sendBroadcast(intent);
    }

    private void updateUnitInfo() {
        // Показываем единицы расстояния из настроек пользователя
        if (userSettings != null) {
            String unitSymbol = userSettings.getDistanceUnit().getDisplayName(this);
            tvUnitInfo.setText(unitSymbol);
        } else {
            tvUnitInfo.setText(getString(R.string.distance_unit_km));
        }
    }
}