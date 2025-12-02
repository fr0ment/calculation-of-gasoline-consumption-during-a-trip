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

public class GpsRecordingActivity extends AppCompatActivity {

    private static final String TAG = "GpsRecordingActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    // FIX: DISPLAY_DATE_FORMAT используется для отображения, а не для парсинга DB-строк.
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    // UI элементы
    private LinearLayout layoutPreStart, layoutRecording, layoutControls;
    private EditText etTripName, etInitialFuel;
    private TextView tvDistanceValue, tvDurationValue, tvFuelRechargedValue, tvUnitInfo;
    private Button btnStart, btnPauseResume, btnRefuel, btnStop, btnCancel;
    private TextView tvCurrentStatus;

    private DatabaseHelper dbHelper;
    private Car selectedCar;
    private int carId; // Добавлено для хранения ID автомобиля

    // Состояние поездки
    private boolean isRecording = false;
    // Новое: Исходный уровень топлива, введенный пользователем перед стартом
    private double initialFuelLevel = 0.0;

    // --- Observer для LiveData ---
    private final Observer<TripRecordingData> tripUpdateObserver = tripData -> {
        if (tripData != null) {
            if (tripData.getDistance() < 0) {
                return;
            }

            // Обновление статистики
            tvDistanceValue.setText(String.format(Locale.getDefault(), "%.1f", tripData.getDistance()));
            tvDurationValue.setText(formatDuration(tripData.getDurationMs()));
            tvFuelRechargedValue.setText(String.format(Locale.getDefault(), "%.1f", tripData.getFuelRecharged()));

            // Обновление кнопки Пауза/Возобновить
            if (tripData.isPaused()) {
                btnPauseResume.setText("ВОЗОБНОВИТЬ");
                tvCurrentStatus.setText("ЗАПИСЬ ПРИОСТАНОВЛЕНА");
            } else {
                btnPauseResume.setText("ПАУЗА");
                tvCurrentStatus.setText("ЗАПИСЬ АКТИВНА");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_recording);

        // NEW: Скрытие клавиатуры по умолчанию при запуске activity
        new Handler(Looper.getMainLooper()).post(() -> {
            if (etTripName != null) hideKeyboard(etTripName);
        });

        // Получение ID автомобиля
        carId = getIntent().getIntExtra("car_id", -1);
        if (carId == -1) {
            Toast.makeText(this, "Автомобиль не выбран", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        selectedCar = dbHelper.getCar(carId);

        if (selectedCar == null) {
            Toast.makeText(this, "Автомобиль не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Инициализация репозитория (важно, что он инициализирован до подписки)
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

            // NEW: Обработка "Назад" во время записи — показ диалога отмены
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

    // NEW: Для singleTask: Обработка повторного запуска (bring to front из уведомления)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // Обновляем intent для extras (car_id)
        carId = intent.getIntExtra("car_id", carId);  // Перезагружаем car_id если изменился
        selectedCar = dbHelper.getCar(carId);
        updateUnitInfo();  // Обновляем UI (единицы)
        // Если запись активна, восстанавливаем состояние
        if (TripRecordingRepository.getInstance().isRecording()) {
            isRecording = true;
            layoutPreStart.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            layoutRecording.setVisibility(View.VISIBLE);
            layoutControls.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        // Элементы Pre-Start
        layoutPreStart = findViewById(R.id.layout_pre_start);
        etTripName = findViewById(R.id.et_trip_name);
        etInitialFuel = findViewById(R.id.et_initial_fuel);
        tvUnitInfo = findViewById(R.id.tv_unit_info);
        btnStart = findViewById(R.id.btn_start_recording);

        // Элементы Recording
        layoutRecording = findViewById(R.id.layout_recording);
        tvCurrentStatus = findViewById(R.id.tv_current_status);

        // Значения статистики (включая include: stat_distance, stat_duration, stat_fuel_recharged)
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvDurationValue = findViewById(R.id.tv_duration_value);
        tvFuelRechargedValue = findViewById(R.id.tv_fuel_recharged_value);

        // Элементы Controls
        layoutControls = findViewById(R.id.layout_controls);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnRefuel = findViewById(R.id.btn_refuel);
        btnStop = findViewById(R.id.btn_stop_recording);
        btnCancel = findViewById(R.id.btn_cancel_recording);
    }

    private void setupListeners() {
        // NEW: Для btnStart — inline скрытие клавиатуры в onClick
        btnStart.setOnClickListener(v -> {
            handleStartRecording();
            hideKeyboard(v);  // Скрываем на View-клике (btnStart)
        });
        btnPauseResume.setOnClickListener(v -> {
            handlePauseResume();
            hideKeyboard(v);  // Опционально: скрываем при клике на другие кнопки
        });
        btnRefuel.setOnClickListener(v -> {
            if (isRecording) {
                showRefuelDialog();
            }
            hideKeyboard(v);
        });
        // NEW: Listener для отмены (был пропущен)
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

    // NEW: Диалог подтверждения отмены записи
    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Отменить запись?")
                .setMessage("Запись будет остановлена без сохранения. Продолжить?")
                .setPositiveButton("ОТМЕНИТЬ", (dialog, which) -> {
                    // Остановка без сохранения
                    stopRecordingService();
                })
                .setNegativeButton("ПРОДОЛЖИТЬ", null)
                .show();
    }

    private void handleStartRecording() {
        // NEW: Валидация initialFuel
        String initialFuelStr = etInitialFuel.getText().toString().trim();
        if (initialFuelStr.isEmpty()) {
            etInitialFuel.setError("Введите начальный уровень топлива.");
            return;
        }
        try {
            initialFuelLevel = Double.parseDouble(initialFuelStr);
            if (initialFuelLevel < 0) {
                etInitialFuel.setError("Значение должно быть неотрицательным.");
                return;
            }
        } catch (NumberFormatException e) {
            etInitialFuel.setError("Некорректное числовое значение.");
            return;
        }

        // Проверка разрешения на локацию
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        // Установка initialFuel в репозиторий
        TripRecordingRepository.getInstance().setInitialFuelLevel(initialFuelLevel);

        // Запуск сервиса
        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        serviceIntent.putExtra("car_id", carId);
        startService(serviceIntent);

        // Переход к UI записи
        isRecording = true;
        layoutPreStart.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
        layoutRecording.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);

        // NEW: Скрытие клавиатуры и фокуса после начала записи (после изменения UI, с задержкой)
        new Handler(Looper.getMainLooper()).postDelayed(() -> hideKeyboard(layoutRecording), 100);  // Задержка 100ms для layout

        // NEW: Добавляем callback для "Назад" после старта записи
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCancelConfirmationDialog();
            }
        });

        Toast.makeText(this, "Запись начата", Toast.LENGTH_SHORT).show();
    }

    // NEW: Улучшенный метод для скрытия клавиатуры (принимает View для точного windowToken)
    private void hideKeyboard(View view) {
        if (view == null) return;

        // Установка режима окна для скрытия клавиатуры
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Снимаем фокус с EditText
        if (etTripName != null) etTripName.clearFocus();
        if (etInitialFuel != null) etInitialFuel.clearFocus();

        // Явно скрываем клавиатуру на конкретном View
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        // Дополнительно: Скрываем фокус с всего окна
        getWindow().getDecorView().clearFocus();
    }

    // NEW: Полная реализация диалога заправки (на основе dialog_refuel.xml)
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

        tvRefuelUnit.setText(String.format(Locale.getDefault(), "Единица измерения: %s", fuelUnit));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnAddRefuel.setOnClickListener(v -> {
            String amountStr = etRefuelAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                etRefuelAmount.setError("Введите количество топлива.");
                return;
            }
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    etRefuelAmount.setError("Количество должно быть положительным.");
                    return;
                }

                // Добавляем в репозиторий (LiveData обновит UI автоматически)
                TripRecordingRepository.getInstance().addRefuel(amount);

                Toast.makeText(this, String.format(Locale.getDefault(), "Заправлено: %.1f %s", amount, fuelUnit), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etRefuelAmount.setError("Некорректное числовое значение.");
            }
        });

