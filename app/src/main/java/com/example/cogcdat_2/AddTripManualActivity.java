package com.example.cogcdat_2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.cogcdat_2.api.models.LoginRequest;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTripManualActivity extends BaseActivity {

    private DatabaseHelper dbHelper;
    private String carId;
    private String currentUserId;
    private UserSettings userSettings;

    // Поля ввода
    private EditText etTripName;
    private EditText etDistance;
    private EditText etFuelSpent;
    private Button btnSelectStartDate;
    private Button btnSelectStartTime;
    private Button btnSelectEndDate;
    private Button btnSelectEndTime;
    private Button btnSaveTrip;
    private Button btnBack;

    // Календари для хранения выбранной даты и времени
    private Calendar startDateTime = Calendar.getInstance();
    private Calendar endDateTime = Calendar.getInstance();

    // Формат даты/времени для БД
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    // Формат для отображения в кнопках
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip_manual);

        dbHelper = new DatabaseHelper(this);
        carId = getIntent().getStringExtra("car_id");

        if (carId == null || carId.isEmpty()) {
            Toast.makeText(this, R.string.error_car_not_selected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Получаем ID текущего пользователя из SharedPreferences
        currentUserId = getSharedPreferences("user", MODE_PRIVATE).getString("user_id", null);

        // Загружаем настройки пользователя
        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
        } else {
            // Если нет user_id, создаем настройки по умолчанию
            userSettings = new UserSettings();
        }

        // Инициализация полей
        etTripName = findViewById(R.id.et_trip_name);
        etDistance = findViewById(R.id.et_distance);
        etFuelSpent = findViewById(R.id.et_fuel_spent);
        btnSelectStartDate = findViewById(R.id.btn_select_start_date);
        btnSelectStartTime = findViewById(R.id.btn_select_start_time);
        btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        btnSelectEndTime = findViewById(R.id.btn_select_end_time);
        btnSaveTrip = findViewById(R.id.btn_save_trip);
        btnBack = findViewById(R.id.btnBack);

        // Устанавливаем подсказку для поля расстояния с учетом единиц измерения
        updateDistanceHint();

        // Установка текущей даты/времени по умолчанию
        updateDateTimeButtons();

        // Установка слушателей
        btnSelectStartDate.setOnClickListener(v -> showDatePicker(startDateTime, true));
        btnSelectStartTime.setOnClickListener(v -> showTimePicker(startDateTime, true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(endDateTime, false));
        btnSelectEndTime.setOnClickListener(v -> showTimePicker(endDateTime, false));

        btnSaveTrip.setOnClickListener(v -> saveTrip());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Обновляет подсказку в поле расстояния в зависимости от единиц измерения
     */
    private void updateDistanceHint() {
        String unitHint;
        if (userSettings != null && userSettings.getDistanceUnit() == DistanceUnit.MI) {
            unitHint = getString(R.string.distance_hint_miles);
        } else {
            unitHint = getString(R.string.distance_hint_km);
        }
        etDistance.setHint(unitHint);
    }

    // --- ВЫБОР ДАТЫ/ВРЕМЕНИ ---

    private void showDatePicker(Calendar calendar, boolean isStart) {
        long selection = calendar.getTimeInMillis();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText(isStart ? getString(R.string.start_date) : getString(R.string.end_date))
                .setSelection(selection)
                .build();

        datePicker.addOnPositiveButtonClickListener(selectionMillis -> {
            calendar.setTimeInMillis(selectionMillis);
            // Приводим к началу дня
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            updateDateTimeButtons();

            // Проверка логики начала/окончания
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, getString(R.string.error_end_before_start_date), Toast.LENGTH_SHORT).show();
                endDateTime.setTime(startDateTime.getTime());
                updateDateTimeButtons();
            }
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker(Calendar calendar, boolean isStart) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText(isStart ? getString(R.string.start_time) : getString(R.string.end_time))
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());

            updateDateTimeButtons();

            // Проверка конца ≥ начала
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, getString(R.string.error_end_before_start_time), Toast.LENGTH_SHORT).show();
                endDateTime.setTime(startDateTime.getTime());
                updateDateTimeButtons();
            }
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void updateDateTimeButtons() {
        btnSelectStartDate.setText(DISPLAY_DATE_FORMAT.format(startDateTime.getTime()));
        btnSelectStartTime.setText(DISPLAY_TIME_FORMAT.format(startDateTime.getTime()));
        btnSelectEndDate.setText(DISPLAY_DATE_FORMAT.format(endDateTime.getTime()));
        btnSelectEndTime.setText(DISPLAY_TIME_FORMAT.format(endDateTime.getTime()));
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private double parseDoubleWithComma(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        // Заменяем запятую на точку для корректного парсинга
        String normalized = input.trim().replace(',', '.');
        return Double.parseDouble(normalized);
    }

    // --- СОХРАНЕНИЕ И ВАЛИДАЦИЯ ---

    private void saveTrip() {
        // 1. Считывание и валидация обязательного поля (Название)
        String name = etTripName.getText().toString().trim();
        if (name.isEmpty()) {
            etTripName.setError(getString(R.string.error_trip_name_required));
            Toast.makeText(this, R.string.error_please_enter_trip_name, Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Считывание и валидация числовых полей
        double userDistance = 0.0;
        double fuelSpent = 0.0;

        // Валидация обязательного поля (Расстояние)
        String distanceStr = etDistance.getText().toString().trim();
        if (distanceStr.isEmpty()) {
            etDistance.setError(getString(R.string.error_distance_required));
            Toast.makeText(this, getString(R.string.error_please_enter_distance), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            userDistance = parseDoubleWithComma(distanceStr);
        } catch (NumberFormatException e) {
            etDistance.setError(getString(R.string.error_invalid_distance));
            return;
        }

        // Валидация обязательного поля (Топливо)
        String fuelSpentStr = etFuelSpent.getText().toString().trim();
        if (fuelSpentStr.isEmpty()) {
            etFuelSpent.setError(getString(R.string.error_fuel_required));
            Toast.makeText(this, getString(R.string.error_please_enter_fuel), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fuelSpent = parseDoubleWithComma(fuelSpentStr);
        } catch (NumberFormatException e) {
            etFuelSpent.setError(getString(R.string.error_invalid_fuel));
            return;
        }

        // Дополнительная валидация данных
        if (userDistance <= 0) {
            etDistance.setError(getString(R.string.error_distance_must_be_positive));
            return;
        }
        if (fuelSpent <= 0) {
            etFuelSpent.setError(getString(R.string.error_fuel_must_be_positive));
            return;
        }
        if (endDateTime.before(startDateTime)) {
            Toast.makeText(this, getString(R.string.error_end_before_start), Toast.LENGTH_LONG).show();
            return;
        }

        // Проверка минимальной длительности поездки (не менее 1 минуты)
        long startMillis = startDateTime.getTimeInMillis();
        long endMillis = endDateTime.getTimeInMillis();
        long durationMillis = endMillis - startMillis;
        long oneMinuteMillis = 60 * 1000;
        if (durationMillis < oneMinuteMillis) {
            Toast.makeText(this, getString(R.string.error_min_duration), Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Создание объекта поездки
        Trip newTrip = new Trip();
        newTrip.setCarId(carId);
        newTrip.setName(name);

        // Форматирование дат
        String startDateTimeStr = DB_DATE_FORMAT.format(startDateTime.getTime());
        String endDateTimeStr = DB_DATE_FORMAT.format(endDateTime.getTime());
        newTrip.setStartDateTime(startDateTimeStr);
        newTrip.setEndDateTime(endDateTimeStr);

        // Устанавливаем расстояние с конвертацией из единиц пользователя в километры
        DistanceUnit inputUnit = userSettings != null ? userSettings.getDistanceUnit() : DistanceUnit.KM;
        newTrip.setDistanceFromUserInput(userDistance, inputUnit);

        newTrip.setFuelSpent(fuelSpent);

        // 4. Сохранение в БД
        String result = dbHelper.addTrip(newTrip);

        if (result != null) {
            Toast.makeText(this, R.string.success_trip_added, Toast.LENGTH_SHORT).show();
            // Уведомляем TripsFragment об успешном добавлении
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.error_adding_trip, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем настройки при возврате на экран
        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
            updateDistanceHint();
        }
    }
}