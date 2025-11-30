package com.example.cogcdat_2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.app.AlertDialog;
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
    private Button btnStart, btnPauseResume, btnRefuel, btnStop;
    private TextView tvCurrentStatus; // ИСПРАВЛЕНО: Было tvStatus

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
            // Специальный сигнал для закрытия
            if (tripData.getDistance() < 0) {
                // Если мы здесь, значит, сервис был остановлен и репозиторий сброшен
                finish();
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

        // Подписка на LiveData
        TripRecordingRepository.getInstance().getTripUpdates().observe(this, tripUpdateObserver);
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
        tvCurrentStatus = findViewById(R.id.tv_current_status); // ИСПРАВЛЕНО

        // Значения статистики (включая include: stat_distance, stat_duration, stat_fuel_recharged)
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvDurationValue = findViewById(R.id.tv_duration_value);
        tvFuelRechargedValue = findViewById(R.id.tv_fuel_recharged_value);


        // Элементы Controls
        layoutControls = findViewById(R.id.layout_controls);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnRefuel = findViewById(R.id.btn_refuel);
        btnStop = findViewById(R.id.btn_stop_recording);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> handleStartRecording());
        btnPauseResume.setOnClickListener(v -> handlePauseResume());
        btnRefuel.setOnClickListener(v -> {
            if (isRecording) { // Проверяем, что запись активна перед показом диалога
                showRefuelDialog(); // НОВЫЙ СЛУШАТЕЛЬ
            } else {
                Toast.makeText(this, "Сначала начните запись поездки.", Toast.LENGTH_SHORT).show();
            }
        });
        btnStop.setOnClickListener(v -> {
            if (isRecording) {
                handleStopRecording();
            } else {
                Toast.makeText(this, "Запись не активна.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUnitInfo() {
        String info = String.format(Locale.getDefault(), "Единицы измерения: %s (топливо), %s (расстояние)",
                selectedCar.getFuelUnit(), selectedCar.getDistanceUnit());
        tvUnitInfo.setText(info);
    }

    private void handleStartRecording() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        // 1. Проверка и сохранение начального уровня топлива
        String fuelInput = etInitialFuel.getText().toString().trim();
        if (fuelInput.isEmpty()) {
            etInitialFuel.setError("Введите начальный уровень топлива.");
            return;
        }
        try {
            initialFuelLevel = Double.parseDouble(fuelInput);
            if (initialFuelLevel < 0) {
                etInitialFuel.setError("Значение должно быть неотрицательным.");
                return;
            }
        } catch (NumberFormatException e) {
            etInitialFuel.setError("Некорректное числовое значение.");
            return;
        }

        // 2. Скрытие Pre-Start и отображение Recording/Controls
        layoutPreStart.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
        layoutRecording.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);

        // ПЕРЕДАЧА НАЧАЛЬНОГО УРОВНЯ ТОПЛИВА В РЕПОЗИТОРИЙ (ВРЕМЕННОЕ РЕШЕНИЕ)
        // Важно: нужно делать это до запуска сервиса, чтобы сервис при старте мог использовать startRecording()
        TripRecordingRepository.getInstance().setInitialFuelLevel(initialFuelLevel);

        // 3. Запуск сервиса
        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        serviceIntent.putExtra("car_id", carId);
        serviceIntent.putExtra("trip_name", etTripName.getText().toString().trim());

        ContextCompat.startForegroundService(this, serviceIntent);
        isRecording = true;
    }

    private void handlePauseResume() {
        if (!isRecording) return; // Проверка, чтобы не отправлять, если запись не идет
        Intent intent = new Intent(GpsRecordingService.ACTION_PAUSE_RESUME);
        sendBroadcast(intent);
    }

    private void handleStopRecording() {
        // Показываем диалог для ввода конечного уровня топлива
        showFinalFuelDialog();
    }

    /**
     * Показывает диалог для добавления заправленного топлива.
     */
    private void showRefuelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        // Используем предоставленный XML-файл для диалога
        View dialogView = inflater.inflate(R.layout.dialog_refuel, null);
        builder.setView(dialogView);

        // Инициализация полей ввода и кнопок
        EditText etRefuelAmount = dialogView.findViewById(R.id.et_refuel_amount);
        TextView tvRefuelUnit = dialogView.findViewById(R.id.tv_refuel_unit);

        // Установка единицы измерения
        tvRefuelUnit.setText(selectedCar.getFuelUnit());

        // Кнопки из запроса
        Button btnAdd = dialogView.findViewById(R.id.btn_add_refuel);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_refuel);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnAdd.setOnClickListener(v -> {
            String amountStr = etRefuelAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                etRefuelAmount.setError("Введите количество.");
                return;
            }
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    etRefuelAmount.setError("Количество должно быть положительным.");
                    return;
                }

                // 1. Отправка команды в репозиторий/сервис для добавления топлива
                TripRecordingRepository.getInstance().addRefuel(amount);
                Toast.makeText(this, String.format(Locale.getDefault(), "Добавлено %.1f %s", amount, selectedCar.getFuelUnit()), Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            } catch (NumberFormatException e) {
                etRefuelAmount.setError("Некорректное числовое значение.");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }


    /**
     * Показывает диалог для ввода остатка топлива перед сохранением.
     */
    private void showFinalFuelDialog() {
        TripRecordingRepository repository = TripRecordingRepository.getInstance();
        double initialFuel = repository.getInitialFuelLevel();
        double totalFuelRecharged = repository.getTotalFuelRecharged();
        double maxPossibleFuel = initialFuel + totalFuelRecharged;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_trip, null);
        builder.setView(dialogView);

        EditText etFinalFuel = dialogView.findViewById(R.id.et_final_fuel);
        TextView tvFinalFuelUnit = dialogView.findViewById(R.id.tv_final_fuel_unit);
        TextView tvSummary = dialogView.findViewById(R.id.tv_final_fuel_summary);
        Button btnSave = dialogView.findViewById(R.id.btn_save_trip);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_final_fuel);

        // Обновление текста с единицами измерения
        String fuelUnit = selectedCar.getFuelUnit();
        tvFinalFuelUnit.setText(String.format(Locale.getDefault(), "Единица измерения: %s (Остаток топлива)", fuelUnit));
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

                // 2. Остановка сервиса
                stopRecordingService();

                dialog.dismiss();
                // Закрытие активности происходит через Observer после reset() в stopRecordingService,
                // чтобы гарантировать, что сервис остановлен и Activity получило финальное обновление.
                // finish(); // Убрано, т.к. происходит через Observer
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
        isRecording = false;
        Intent serviceIntent = new Intent(this, GpsRecordingService.class);
        // Сброс состояния репозитория должен быть вызван до stopService,
        // чтобы Activity успело получить сигнал-закрытие (-1.0)
        TripRecordingRepository.getInstance().reset();
        stopService(serviceIntent);
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
}