        btnCancelRefuel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showSaveTripDialog() {
        if (!TripRecordingRepository.getInstance().isRecording()) {
            Toast.makeText(this, "Запись не активна. Закрытие...", Toast.LENGTH_SHORT).show();
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

        tvUnit.setText(String.format(Locale.getDefault(), "Единица измерения: %s (Остаток топлива)", fuelUnit));
        tvSummary.setText(String.format(Locale.getDefault(), "Пожалуйста, введите количество топлива, оставшееся в баке (%s). Макс. возможное: %.1f %s.", fuelUnit, maxPossibleFuel, fuelUnit));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String finalFuelStr = etFinalFuel.getText().toString().trim();
            if (finalFuelStr.isEmpty()) {
                etFinalFuel.setError("Введите остаток топлива.");
                return;
            }
            try {
                double finalFuelLevel = Double.parseDouble(finalFuelStr);
                if (finalFuelLevel < 0) {
                    etFinalFuel.setError("Значение должно быть неотрицательным.");
                    return;
                }

                // --- НОВАЯ ПРОВЕРКА: Остаток топлива не может быть больше, чем (Начальное + Заправленное) ---
                if (finalFuelLevel > maxPossibleFuel) {
                    etFinalFuel.setError(String.format(Locale.getDefault(), "Остаток не может превышать %.1f %s (Начальное + Заправленное).", maxPossibleFuel, fuelUnit));
                    return;
                }
                // ------------------------------------------------------------------------------------------

                // 1. Расчет и сохранение поездки в БД
                saveTripToDatabase(finalFuelLevel);

                // 2. Остановка сервиса (теперь с finish())
                stopRecordingService();

                dialog.dismiss();
            } catch (NumberFormatException e) {
                etFinalFuel.setError("Некорректное числовое значение.");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Сохраняет данные о поездке в локальную БД.
     */
    private void saveTripToDatabase(double finalFuelLevel) {
        TripRecordingRepository repository = TripRecordingRepository.getInstance();

        // Получение финальных данных
        double distance = repository.getCurrentDistanceKm();
        double totalFuelRecharged = repository.getTotalFuelRecharged();
        double initialFuel = repository.getInitialFuelLevel();
        String name = etTripName.getText().toString().trim();
        String startDateTime = repository.getStartDateTime();
        String endDateTime = repository.getCurrentDateTime();

        // FIX: Дебаж initialFuel (удали после теста)
        if (initialFuel <= 0) {
            Log.e(TAG, "Initial fuel not set correctly! Value: " + initialFuel);
        }

        // Расчет потраченного топлива
        // Потраченное = (Начальное + Заправленное) - Конечное
        double fuelSpent = (initialFuel + totalFuelRecharged) - finalFuelLevel;

        // Расчет расхода топлива (л/100км)
        double fuelConsumption = 0.0;
        if (distance > 0 && fuelSpent > 0) {
            fuelConsumption = (fuelSpent / distance) * 100;
        }

        // 2. Создание и сохранение объекта Trip
        // FIX: Использование new Date() вместо парсинга строки, если имя пустое
        String tripName = name.isEmpty() ? "Поездка " + DISPLAY_DATE_FORMAT.format(new Date()) : name;

        Trip newTrip = new Trip(
                carId,
                tripName,
                startDateTime,
                endDateTime,
                distance,
                fuelSpent,
                fuelConsumption
        );

        long result = dbHelper.addTrip(newTrip);

        if (result > 0) {
            Toast.makeText(this, "Поездка успешно сохранена!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Ошибка при сохранении поездки.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecordingService() {
        // FIX: Сначала reset() (очистка репозитория и сигнал в observer),
        // затем stopService, затем finish() (гарантированное закрытие activity)
        TripRecordingRepository.getInstance().reset();
        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        stopService(serviceIntent);
        isRecording = false;
        finish();  // Явное закрытие activity — теперь всегда срабатывает
    }

    // NEW: Скрытие клавиатуры при тапе вне полей (activity-wide)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard(getCurrentFocus());  // Если фокус на View
        return super.onTouchEvent(event);
    }

    // --- Жизненный цикл активности и разрешения ---

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Отписка от LiveData для предотвращения утечек памяти
        if (TripRecordingRepository.getInstance() != null) {
            TripRecordingRepository.getInstance().getTripUpdates().removeObserver(tripUpdateObserver);
        }
    }

    // --- Утилиты (без изменений) ---

    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        // Используем формат ЧЧ ч ММ мин СС сек для более точного отображения
        return String.format(Locale.getDefault(), "%02d ч %02d мин %02d сек", hours, minutes, seconds);
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

    private void handlePauseResume() {
        // Отправка broadcast для toggle в сервисе
        Intent intent = new Intent(GpsRecordingService.ACTION_PAUSE_RESUME);
        sendBroadcast(intent);
    }

    private void updateUnitInfo() {
        // Установка единиц из selectedCar
        if (selectedCar != null) {
            tvUnitInfo.setText(selectedCar.getDistanceUnit());
        }
    }
}