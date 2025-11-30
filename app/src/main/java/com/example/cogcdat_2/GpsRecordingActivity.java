package com.example.cogcdat_2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class GpsRecordingActivity extends AppCompatActivity {

    private static final String TAG = "GpsRecordingActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // UI элементы
    private LinearLayout layoutPreStart, layoutRecording, layoutControls;
    private EditText etTripName, etInitialFuel;
    private TextView tvDistanceValue, tvDurationValue, tvFuelRechargedValue, tvUnitInfo;
    private Button btnStart, btnPauseResume, btnRefuel, btnStop;
    private TextView tvStatus;

    private DatabaseHelper dbHelper;
    private Car selectedCar;

    // --- Observer для LiveData ---
    private final Observer<TripRecordingData> tripUpdateObserver = update -> {
        if (selectedCar == null || update == null) return;

        // ИСПРАВЛЕНО: Закрытие Activity, когда LiveData сбрасывается (сигнал от postReset)
        if (update.getDistance() <= 0.01 && update.getDurationMs() == 0 && !GpsRecordingService.isServiceRunning) {
            Log.d(TAG, "Trip data reset received. Service stopped. Closing Activity.");
            finish();
            return;
        }

        // Обновление UI статистики
        if (tvDistanceValue != null) {
            tvDistanceValue.setText(String.format(Locale.getDefault(), "%.2f %s", update.getDistance(), selectedCar.getDistanceUnit()));
        }

        if (tvDurationValue != null) {
            tvDurationValue.setText(formatDuration(update.getDurationMs()));
        }

        if (tvFuelRechargedValue != null) {
            tvFuelRechargedValue.setText(String.format(Locale.getDefault(), "%.1f %s", update.getFuelRecharged(), selectedCar.getFuelUnit()));
        }


        // Обновление кнопки и статуса паузы
        if (tvStatus != null) {
            if (update.isPaused()) {
                tvStatus.setText("ЗАПИСЬ ПРИОСТАНОВЛЕНА");
                if (btnPauseResume != null) btnPauseResume.setText("ПРОДОЛЖИТЬ");
            } else {
                tvStatus.setText("ЗАПИСЬ АКТИВНА");
                if (btnPauseResume != null) btnPauseResume.setText("ПАУЗА");
            }
        }
    };

    // --- Lifecycle Methods ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_gps_recording);

            dbHelper = new DatabaseHelper(this);
            int carId = getIntent().getIntExtra("CAR_ID", -1);
            selectedCar = dbHelper.getCarById(carId);

            if (selectedCar == null) {
                Toast.makeText(this, "Ошибка: Автомобиль не найден.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Главные методы
            initViews();
            setInitialState();
            checkLocationPermission();

            // ПОДПИСКА НА LIVEDATA (после инициализации View)
            TripRecordingRepository.getInstance().getTripUpdates().observe(this, tripUpdateObserver);

        } catch (Exception e) {
            Log.e(TAG, "CRASH during onCreate! Root cause:", e);
            Toast.makeText(this, "Критическая ошибка при запуске. Проверьте Logcat!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // onResume/onPause теперь не нужны, так как нет LocalBroadcastManager


    // --- Инициализация и Управление UI (С ПРОВЕРКАМИ NULL) ---

    private void initViews() {
        // Группы
        layoutPreStart = findViewById(R.id.layout_pre_start);
        layoutRecording = findViewById(R.id.layout_recording);
        layoutControls = findViewById(R.id.layout_controls);
        tvStatus = findViewById(R.id.tv_current_status);

        // Pre-Start
        etTripName = findViewById(R.id.et_trip_name);
        etInitialFuel = findViewById(R.id.et_initial_fuel);
        tvUnitInfo = findViewById(R.id.tv_unit_info);

        if (tvUnitInfo != null) {
            tvUnitInfo.setText(String.format("Единицы измерения: %s (топливо), %s (расстояние)",
                    selectedCar.getFuelUnit(), selectedCar.getDistanceUnit()));
        }

        btnStart = findViewById(R.id.btn_start_recording);

        // Recording Stats
        View statDistance = findViewById(R.id.stat_distance);
        if (statDistance != null) {
            TextView label = statDistance.findViewById(R.id.tv_stat_label);
            if (label != null) label.setText("Пробег:");
            tvDistanceValue = statDistance.findViewById(R.id.tv_stat_value);
        } else {
            Log.w(TAG, "ID stat_distance не найден в XML.");
        }

        View statDuration = findViewById(R.id.stat_duration);
        if (statDuration != null) {
            TextView label = statDuration.findViewById(R.id.tv_stat_label);
            if (label != null) label.setText("Длительность:");
            tvDurationValue = statDuration.findViewById(R.id.tv_stat_value);
        } else {
            Log.w(TAG, "ID stat_duration не найден в XML.");
        }

        View statFuelRecharged = findViewById(R.id.stat_fuel_recharged);
        if (statFuelRecharged != null) {
            TextView label = statFuelRecharged.findViewById(R.id.tv_stat_label);
            if (label != null) label.setText("Заправлено:");
            tvFuelRechargedValue = statFuelRecharged.findViewById(R.id.tv_stat_value);
        } else {
            Log.w(TAG, "ID stat_fuel_recharged не найден в XML.");
        }


        // Controls
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnRefuel = findViewById(R.id.btn_refuel);
        btnStop = findViewById(R.id.btn_stop_recording);

        // Слушатели
        if (btnStart != null) btnStart.setOnClickListener(v -> startRecordingClick());
        if (btnPauseResume != null) btnPauseResume.setOnClickListener(v -> togglePauseResume());
        if (btnRefuel != null) btnRefuel.setOnClickListener(v -> showRefuelDialog());
        if (btnStop != null) btnStop.setOnClickListener(v -> showStopDialog());
    }

    private void setInitialState() {
        if (GpsRecordingService.isServiceRunning) {
            switchUiToRecording();
        } else {
            // Безопасное обновление видимости
            if (layoutPreStart != null) layoutPreStart.setVisibility(View.VISIBLE);
            if (btnStart != null) btnStart.setVisibility(View.VISIBLE);
            if (layoutRecording != null) layoutRecording.setVisibility(View.GONE);
            if (layoutControls != null) layoutControls.setVisibility(View.GONE);
        }
    }

    private void switchUiToRecording() {
        if (layoutPreStart != null) layoutPreStart.setVisibility(View.GONE);
        if (btnStart != null) btnStart.setVisibility(View.GONE);
        if (layoutRecording != null) layoutRecording.setVisibility(View.VISIBLE);
        if (layoutControls != null) layoutControls.setVisibility(View.VISIBLE);
    }

    // --- Обработчики Кнопок ---

    private void startRecordingClick() {
        String name = etTripName.getText().toString().trim();
        String initialFuelStr = etInitialFuel.getText().toString().trim();

        if (name.isEmpty()) {
            etTripName.setError("Обязательное поле!");
            return;
        }
        if (initialFuelStr.isEmpty()) {
            etInitialFuel.setError("Введите количество топлива!");
            return;
        }

        try {
            double initialFuel = Double.parseDouble(initialFuelStr);

            if (!checkLocationPermission()) {
                requestLocationPermission();
                return;
            }

            Intent serviceIntent = new Intent(this, GpsRecordingService.class);
            serviceIntent.putExtra("CAR_ID", selectedCar.getId());
            serviceIntent.putExtra("TRIP_NAME", name);
            serviceIntent.putExtra("INITIAL_FUEL", initialFuel);

            ContextCompat.startForegroundService(this, serviceIntent);

            switchUiToRecording();

        } catch (NumberFormatException e) {
            etInitialFuel.setError("Некорректное число.");
        }
    }

    private void togglePauseResume() {
        Intent intent = new Intent(GpsRecordingService.ACTION_PAUSE_RESUME);
        // Используем стандартный Broadcast, так как сервис настроен на него
        sendBroadcast(intent);
    }

    private void showRefuelDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_refuel, null);
        EditText etRefuelAmount = dialogView.findViewById(R.id.et_refuel_amount);
        TextView tvRefuelUnit = dialogView.findViewById(R.id.tv_refuel_unit);
        tvRefuelUnit.setText(selectedCar.getFuelUnit());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Заправка")
                .setMessage("Введите количество заправленного топлива:")
                .setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String amountStr = etRefuelAmount.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Введите количество.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount > 0) {
                            Intent intent = new Intent(GpsRecordingService.ACTION_REFUEL);
                            intent.putExtra("AMOUNT", amount);
                            // Используем стандартный Broadcast, так как сервис настроен на него
                            sendBroadcast(intent);
                            Toast.makeText(this, String.format("Добавлено %.1f %s", amount, selectedCar.getFuelUnit()), Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Некорректное число.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showStopDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_refuel, null);
        EditText etFinalFuel = dialogView.findViewById(R.id.et_refuel_amount);
        TextView tvFinalFuelUnit = dialogView.findViewById(R.id.tv_refuel_unit);
        tvFinalFuelUnit.setText(selectedCar.getFuelUnit());

        TextView titleView = dialogView.findViewById(R.id.tv_refuel_title);
        if(titleView != null) titleView.setText("Окончание поездки");


        new MaterialAlertDialogBuilder(this)
                .setTitle("Остаток топлива")
                .setMessage("Введите количество топлива, оставшегося в баке:")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String amountStr = etFinalFuel.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Введите оставшееся топливо!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double finalFuel = Double.parseDouble(amountStr);

                        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
                        serviceIntent.putExtra("FINAL_FUEL", finalFuel);
                        // Отправляем команду остановки с данными через startForegroundService.
                        // Сервис сохранит данные и вызовет postReset() для закрытия Activity.
                        ContextCompat.startForegroundService(this, serviceIntent);

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Некорректное число.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // --- Утилиты (без изменений) ---

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = (seconds % 3600) / 60;
        long hours = seconds / 3600;
        return String.format(Locale.getDefault(), "%02d ч %02d мин", hours, minutes);
    }

    // --- Управление Разрешениями (без изменений) ---

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
                Toast.makeText(this, "Разрешение получено. Нажмите 'Начать'.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Разрешение на GPS требуется для записи поездки.", Toast.LENGTH_LONG).show();
            }
        }
    }
}